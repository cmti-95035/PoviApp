package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.CamcorderProfile;
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
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
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

public class RecordVideoActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "RecordVideoActivity";
    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";
    private Toolbar mToolbar;
    private Tracker mTracker;
    private FloatingActionButton mAction;

    private MediaPlayer mPlayer;
    private MediaRecorder mRecorder;
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
    private SurfaceView mPreview;
    private Camera mCamera;
    private FrameLayout fr;
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
        mTracker.setScreenName("Record video comment screen");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Set layout
        setContentView(R.layout.activity_record_video);

        context = this;

        fr=(FrameLayout)findViewById(R.id.frame);
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
                bundle.putInt("content_group", 4);
                bundle.putInt("resource_id",beat.resourceId);
                bundle.putInt("tip_id",beat.tipId);
                bundle.putString("media_file", mFileName);
                Intent saveContentActivity = new Intent(context, AddBeatActivity.class);
                saveContentActivity.putExtras(bundle);
                startActivity(saveContentActivity);
                finish();
            }
        });
        mPreview = (SurfaceView) findViewById(R.id.preview);
        mPreview.setKeepScreenOn(true);
        mPreview.getHolder().addCallback(this);

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
                mFileName = currentUser + "_" + mChild.get(tab.getPosition()).getName() + "_" + Integer.toString(beat.tipId) + "_" + dateStr + ".mp4";
                File file = new File(poviPath + mFileName);
                if (file.exists()){
                    mAction.setImageResource(R.drawable.ic_play_arrow_white_36dp);
                    recordingState = 2; // recorded
                    //mPlayer = new MediaPlayer();
                    try {
                        mPlayer.setDataSource(poviPath + mFileName);
                       // mPlayer.setDisplay(mPreview.getHolder());
                        mPlayer.prepare();
                        cal.setTimeInMillis(mPlayer.getDuration());
                        mTimerView.setText("0:00 / " + timeFormat.format(cal.getTime()));
                        //mPlayer.reset();
                        //mPlayer = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                else{
                    mAction.setImageResource(R.drawable.ic_videocam_white_36dp);
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

        mPlayer = new MediaPlayer();

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
        // Initialize first back camera
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setRecordingHint(true);
        //parameters.setPreviewSize(1280, 720);

        int height= fr.getWidth();
        int width=fr.getHeight();
        Camera.Size bestSize = parameters.getPreviewSize();
        //Camera.Size bestSize = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), width, height);
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

        bestSize.height = 10;
        bestSize.width = 10;
        if (sizes != null) {
            for (Camera.Size size : sizes)
                if (size.height <= height && size.width <= width && size.height >= bestSize.height && size.width >= bestSize.width)
                    bestSize = size;
            parameters.setPreviewSize(bestSize.width, bestSize.height);

        }

        mCamera.setParameters(parameters);
        //setCameraDisplayOrientation(this, 0, mCamera);
        mCamera.setDisplayOrientation(90);

        //mCamera.startPreview();
        mCamera.unlock();
        mRecorder = new MediaRecorder();
        mRecorder.setCamera(mCamera);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
        mRecorder.setOrientationHint(90);

        mRecorder.setOutputFile(poviPath + mFileName);
        mRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        // Limit the recording to 1 minute for the moment
        mRecorder.setMaxDuration(1000 * 60);
        mRecorder.setMaxFileSize(10240000);
        mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Snackbar.make(mAction, "Recording error!", Snackbar.LENGTH_LONG).show();
            }
        });
        try {
            mRecorder.prepare();
            ViewGroup.LayoutParams lp = mPreview.getLayoutParams();
            lp.width = bestSize.height;
            lp.height = bestSize.width;
            mPreview.setLayoutParams(lp);
            mRecorder.start();
           // mCamera.lock();
        } catch (IOException e) {
            mRecorder.release();
            Snackbar.make(mAction, "Unable to start recording!", Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    private void stopRecording(){
        mRecorder.stop();
        mCamera.stopPreview();
        mRecorder.release();
        mRecorder = null;
       // mCamera.lock();
        mCamera.release();
    }

    private void startPlaying() {
        //mPlayer = new MediaPlayer();
        try {
            mPlayer.reset();
            mPlayer.setDataSource(poviPath + mFileName);
            //mPlayer.setDisplay(mPreview.getHolder());
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
            Snackbar.make(mAction, "Unable to play the recorded comment!", Snackbar.LENGTH_LONG).show();
        }
    }

    private void stopPlaying() {
        //mPlayer.release();
        //mPlayer = null;
        mPlayer.stop();
    }

    private void updateTimer(){
        calendar.setTimeInMillis(System.currentTimeMillis() - startTime);
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

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}



