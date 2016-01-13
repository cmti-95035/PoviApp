package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.antwish.povi.server.Child;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static java.io.File.separator;

public class RecordPictureActivity extends AppCompatActivity {
    private static final String TAG = "RecordPictureActivity";
    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";
    private Toolbar mToolbar;
    private Tracker mTracker;
    private String mFileName;
    private TabLayout tabs;
    private List<Child> mChild;
    private Button mDraftButton;
    private Button mSaveButton;
    private ImageView mPicture;
    private Context context;
    private Uri picUri;
    private static int REQUEST_TAKE_PHOTO = 0;
    private static int REQUEST_LOAD_PHOTO = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        mTracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        mTracker.setScreenName("Record picture comment screen");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Set layout
        setContentView(R.layout.activity_record_picture);

        context = this;

        // Get bundle
        final Beat beat = getIntent().getParcelableExtra("beat");

        // Set up controls
        mPicture = (ImageView) findViewById(R.id.picture);
        mPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final String currentUser = sharedPref.getString(POVI_USERID, "");
                mFileName = currentUser + "_" + mChild.get(tabs.getSelectedTabPosition()).getName() + "_" + Integer.toString(beat.tipId) + Long.toString(System.currentTimeMillis()) + ".jpg";
                // Show take / load picture
                final CharSequence[] options = {"Take picture", "Load picture"};
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Picture action");
                // Discriminate the fact the user owns the content
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0){
                            // Take picture
                            Intent takeIntent = PoviUtils.dispatchTakePictureIntent(mFileName, context);
                            if (takeIntent != null) {
                                picUri = (Uri) takeIntent.getExtras().get(MediaStore.EXTRA_OUTPUT);
                                startActivityForResult(takeIntent, REQUEST_TAKE_PHOTO);
                            }
                            else
                                Snackbar.make(mPicture, "Error taking picture!", Snackbar.LENGTH_LONG).show();
                        }
                        else{
                            // Load picture
                            Intent loadIntent = PoviUtils.dispatchLoadPictureIntent();
                            if (loadIntent != null) {
                                startActivityForResult(loadIntent,REQUEST_LOAD_PHOTO);
                            }
                            else
                                Snackbar.make(mPicture, "Error loading picture!", Snackbar.LENGTH_LONG).show();

                        }
                    }
                });
                builder.create().show();
            }
        });
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
                bundle.putInt("content_group", 3);
                bundle.putInt("resource_id",beat.resourceId);
                bundle.putInt("tip_id",beat.tipId);
                bundle.putString("media_file", mFileName);
                Intent saveContentActivity = new Intent(context, AddBeatActivity.class);
                saveContentActivity.putExtras(bundle);
                startActivity(saveContentActivity);
                finish();
            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String currentToken = sharedPref.getString(POVI_TOKEN, "");
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String poviPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + separator + "POVI"+ separator;
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                File picFile = new File(picUri.getPath());
                boolean res = picFile.renameTo(new File(poviPath + mFileName));
                if (res){
                    Bitmap bmp = PoviUtils.compensateBitmapRotation(poviPath + mFileName);
                    OutputStream outStream = null;
                    try {
                        outStream = new FileOutputStream(poviPath + mFileName);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 50,outStream);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    mPicture.setImageBitmap(bmp);
                }
                else
                    Snackbar.make(mPicture, "Error renaming picture!", Snackbar.LENGTH_LONG).show();
            }

            if (requestCode == REQUEST_LOAD_PHOTO && data != null && data.getData() != null) {

                Uri uri = data.getData();
                InputStream input = null;

                File imageFile = new File(poviPath + mFileName);

                OutputStream outStream = null;
                try {
                    input = getContentResolver().openInputStream(uri);
                    outStream = new FileOutputStream(imageFile);
                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        outStream.write(buffer, 0, read);
                    }
                    outStream.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Snackbar.make(mPicture, "Error loading picture!", Snackbar.LENGTH_LONG).show();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    Snackbar.make(mPicture, "Error opening picture file!", Snackbar.LENGTH_LONG).show();
                    return;
                }
                finally {
                    try {
                        outStream.close();
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Snackbar.make(mPicture, "Error opening picture file!", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                }
                Bitmap bmp = BitmapFactory.decodeFile(poviPath + mFileName);
                mPicture.setImageBitmap(bmp);
            }
        }
    }
}



