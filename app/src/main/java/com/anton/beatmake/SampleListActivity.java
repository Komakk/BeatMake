package com.anton.beatmake;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.anton.beatmake.database.SampleCursorWrapper;
import com.anton.beatmake.database.SampleDbHelper;
import com.anton.beatmake.database.SampleDbSchema.SampleTable;

import java.util.ArrayList;
import java.util.List;

public class SampleListActivity extends Activity implements View.OnClickListener,
AdapterView.OnItemLongClickListener {
    static final String SAMPLE_EXTRA = "sample";
    static final int RESULT_SAVED = 2;
    static final int RESULT_LOADED = 3;
    static final String TITLE_EXTRA = "title";

    private SampleListAdapter adapter;
    private ListView listView;
    private SQLiteDatabase database;
    private Sample sample;
    private List<Sample> samples;
    private EditText editText;
    private String title;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sample = (Sample) getIntent().getExtras().getSerializable(SAMPLE_EXTRA);
        setContentView(R.layout.samplelistactivity_layout);

        editText = (EditText) findViewById(R.id.edittext);
        editText.setText(sample.getTitle());
        editText.setSelection(editText.getText().length());

        Button saveButton = (Button) findViewById(R.id.savebutton);
        saveButton.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.listview);

        database = new SampleDbHelper(this).getWritableDatabase();
        samples = new ArrayList<>();
        getSamples();
        adapter = new SampleListAdapter(this, android.R.layout.simple_list_item_1, samples);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Sample sample = (Sample) parent.getItemAtPosition(position);
                Intent intent = new Intent();
                intent.putExtra(SAMPLE_EXTRA, sample);
                setResult(RESULT_LOADED, intent);
                finish();
            }
        });
    }

    private ContentValues getContentValues(Sample sample) {
        ContentValues values = new ContentValues();
        values.put(SampleTable.Cols.TITLE, sample.getTitle());
        values.put(SampleTable.Cols.SEQUENCE, sample.getJsonSequence());
        values.put(SampleTable.Cols.TEMPO, sample.getTempo());
        values.put(SampleTable.Cols.CHANNEL_VOLUMES, sample.getJsonChannelVolumes());

        return values;
    }

    private void addSample(final Sample sample) {
        ContentValues values = getContentValues(sample);

        //database.insert(SampleTable.NAME, null, values);
        try {
            database.insertWithOnConflict(SampleTable.NAME, null, values, SQLiteDatabase.CONFLICT_FAIL);
            returnResult();
        } catch (SQLiteConstraintException e) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.overwrite_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.overwrite, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateSample(sample);
                            returnResult();
                        }
                    })
                    .create()
                    .show();
        }
    }

    private void updateSample(Sample sample) {
        String sampleTitle = sample.getTitle();
        ContentValues values = getContentValues(sample);

        database.update(SampleTable.NAME, values, SampleTable.Cols.TITLE + " = ?",
                new String[] { sampleTitle });
    }

    private void deleteSample(Sample sample) {
        String sampleTitle = sample.getTitle();
        database.delete(SampleTable.NAME, SampleTable.Cols.TITLE + " = ?",
                new String[] { sampleTitle });
    }

    private SampleCursorWrapper querySamples(String whereClause, String[] whereArgs) {
        Cursor cursor = database.query(
                SampleTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new SampleCursorWrapper(cursor);
    }

    private void getSamples() {
        if (samples.size() > 0)
            samples.clear();

        SampleCursorWrapper cursor = querySamples(null, null);

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                samples.add(cursor.getSample());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }

    }

    @Override
    public void onClick(View v) {
        title = editText.getText().toString();
        sample.setTitle(title);
        addSample(sample);
        //updateUI();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Sample sample = (Sample) parent.getItemAtPosition(position);

        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_sample)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSample(sample);
                        updateUI();
                    }
                })
                .create()
                .show();
        return true;
    }


    private void returnResult() {
        Intent intent = new Intent();
        intent.putExtra(TITLE_EXTRA, title);
        setResult(RESULT_SAVED, intent);
        finish();
    }

    private void updateUI() {
        getSamples();

        if (adapter == null) {
            adapter = new SampleListAdapter(this, android.R.layout.simple_list_item_1, samples);
            listView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }


    private class SampleListAdapter extends ArrayAdapter<Sample> {
        private Context c;
        private int id;
        private List<Sample> mSamples;

        private SampleListAdapter(Context context, int textViewResourceId,
                                 List<Sample> objects) {
            super(context, textViewResourceId, objects);
            c = context;
            id = textViewResourceId;
            mSamples = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = vi.inflate(id, null);
            }

            if (mSamples.size() == 0)
                return rowView;

            Sample sample = mSamples.get(position);
            if (sample != null) {
                TextView textView = (TextView) rowView;
                textView.setText(sample.getTitle());
            }
            return rowView;
        }
    }
}
