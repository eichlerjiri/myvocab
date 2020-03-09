package eichlerjiri.myvocab.data;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;

import eichlerjiri.myvocab.LessonActivity;
import eichlerjiri.myvocab.R;

import static eichlerjiri.myvocab.utils.Common.*;

public class VocabLoader implements Runnable {

    public final LessonActivity c;
    public final String filename;
    public final File cacheDir;
    public final String myvocabUrl;
    public final boolean forceReload;
    public final boolean forceReloadAudio;

    public AlertDialog alertDialog;
    public final ArrayList<VocabItem> vocabs = new ArrayList<>();
    public String audio;
    public final ArrayList<Integer> seekIndex = new ArrayList<>();
    public final ArrayList<String> errors = new ArrayList<>();

    public VocabLoader(LessonActivity c, String filename, boolean forceReload, boolean forceReloadAudio) {
        this.c = c;
        this.filename = filename;
        cacheDir = c.getCacheDir();
        myvocabUrl = c.getString(R.string.myvocab_url);
        this.forceReload = forceReload;
        this.forceReloadAudio = forceReloadAudio;
    }

    public void load() {
        alertDialog = new AlertDialog.Builder(c)
                .setMessage("Please wait")
                .setTitle("Loading lesson")
                .setCancelable(false)
                .show();

        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            byte[] data = cachedDownload(myvocabUrl, cacheDir, filename, forceReload, errors);
            if (errors.isEmpty()) {
                BufferedReader r = readByLine(data);
                while (true) {
                    String line = readLine(r);
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    ArrayList<String> parts = splitLine(line, '=');
                    if (parts.size() == 1) {
                        ArrayList<String> audioParts = splitLine(parts.get(0), ':');
                        if (audioParts.size() != 2 || !"audio".equals(audioParts.get(0)) || audio != null) {
                            errors.add("Invalid file format");
                            break;
                        }
                        audio = audioParts.get(1);
                    } else if (parts.size() == 2 || parts.size() == 3) {
                        ArrayList<String> translationParts = splitLine(parts.get(1), ':');
                        if (translationParts.size() > 2) {
                            errors.add("Invalid file format");
                            break;
                        }

                        String translation = translationParts.get(0);
                        String translationWritten = null;
                        if (translationParts.size() == 2) {
                            translationWritten = translationParts.get(1);
                        }

                        int audioFrom = -1;
                        int audioTo = -1;
                        if (parts.size() == 3) {
                            String audioInfo = parts.get(2);
                            int idx = audioInfo.indexOf('-');
                            if (idx == -1) {
                                errors.add("Invalid file format: " + parts.get(0));
                                break;
                            }
                            audioFrom = parseInt(audioInfo.substring(0, idx).trim(), -1);
                            audioTo = parseInt(audioInfo.substring(idx + 1).trim(), -1);
                            if (audioFrom < 0 || audioTo < 0 || audioFrom >= audioTo) {
                                errors.add("Invalid file format");
                                break;
                            }
                        }
                        vocabs.add(new VocabItem(parts.get(0), translation, translationWritten, audioFrom, audioTo));
                    } else {
                        errors.add("Invalid file format");
                        break;
                    }
                }
                closeStream(r);
            }

            if (errors.isEmpty() && audio != null) {
                byte[] audioFile = cachedDownload(myvocabUrl, cacheDir, audio, forceReloadAudio, errors);
                if (errors.isEmpty()) {
                    indexMP3File(audioFile);
                }
            }

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    alertDialog.dismiss();

                    if (errors.isEmpty()) {
                        c.vocabsLoaded(vocabs, audio, seekIndex);
                    } else {
                        Toast.makeText(c, "Error loading lesson: " + errors.get(0), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (InterruptedIOException e) {
            // end
        }
    }

    public void indexMP3File(byte[] audioFile) {
        int[][][] bitrateTable = new int[][][]{
                new int[][]{
                        null,
                        new int[]{0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0},
                        new int[]{0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0},
                        new int[]{0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, 0}},
                new int[][]{
                        null,
                        new int[]{0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0},
                        new int[]{0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 0},
                        new int[]{0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, 0}}
        };

        int[][] samplingTable = new int[][]{
                new int[]{22050, 24000, 16000, 0},
                new int[]{44100, 48000, 32000, 0}
        };

        int[] samplesTable = new int[]{72, 144};

        int state = 0;
        int decodedSamples = 0;

        int version = 0;
        int layer = 0;

        for (int i = 0; i < audioFile.length; i++) {
            int b = audioFile[i] & 0xFF;

            if (state == 0) {
                if (b == 0xFF) {
                    state = 1;
                }
            } else if (state == 1) {
                state = 0;

                if (b >= 0xF0) {
                    version = (b >> 3) & 0x1;
                    layer = (b >> 1) & 0x3;

                    if (layer != 0) {
                        state = 2;
                    }
                }
            } else if (state == 2) {
                int bitrate = (b >> 4) & 0xF;
                int samplingRate = (b >> 2) & 0x3;
                int padding = (b >> 1) & 0x1;

                int bitrateKbps = bitrateTable[version][layer][bitrate];
                int samplingRateHz = samplingTable[version][samplingRate];
                int samples = samplesTable[version];

                if (bitrateKbps != 0 && samplingRateHz != 0) {
                    i -= 3;

                    int frameLen = ((samples * bitrateKbps * 1000 / samplingRateHz) + padding);
                    decodedSamples += samples * 8;

                    if (seekIndex.isEmpty()) {
                        seekIndex.add(Integer.valueOf(i));
                    } else if (decodedSamples > samplingRateHz) {
                        decodedSamples -= samplingRateHz;
                        seekIndex.add(Integer.valueOf(i));
                    }

                    // skip bytes
                    i += frameLen;
                }

                state = 0;
            }
        }

        if (seekIndex.isEmpty()) {
            errors.add("Invalid audio file");
        }
    }
}
