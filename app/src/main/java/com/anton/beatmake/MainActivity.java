package com.anton.beatmake;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.view.ViewGroup.LayoutParams;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anton.beatmake.fileexplorer.FileExplorer;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_LOSS;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
import static android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
import static android.media.AudioManager.STREAM_MUSIC;

public class MainActivity extends AppCompatActivity  {
    private static final String SAMPLE_FOLDER = "sample_sounds";
    private static final int SAMPLE_REQUEST = 1;
    private static final int EXPLORER_REQUEST = 2;
    private static final String TAG = "BeatMake";
    private static final int TOTAL_STEPS = 16;
    private static final int TOTAL_CHANNELS = 4;
    private float[] volumes = new float[] {0.4f, 0.4f, 0.4f, 0.4f};
    private TextView volume0Text, volume1Text, volume2Text, volume3Text;
    private int tempo = 120;
    private String title = "untitled";
    private TextView tempoText;
    private boolean isRunning;
    private int mainLayoutWidth, mainLayoutHeight;
    private LinearLayout mainLayout, channelsLayout;
    private LinearLayout boardLayouts[] = new LinearLayout[TOTAL_CHANNELS];
    private AppCompatButton channels[] = new AppCompatButton[TOTAL_CHANNELS];
    private ToggleButton sequencerButtons[][] = new ToggleButton[TOTAL_CHANNELS][TOTAL_STEPS];
    private float[][] sequencer = new float[TOTAL_CHANNELS][TOTAL_STEPS];

