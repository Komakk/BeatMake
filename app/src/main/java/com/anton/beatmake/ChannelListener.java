package com.anton.beatmake;


import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ToggleButton;

import org.puredata.core.PdListener;

class ChannelListener implements PdListener {
    private static final String TAG = "BeatMake";

    private Context context;
    private ToggleButton[][] sequencerButtons;
    private int channel, step;

    ChannelListener(Context context, ToggleButton[][] sequencer) {
        sequencerButtons = sequencer;
        this.context = context;
    }

    @Override
    public void receiveBang(String source) {

    }

    @Override
    public void receiveFloat(String source, float x) {
        Log.i(TAG, "receive float= " + x);
        if(x >= 16) {
            return;
        }
        channel = Character.getNumericValue(source.charAt(2));
        step = (int) x;
        startAnimation(sequencerButtons[channel][step]);
    }

    @Override
    public void receiveSymbol(String source, String symbol) {

    }

    @Override
    public void receiveList(String source, Object... args) {

    }

    @Override
    public void receiveMessage(String source, String symbol, Object... args) {

    }

    private void startAnimation(View v) {
        Log.i("BeatMake", "Start anim");
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.scaleandcoloranim);
        v.startAnimation(anim);
    }
}
