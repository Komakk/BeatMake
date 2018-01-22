package com.anton.beatmake.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.anton.beatmake.Sample;

import static com.anton.beatmake.database.SampleDbSchema.*;

public class SampleCursorWrapper extends CursorWrapper {
    /**
     * Creates a cursor wrapper.
     *
     * @param cursor The underlying cursor to wrap.
     */
    public SampleCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Sample getSample() {
        String sampleTitle = getString(getColumnIndex(SampleTable.Cols.TITLE));
        float[][] sequence = Sample.jsonToSequence(getString(getColumnIndex(SampleTable.Cols.SEQUENCE)));
        int tempo = getInt(getColumnIndex(SampleTable.Cols.TEMPO));
        float[] channelVolumes = Sample.jsonToChannelVolumes(getString(getColumnIndex(SampleTable.Cols.CHANNEL_VOLUMES)));

        Sample sample = new Sample(sampleTitle, sequence, tempo, channelVolumes);

        return sample;
    }
}
