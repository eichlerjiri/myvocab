package eichlerjiri.myvocab;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Random;

import eichlerjiri.myvocab.data.VocabItem;
import eichlerjiri.myvocab.data.VocabLoader;

import static android.view.ViewGroup.LayoutParams.*;
import static eichlerjiri.myvocab.utils.Common.*;
import static java.lang.Math.*;

public class LessonActivity extends Activity {

    public String filename;
    public ArrayList<VocabItem> vocabs;
    public String audioFile;
    public ArrayList<Integer> seekIndex;

    public int totalCount;
    public int currentIndex;
    public boolean confirmed;
    public boolean playAudio;
    public ArrayList<Integer> removeLog = new ArrayList<>();

    public Random random;
    public RelativeLayout l;
    public TextView progress;
    public TextView original;
    public TextView translation;
    public Button replay;

    public Bundle savedState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String title = null;

        Bundle b = getIntent().getExtras();
        if (b != null) {
            title = b.getString("title");
            filename = b.getString("filename");
        }

        if (title == null || filename == null) {
            Toast.makeText(this, "Lesson unavailable", Toast.LENGTH_LONG).show();
            return;
        }

        setTitle(title);

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            throw new Error("Action bar not available");
        }

        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        savedState = savedInstanceState;
        new VocabLoader(this, filename, false, false).load();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("reload").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new VocabLoader(LessonActivity.this, filename, true, false).load();
                return true;
            }
        });
        menu.add("reload audio").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new VocabLoader(LessonActivity.this, filename, false, true).load();
                return true;
            }
        });
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (vocabs != null) {
            outState.putInt("totalCount", totalCount);
            outState.putInt("currentIndex", currentIndex);
            outState.putBoolean("confirmed", confirmed);
            outState.putBoolean("playAudio", playAudio);
            outState.putIntegerArrayList("removeLog", removeLog);
        }
    }

    public void vocabsLoaded(ArrayList<VocabItem> vocabsN, String audioFileN, ArrayList<Integer> seekIndexN) {
        vocabs = vocabsN;
        audioFile = audioFileN;
        seekIndex = seekIndexN;
        totalCount = vocabs.size();

        random = new Random();
        float sp = getResources().getDisplayMetrics().scaledDensity;

        progress = new TextView(this);
        progress.setPadding(0, 0, 0, round(10 * sp));

        original = new TextView(this);
        original.setTextSize(original.getTextSize() * 1.2f);

        translation = new TextView(this);
        translation.setTextSize(translation.getTextSize() * 1.2f);
        translation.setTypeface(translation.getTypeface(), Typeface.BOLD);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        int padding = round(5 * sp);
        ll.setPadding(padding, padding, padding, padding);
        ll.addView(progress);
        ll.addView(original);
        ll.addView(translation);

        Button noButton = new Button(this);
        noButton.setBackgroundColor(Color.RED);
        noButton.setTextSize(noButton.getTextSize() * 1.5f);
        RelativeLayout.LayoutParams paramsNoButton = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        paramsNoButton.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        paramsNoButton.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        noButton.setLayoutParams(paramsNoButton);

        Button goodButton = new Button(this);
        goodButton.setBackgroundColor(Color.GREEN);
        goodButton.setTextSize(goodButton.getTextSize() * 1.5f);
        RelativeLayout.LayoutParams paramsGoodButton = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        paramsGoodButton.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        paramsGoodButton.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        goodButton.setLayoutParams(paramsGoodButton);

        replay = new Button(this);
        replay.setText("replay");
        RelativeLayout.LayoutParams paramsReplayButton = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        paramsReplayButton.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        paramsReplayButton.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        replay.setLayoutParams(paramsReplayButton);

        l = new RelativeLayout(this);
        l.addView(ll);
        l.addView(noButton);
        l.addView(goodButton);

        goodButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultButtonClicked(true);
            }
        });
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultButtonClicked(false);
            }
        });
        replay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    playAudio();
                } catch (InterruptedIOException e) {
                    // end
                }
            }
        });

        nextVocab();
        setContentView(l);
    }

    public void resultButtonClicked(boolean good) {
        try {
            if (confirmed) {
                if (good) {
                    vocabs.remove(currentIndex);
                    removeLog.add(Integer.valueOf(currentIndex));
                }

                translation.setText("");
                confirmed = false;
                playAudio = false;
                l.removeView(replay);

                nextVocab();
            } else {
                VocabItem item = vocabs.get(currentIndex);

                translation.setText(item.translation);
                confirmed = true;
                if (playAudio()) {
                    playAudio = true;
                    l.addView(replay);
                }
            }
        } catch (InterruptedIOException e) {
            // end
        }
    }

    public boolean playAudio() throws InterruptedIOException {
        VocabItem item = vocabs.get(currentIndex);
        if (item.audioFrom != -1 && item.audioTo != -1) {
            if (seekIndex.size() <= item.audioFrom || seekIndex.size() <= item.audioTo) {
                Toast.makeText(this, "Error playing audio", Toast.LENGTH_LONG).show();
                return false;
            }

            int from = seekIndex.get(item.audioFrom).intValue();
            int to = seekIndex.get(item.audioTo).intValue();

            byte[] data = readFilePartial(new File(getCacheDir(), audioFile), from, to - from);
            if (data == null) {
                Toast.makeText(this, "Error playing audio", Toast.LENGTH_LONG).show();
                return false;
            }

            File tmp = createTempFile("myvocab", ".mp3");
            boolean ret = doPlayAudio(tmp, data);
            unlinkFile(tmp);
            return ret;
        } else {
            return false;
        }
    }

    public boolean doPlayAudio(File tmp, byte[] data) throws InterruptedIOException {
        if (!writeFile(tmp, data)) {
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_LONG).show();
            return false;
        }

        MediaPlayer mediaPlayer = MediaPlayer.create(this, Uri.fromFile(tmp));
        if (mediaPlayer == null) {
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_LONG).show();
            return false;
        }
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
        mediaPlayer.start();
        return true;
    }

    public void nextVocab() {
        if (vocabs.isEmpty()) {
            finish();
        } else {
            if (savedState != null && savedState.containsKey("totalCount")
                    && savedState.getInt("totalCount") == totalCount) {
                currentIndex = savedState.getInt("currentIndex");
                confirmed = savedState.getBoolean("confirmed");
                playAudio = savedState.getBoolean("playAudio");
                removeLog = savedState.getIntegerArrayList("removeLog");

                for (int i = 0; i < removeLog.size(); i++) {
                    vocabs.remove(removeLog.get(i).intValue());
                }

                if (confirmed) {
                    translation.setText(vocabs.get(currentIndex).translation);
                }
                if (playAudio) {
                    l.addView(replay);
                }

                savedState = null;
            } else {
                currentIndex = random.nextInt(vocabs.size());
            }

            progress.setText((totalCount - vocabs.size() + 1) + " / " + totalCount);
            original.setText(vocabs.get(currentIndex).original);
        }
    }
}
