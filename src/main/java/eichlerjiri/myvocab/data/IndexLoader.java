package eichlerjiri.myvocab.data;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import eichlerjiri.myvocab.MyVocab;
import eichlerjiri.myvocab.R;
import static eichlerjiri.myvocab.utils.Common.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;

public class IndexLoader implements Runnable {

    public final MyVocab c;
    public final File cacheDir;
    public final String myvocabUrl;
    public final boolean forceReload;

    public AlertDialog alertDialog;
    public final ArrayList<IndexItem> items = new ArrayList<>();
    public final ArrayList<String> errors = new ArrayList<>();

    public IndexLoader(MyVocab c, boolean forceReload) {
        this.c = c;
        cacheDir = c.getCacheDir();
        myvocabUrl = c.getString(R.string.myvocab_url);
        this.forceReload = forceReload;
    }

    public void load() {
        alertDialog = new AlertDialog.Builder(c)
                .setMessage("Please wait")
                .setTitle("Loading list of lessons")
                .setCancelable(false)
                .show();

        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            byte[] data = cachedDownload(myvocabUrl, cacheDir, "index.txt", forceReload, errors);
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

                    ArrayList<String> parts = splitLine(line, ':');
                    if (parts.size() != 2) {
                        errors.add("Invalid file format");
                        break;
                    }

                    items.add(new IndexItem(parts.get(0), parts.get(1)));
                }
                closeStream(r);
            }

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    alertDialog.dismiss();

                    if (errors.isEmpty()) {
                        c.indexLoaded(items);
                    } else {
                        Toast.makeText(c, "Error loading list of lessons: " + errors.get(0), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (InterruptedIOException e) {
            // end
        }
    }
}
