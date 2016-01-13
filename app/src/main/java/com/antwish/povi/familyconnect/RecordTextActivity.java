package com.antwish.povi.familyconnect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.antwish.povi.server.Child;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecordTextActivity extends AppCompatActivity {
    private static final String TAG = "RecordTextActivity";
    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";
    private Toolbar mToolbar;
    private Tracker mTracker;
    private String mFileName;
    private TabLayout tabs;
    private List<Child> mChild;
    private Calendar calendar = Calendar.getInstance();
    private  SimpleDateFormat timeFormat = new SimpleDateFormat("m:ss", Locale.US);
    private EditText mText;
    private Button mDraftButton;
    private Button mSaveButton;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        mTracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        mTracker.setScreenName("Record text comment screen");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Set layout
        setContentView(R.layout.activity_record_text);

        context = this;

        // Get bundle
        final Beat beat = getIntent().getParcelableExtra("beat");

        // Set up controls
        mText = (EditText) findViewById(R.id.text);
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
                bundle.putInt("content_group", 1);
                bundle.putInt("resource_id",beat.resourceId);
                bundle.putInt("tip_id",beat.tipId);
                bundle.putString("comment_text", mText.getText().toString());
                Intent saveContentActivity = new Intent(context, AddBeatActivity.class);
                saveContentActivity.putExtras(bundle);
                startActivity(saveContentActivity);
                finish();
            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String currentToken = sharedPref.getString(POVI_TOKEN, "");
        final String currentUser = sharedPref.getString(POVI_USERID, "");
        tabs = (TabLayout) findViewById(R.id.tabs);

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
}



