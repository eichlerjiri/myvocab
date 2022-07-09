package eichlerjiri.myvocab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import eichlerjiri.myvocab.data.IndexLoader;
import eichlerjiri.myvocab.data.IndexLoader.IndexItem;
import static eichlerjiri.myvocab.utils.Common.*;
import java.util.ArrayList;

public class MyVocab extends Activity {

    public ListView indexListView;
    public ArrayList<IndexItem> items;

    public IndexItem selectedItem;
    public Bundle savedState;

    public MyVocab() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Log.e("MyVocab", exceptionToString(e), e);

                Intent intent = new Intent(MyVocab.this, ErrorActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("msg", exceptionToString(e));
                startActivity(intent);

                System.exit(1);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Lessons");

        savedState = savedInstanceState;
        new IndexLoader(this, false).load();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("reload").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new IndexLoader(MyVocab.this, true).load();
                return true;
            }
        });
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (indexListView != null) {
            outState.putParcelable("indexList", indexListView.onSaveInstanceState());
        }
    }

    public void indexLoaded(ArrayList<IndexItem> itemsN) {
        items = itemsN;
        String[] viewItems = new String[items.size()];
        for (int i = 0; i < viewItems.length; i++) {
            viewItems[i] = items.get(i).title;
        }

        indexListView = new ListView(this);

        indexListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItem = items.get(position);
                variantDialog();
            }
        });
        indexListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, viewItems));

        if (savedState != null && savedState.containsKey("indexList")) {
            indexListView.onRestoreInstanceState(savedState.getParcelable("indexList"));
            savedState = null;
        }

        setContentView(indexListView);
    }

    public void variantDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage("Evaluation mode")
                .setNegativeButton("Speaking only", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        startLesson(false);
                    }
                })
                .setPositiveButton("Writing", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        startLesson(true);
                    }
                });
        builder.create().show();
    }

    public void startLesson(boolean writingMode) {
        Intent intent = new Intent(this, LessonActivity.class);
        intent.putExtra("title", selectedItem.title);
        intent.putExtra("filename", selectedItem.filename);
        intent.putExtra("writingMode", writingMode);
        startActivity(intent);
    }
}
