package com.antwish.povi.familyconnect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.antwish.povi.server.Child;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecordAudioActivity extends AppCompatActivity {
    private static final String TAG = "RecordAudioActivity";
    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";
    private Toolbar mToolbar;
    private Tracker mTracker;
    private FloatingActionButton mAction;
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private String mFileName;
    private TabLayout tabs;
    private List<Child> mChild;
    private int recordingState = 0;
    private long startTime = 0;
    private Calendar calendar = Calendar.getInstance();
    private  SimpleDateFormat timeFormat = new SimpleDateFormat("m:ss", Locale.US);
    private TextView mTimerView;
    private final Handler handler = new Handler();
    private Runnable update_runnable = new Runnable() {
        public void run() {
            updateTimer();
        }
    };
    private Button mDraftButton;
    private Button mSaveButton;
    private Context context;
    private String poviPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        poviPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "POVI/";

        // Get tracker and signals the user entered the registration screen
        mTracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        mTracker.setScreenName("Record audio comment screen");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Set layout
        setContentView(R.layout.activity_record_audio);

        context = this;

        // Get bundle
        final Beat beat = getIntent().getParcelableExtra("beat");

        // Set up controls
        mDraftButton = (Button) findViewById(R.id.draftButton);
        mSaveButton = (Button) findViewById(R.id.saveButton);
        mDraftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open save content intent
                Bundle bundle = new Bundle();
                bundle.putString("child_name", mChild.get(tabs.getSelectedTabPosition()).getName());
                bundle.putInt("content_group", 2);
                bundle.putInt("resource_id",beat.resourceId);
                bundle.putInt("tip_id",beat.tipId);
                //if (pictureFileName != null)
                  //  bundle.putString("image_file", pictureFileName);
                bundle.putString("media_file", mFileName);
                Intent saveContentActivity = new Intent(context, AddBeatActivity.class);
                saveContentActivity.putExtras(bundle);
                startActivity(saveContentActivity);
                finish();
            }
        });
        mTimerView = (TextView) findViewById(R.id.timerText);
        mAction = (FloatingActionButton) findViewById(R.id.recordButton);
        mAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (recordingState){
                    case 0: // Never recorded
                        mAction.setImageResource(R.drawable.ic_stop_white_36dp);
                        startRecording();
                        mTimerView.setText("0:00 / 1:00");
                        startTime = System.currentTimeMillis();
                        recordingState = 1; // recording
                        handler.post(update_runnable);
                        break;
                    case 1: // Recording
                        mAction.setImageResource(R.drawable.ic_play_arrow_white_36dp);
                        recordingState = 2; // recorded
                        stopRecording();
                        break;
                    case 2: // Recorded
                        mAction.setImageResource(R.drawable.ic_stop_white_36dp);
                        startPlaying();
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(mPlayer.getDuration());
                        mTimerView.setText("0:00 / " + timeFormat.format(cal.getTime()));
                        startTime = System.currentTimeMillis();
                        recordingState = 3; // playing
                        handler.post(update_runnable);
                        break;
                    case 3: // playing
                        mAction.setImageResource(R.drawable.ic_play_arrow_white_36dp);
                        recordingState = 2; // recorded
                        stopPlaying();
                        break;
                }

            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String currentToken = sharedPref.getString(POVI_TOKEN, "");
        final String currentUser = sharedPref.getString(POVI_USERID, "");
        tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Stop recording or playing
                if (mRecorder != null){
                    stopRecording();

                }
                else
                if (mPlayer != null){
                    stopPlaying();

                }

                // Set state according to the existence of a previous record
                // Get current date
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMd", Locale.US);
                String dateStr = dayFormat.format(cal.getTime());
                // Change child name
                mFileName = currentUser + "_" + mChild.get(tab.getPosition()).getName() + "_" + Integer.toString(beat.tipId) + "_" + dateStr + ".aac";
                File file = new File(poviPath + mFileName);
                if (file.exists()){
                    mAction.setImageResource(R.drawable.ic_play_arrow_white_36dp);
                    recordingState = 2; // recorded
                    mPlayer = new MediaPlayer();
                    try {
                        mPlayer.setDataSource(poviPath + mFileName);
                        mPlayer.prepare();
                        cal.setTimeInMillis(mPlayer.getDuration());
                        mTimerView.setText("0:00 / " + timeFormat.format(cal.getTime()));
                        mPlayer.release();
                        mPlayer = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                else{
                    mAction.setImageResource(R.drawable.ic_mic_white_36dp);
                    recordingState = 0; // recorded
                    mTimerView.setText("0:00 / 1:00");
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // Setting up Toolbar
        setUpToolbar();

        // Populate tabs
        mChild = RestServer.getChildren(currentToken);
        if (mChild != null)
            for (Child child:mChild)
                tabs.addTab(tabs.newTab().setText(child.getName()));
    }

    /**
     * Setting up toolbar
     */
    private void setUpToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        //CardView cardToolbar = (CardView) findViewById(R.id.cardToolbar);
        if (mToolbar != null) {
            if (mToolbar != null) {
                // Add top padding if required
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    final float scale = getResources().getDisplayMetrics().density;
                    int top = (int) (30 * scale + 0.5f);
                    mToolbar.setPadding(0,top,0,0);
                }
                setSupportActionBar(mToolbar);
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            setSupportActionBar(mToolbar);
        }
    }

    private void startRecording(){
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setOutputFile(poviPath + mFileName);
        // Limit the recording to 1 minute for the moment
        mRecorder.setMaxDuration(1000 * 60);
        mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Snackbar.make(mAction, "Recording error!", Snackbar.LENGTH_LONG).show();
            }
        });
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Snackbar.make(mAction, "Unable to start recording!", Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
        mRecorder.start();
    }

    private void stopRecording(){
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(poviPath + mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
            Snackbar.make(mAction, "Unable to play the recorded comment!", Snackbar.LENGTH_LONG).show();
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void updateTimer(){
        calendar.setTimeInMillis(System.currentTimeMillis()-startTime);
        if (recordingState == 1)
            mTimerView.setText(timeFormat.format(calendar.getTime()) + " / 1:00");
        if (recordingState == 3) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(mPlayer.getDuration());
            mTimerView.setText(timeFormat.format(calendar.getTime()) + " / " + timeFormat.format(cal.getTime()));
        }
        // Stop recording if time expired
        if (recordingState == 1 && calendar.getTimeInMillis() >= 60 * 1000) {
            recordingState = 2;
            mAction.setImageResource(R.drawable.ic_play_arrow_white_36dp);
        }
        if (recordingState == 3 && calendar.getTimeInMillis() >= mPlayer.getDuration()) {
            recordingState = 2;
            mAction.setImageResource(R.drawable.ic_play_arrow_white_36dp);
        }
        if (recordingState == 1 || recordingState == 3)
            handler.postDelayed(update_runnable, 1000); // handler re-executes the Runnable
        // every second; which calls

    }

}