    private SeekBar.OnSeekBarChangeListener tempoListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            switch (seekBar.getId()) {
                case R.id.bpm_seekbar:
                    tempo = (progress + 1) * 10 + (tempo % 10);
                    PdBase.sendFloat("tempo", tempo);
                    if (tempoText != null) {
                        tempoText.setText(" ");
                        tempoText.append(String.valueOf(tempo));
                    }
                    break;

                case R.id.ch0_seekbar:
                    volumes[0] =(float) progress / 10;
                    PdBase.sendFloat("amp0", volumes[0]);
                    if (volume0Text != null) {
                        volume0Text.setText(" ");
                        volume0Text.append(String.valueOf(volumes[0]));
                    }
                    break;

                case R.id.ch1_seekbar:
                    volumes[1] =(float) progress / 10;
                    PdBase.sendFloat("amp1", volumes[1]);
                    if (volume1Text != null) {
                        volume1Text.setText(" ");
                        volume1Text.append(String.valueOf(volumes[1]));
                    }
                    break;

                case R.id.ch2_seekbar:
                    volumes[2] =(float) progress / 10;
                    PdBase.sendFloat("amp2", volumes[2]);
                    if (volume2Text != null) {
                        volume2Text.setText(" ");
                        volume2Text.append(String.valueOf(volumes[2]));
                    }
                    break;

                case R.id.ch3_seekbar:
                    volumes[3] =(float) progress / 10;
                    PdBase.sendFloat("amp3", volumes[3]);
                    if (volume3Text != null) {
                        volume3Text.setText(" ");
                        volume3Text.append(String.valueOf(volumes[3]));
                    }
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private AudioManager audioManager;
    private final AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (pdService == null) {
                return;
            }
            switch (focusChange) {
                case AUDIOFOCUS_GAIN:
                    if (!pdService.isRunning()) {
                        startAudio();
                    }
                    break;
                case AUDIOFOCUS_LOSS_TRANSIENT:
                case AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (pdService.isRunning()) {
                        stopAudio();
                    }
                    break;
                case AUDIOFOCUS_LOSS:
                    if (pdService.isRunning()) {
                        stopAudio();
                    }
                    break;
                default:
                    break;
            }

        }
    };

    private PdService pdService = null;
    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder)service).getService();
            try {

                initPD();
                loadPDPatch();
                setSequencerParam();
                //loadSamples();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // this method will never be called
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences score = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor score_inc = score.edit();
        int counter = score.getInt("counter",0);
        if(counter==0)
        {
            //Put your function to copy files here
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    long time = System.currentTimeMillis();
                    copyAssetFolder(getAssets(), SAMPLE_FOLDER,
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/beatmake");
                    time = System.currentTimeMillis() - time;
                    Log.i(TAG, "Time is " + time);
                }
            });
            thread.start();
            score_inc.putInt("counter", ++counter);
            score_inc.commit();
        }

        // Use the whole device screen.
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prepareBoard();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        clearAnimation();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        clearAnimation();
        cleanup();
        super.onDestroy();
    }

    private void initPD() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        pdService.initAudio(sampleRate, 0, 2, 10.0f);
        startAudio();
    }

    private void loadPDPatch() throws IOException {
        File dir = getFilesDir();
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.pd), dir, true);
        File pdPatch = new File(dir, "readsf.pd");
        PdBase.openPatch(pdPatch.getAbsolutePath());
        PdUiDispatcher dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
        ChannelListener channelListener = new ChannelListener(this, sequencerButtons);
        dispatcher.addListener("ch0", channelListener);
        dispatcher.addListener("ch1", channelListener);
        dispatcher.addListener("ch2", channelListener);
        dispatcher.addListener("ch3", channelListener);
    }

   private static boolean copyAssetFolder(AssetManager assetManager,
                                          String fromAssetPath, String toPath) {
       try {
           String[] files = assetManager.list(fromAssetPath);
           File dir = new File(toPath);
           if (!dir.exists())
               dir.mkdirs();
           boolean res = true;
           for (String file : files)
               if (file.contains("."))
                   res &= copyAsset(assetManager,
                           fromAssetPath + "/" + file,
                           toPath + "/" + file);
               else
                   res &= copyAssetFolder(assetManager,
                           fromAssetPath + "/" + file,
                           toPath + "/" + file);
           return res;
       } catch (Exception e) {
           e.printStackTrace();
           return false;
       }
   }

    private static boolean copyAsset(AssetManager assetManager,
                                     String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private void setSequencerParam () {
        for (int channelPos = 0; channelPos < TOTAL_CHANNELS; channelPos++) {
            PdBase.writeArray("ch" + channelPos, 0, sequencer[channelPos], 0, 16);
        }
        PdBase.sendFloat("tempo", tempo);
        PdBase.sendFloat("amp0", volumes[0]);
        PdBase.sendFloat("amp1", volumes[1]);
        PdBase.sendFloat("amp2", volumes[2]);
        PdBase.sendFloat("amp3", volumes[3]);
    }

    private void startAudio() {
        String name = getResources().getString(R.string.app_name);
        pdService.startAudio(new Intent(this, MainActivity.class), R.drawable.icon, name, "Return to " + name + ".");
    }

    private boolean requestAudioFocus() {
        int result = audioManager.requestAudioFocus(audioFocusListener, STREAM_MUSIC,
                AUDIOFOCUS_GAIN);
        return result == AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        audioManager.abandonAudioFocus(audioFocusListener);
    }

    private void stopAudio() {
        pdService.stopAudio();
    }

    private void cleanup() {
        if (isRunning) {
            PdBase.sendBang("onoff");
            abandonAudioFocus();
        }
        stopAudio();
        PdBase.release();
        try {
            unbindService(pdConnection);
        } catch (IllegalArgumentException e) {
            // already unbound
            pdService = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toggle_sequencer:
                if (isRunning) {
                    stopPlayback();
                    item.setIcon(android.R.drawable.ic_media_play);
                } else {
                    startPlayback();
                    item.setIcon(android.R.drawable.ic_media_pause);
                }
                break;
            case R.id.preferences:
                MaterialDialog dialog = new MaterialDialog.Builder(this)
                        .customView(R.layout.dialog, false)
                        .backgroundColorRes(R.color.darkgray)
                        .dismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                //configButton.setSelected(false);
                            }
                        })
                        .show();

                View dialogView = dialog.getCustomView();
                assert dialogView != null;

                tempoText = (TextView) dialogView.findViewById(R.id.bpm_label);
                tempoText.setText(" ");
                tempoText.append(String.valueOf(tempo));

                volume0Text = (TextView) dialogView.findViewById(R.id.ch0_label);
                volume0Text.setText(" ");
                volume0Text.append(String.valueOf(volumes[0]));

                volume1Text = (TextView) dialogView.findViewById(R.id.ch1_label);
                volume1Text.setText(" ");
                volume1Text.append(String.valueOf(volumes[1]));

                volume2Text = (TextView) dialogView.findViewById(R.id.ch2_label);
                volume2Text.setText(" ");
                volume2Text.append(String.valueOf(volumes[2]));

                volume3Text = (TextView) dialogView.findViewById(R.id.ch3_label);
                volume3Text.setText(" ");
                volume3Text.append(String.valueOf(volumes[3]));


                AppCompatSeekBar tempoSeek = (AppCompatSeekBar)dialogView.findViewById(R.id.bpm_seekbar);
                tempoSeek.setProgress((tempo / 10) - 1);
                tempoSeek.setOnSeekBarChangeListener(tempoListener);

                AppCompatSeekBar ch0Seek = (AppCompatSeekBar)dialogView.findViewById(R.id.ch0_seekbar);
                ch0Seek.setProgress((int)(volumes[0] * 10));
                ch0Seek.setOnSeekBarChangeListener(tempoListener);

                AppCompatSeekBar ch1Seek = (AppCompatSeekBar)dialogView.findViewById(R.id.ch1_seekbar);
                ch1Seek.setProgress((int)(volumes[1] * 10));
                ch1Seek.setOnSeekBarChangeListener(tempoListener);

                AppCompatSeekBar ch2Seek = (AppCompatSeekBar)dialogView.findViewById(R.id.ch2_seekbar);
                ch2Seek.setProgress((int)(volumes[2] * 10));
                ch2Seek.setOnSeekBarChangeListener(tempoListener);

                AppCompatSeekBar ch3Seek = (AppCompatSeekBar)dialogView.findViewById(R.id.ch3_seekbar);
                ch3Seek.setProgress((int)(volumes[3] * 10));
                ch3Seek.setOnSeekBarChangeListener(tempoListener);
                break;
            case R.id.reset:
                clearSequencer();
                break;
            case R.id.save:
                Sample sample = new Sample(title, sequencer, tempo, volumes);
                Intent intent = new Intent(this, SampleListActivity.class);
                intent.putExtra(SampleListActivity.SAMPLE_EXTRA, sample);
                startActivityForResult(intent, SAMPLE_REQUEST);
        }
        return false;
    }

    private void startPlayback() {
        if (requestAudioFocus()) {
            PdBase.sendBang("onoff");
            isRunning = true;
        }
    }

    private void stopPlayback() {
        PdBase.sendBang("onoff");
        abandonAudioFocus();
        isRunning = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

            switch (requestCode){
                case EXPLORER_REQUEST:
                    if (resultCode == RESULT_OK) {
                        int buttonId = data.getIntExtra(FileExplorer.ID_EXTRA, -1);
                        if (buttonId == -1) {
                            Log.i(TAG, "Error buttonId");
                        } else {
                            String path = data.getStringExtra(FileExplorer.PATH_EXTRA);
                            PdBase.sendSymbol("sample" + buttonId, path);
                        }
                    }
                    break;
                case SAMPLE_REQUEST:
                    if(resultCode == SampleListActivity.RESULT_LOADED) {
                        Sample sample = (Sample) data.getExtras().getSerializable(SampleListActivity.SAMPLE_EXTRA);
                        title = sample.getTitle();
                        sequencer = sample.getSequence();
                        volumes = sample.getChannelVolumes();
                        tempo = sample.getTempo();
                        setSequencerParam();
                        updateUI();
                    } else if(resultCode == SampleListActivity.RESULT_SAVED){
                        title = data.getStringExtra(SampleListActivity.TITLE_EXTRA);
                    }
                    break;
            }
    }

    private void prepareBoard() {
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.HORIZONTAL);

        channelsLayout = new LinearLayout(this);
        channelsLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams samplesLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        samplesLayoutParams.weight = (float) 0.9375;
        channelsLayout.setLayoutParams(samplesLayoutParams);
        channelsLayout.setBackgroundColor(getResources().getColor(R.color.material_dark));
        rootLayout.addView(channelsLayout);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mainLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mainLayoutParams.weight = (float) 0.0625;
        mainLayout.setLayoutParams(mainLayoutParams);
        rootLayout.addView(mainLayout);

        setContentView(rootLayout);

        mainLayout.post(new Runnable() {
            @Override
            public void run() {
           mainLayoutHeight = mainLayout.getHeight();
                mainLayoutWidth = mainLayout.getWidth();
                createLayouts();
                createBoardButtons();
            }
        });
    }

    private void createLayouts() {

        for (int channelPos = 0; channelPos < TOTAL_CHANNELS; channelPos++) {

            boardLayouts[channelPos] = new LinearLayout(this);
            boardLayouts[channelPos].setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    mainLayoutHeight / TOTAL_CHANNELS));
            boardLayouts[channelPos].setBackgroundColor(getResources().getColor(R.color.gray));
            boardLayouts[channelPos].setGravity(Gravity.CENTER);
            mainLayout.addView(boardLayouts[channelPos]);
        }

    }

    private void createBoardButtons() {
        String buttonLabel;
        int buttonWidth = mainLayoutWidth / TOTAL_STEPS;
        int buttonHeight = mainLayoutWidth / TOTAL_STEPS;
        final int[] buttonsWithDarkGrayColor = new int[] {4,5,6,7,12,13,14,15};

        for (int channelPos = 0; channelPos < TOTAL_CHANNELS; channelPos++) {
            channels[channelPos] = new AppCompatButton(this);
            channels[channelPos].setId(channelPos);
            buttonLabel = getResources().getString(R.string.channel) + Integer.toString(channelPos);
            channels[channelPos].setText(buttonLabel);
            channels[channelPos].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(MainActivity.this, FileExplorer.class);
                    i.putExtra(FileExplorer.ID_EXTRA, v.getId());
                    startActivityForResult(i, EXPLORER_REQUEST);
                }
            });
            channels[channelPos].setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, mainLayoutHeight / TOTAL_CHANNELS));
            channels[channelPos].setBackgroundColor(getResources().getColor(R.color.material_dark));
            channelsLayout.addView(channels[channelPos]);

            for (int stepPos = 0; stepPos < TOTAL_STEPS; stepPos++) {
                sequencerButtons[channelPos][stepPos] = new ToggleButton(this);
                sequencerButtons[channelPos][stepPos].setTextOff("");
                sequencerButtons[channelPos][stepPos].setTextOn("");
                sequencerButtons[channelPos][stepPos].setText("");
                sequencerButtons[channelPos][stepPos].setWidth(buttonWidth);
                sequencerButtons[channelPos][stepPos].setHeight(buttonHeight);
                sequencerButtons[channelPos][stepPos].setId(TOTAL_STEPS * channelPos + stepPos);
                sequencerButtons[channelPos][stepPos].setOnClickListener(new SequencerButtonClickListener());

                    if (Arrays.binarySearch(buttonsWithDarkGrayColor, stepPos) < 0) {
                        boardLayouts[channelPos].addView(sequencerButtons[channelPos][stepPos]);

                    } else {
                        LinearLayout buttonLayout = new LinearLayout(this);
                        buttonLayout.setLayoutParams(new LayoutParams(buttonWidth, mainLayoutHeight / TOTAL_CHANNELS));
                        buttonLayout.setGravity(Gravity.CENTER);
                        buttonLayout.setBackgroundColor(getResources().getColor(R.color.darkgray));
                        buttonLayout.addView(sequencerButtons[channelPos][stepPos]);
                        boardLayouts[channelPos].addView(buttonLayout);
                    }
            }
        }
    }

    private void writeArray(View view, int channel, int step) {
        ToggleButton currentButton = (ToggleButton) view;
        if (currentButton.isChecked()) {
            sequencer[channel][step] = 1;
        }
        else {
            sequencer[channel][step] = 0;
        }
        PdBase.writeArray("ch"+channel, step, sequencer[channel], step, 1);
    }

    private void clearSequencer() {
        for (int channelPos = 0; channelPos < TOTAL_CHANNELS; channelPos++) {
            for (int stepPos = 0; stepPos < TOTAL_STEPS; stepPos++) {
                sequencer[channelPos][stepPos] = 0;
                PdBase.writeArray("ch" + channelPos, stepPos, sequencer[channelPos], stepPos, 1);
                sequencerButtons[channelPos][stepPos].setChecked(false);
            }
        }
    }

    private void clearAnimation() {
        for (int channelPos = 0; channelPos < TOTAL_CHANNELS; channelPos++) {
            for (int stepPos = 0; stepPos < TOTAL_STEPS; stepPos++) {
                sequencerButtons[channelPos][stepPos].clearAnimation();
            }
        }
    }

    private void updateUI() {
        for (int channelPos = 0; channelPos < TOTAL_CHANNELS; channelPos++) {
            for (int stepPos = 0; stepPos < TOTAL_STEPS; stepPos++) {
                if (sequencer[channelPos][stepPos] == 1) {
                    sequencerButtons[channelPos][stepPos].setChecked(true);
                } else {
                    sequencerButtons[channelPos][stepPos].setChecked(false);
                }
            }
        }
    }
/*
    private void readAsset() {
        AssetManager am = getAssets();
        try {
            InputStream is = am.open(SAMPLE_FOLDER + "/claps/Dance.wav");
            OutputStream os = openFileOutput("Clap.wav", MODE_PRIVATE);
            copyFile(is, os);
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    private class SequencerButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            int buttonId = view.getId();
            int channel = buttonId / TOTAL_STEPS;
            int step = buttonId % TOTAL_STEPS;
            writeArray(view, channel, step);
        }
    }
}