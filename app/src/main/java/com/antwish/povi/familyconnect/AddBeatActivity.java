package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.linkedin.data.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Calendar;

import static java.io.File.separator;

public class AddBeatActivity extends AppCompatActivity {
    private static final String TAG = "AddBeatActivity";
    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";
    private Toolbar mToolbar;
    private Tracker mTracker;
    private ImageView mPicture;
    private FloatingActionButton mShareButton;
    private CheckBox mFacebookCheckbox;
    private CheckBox mCommunityCheckbox;
    private CheckBox mCirclesCheckbox;
    private EditText mDescription;
    private Activity activity;
    private CallbackManager callbackManager;
    private ShareDialog shareDialog;
    private String childName;
    private int contentGroup;
    private int tipId;
    private int resourceId;
    private String commentText;
    private String mediaFileName;
    private Context mContext;
    private String poviPath;
    private Bitmap thumbnail = null;
    private ProgressDialog addDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        poviPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + separator + "POVI"+ separator;

        // Get tracker and signals the user entered the registration screen
        mTracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        mTracker.setScreenName("Save/share beat screen");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Set layout
        setContentView(R.layout.activity_addbeat);

        mContext = this;

        activity = this;

        // Get bundle
        Bundle bundle = getIntent().getExtras();
        childName = bundle.getString("child_name");
        contentGroup = bundle.getInt("content_group");
        tipId = bundle.getInt("tip_id");
        resourceId = bundle.getInt("resource_id");
        commentText = bundle.getString("comment_text");
        mediaFileName = bundle.getString("media_file");

        // Set up controls
        mDescription = (EditText) findViewById(R.id.description);
        mPicture = (ImageView) findViewById(R.id.picture);
        if (contentGroup == 1){
            mDescription.setText(commentText);
            mDescription.setFocusable(false);
        }

        mFacebookCheckbox = (CheckBox) findViewById(R.id.facebookCheckBox);
        mCommunityCheckbox = (CheckBox) findViewById(R.id.communityCheckBox);

        mShareButton = (FloatingActionButton) findViewById(R.id.shareButton);
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Read checkboxes status and share beat in case
                Calendar calendar = Calendar.getInstance();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final String currentToken = sharedPref.getString(POVI_TOKEN, "");
                final String currentUser = sharedPref.getString(POVI_USERID, "");

                addDialog.show();

                ByteString thumbnailString = ByteString.copyString("", Charset.defaultCharset());
                if (thumbnail != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    int thumbnailQuality = contentGroup == 3 ? 50 : 100;
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, thumbnailQuality, stream);
                    thumbnailString = ByteString.copy(stream.toByteArray());
                }

                boolean res = RestServer.createBeat(currentUser, childName, tipId, resourceId, contentGroup, calendar.getTimeInMillis(), commentText, mCommunityCheckbox.isChecked(), currentToken, mediaFileName, mDescription.getText().toString(), thumbnailString);
                if (!res) {
                    addDialog.cancel();
                    // Skip rest
                    Snackbar.make(mDescription, "Error creating beat!", Snackbar.LENGTH_LONG).show();
                    return;
                }

                // Else start loading media
                if (mediaFileName != null) {
                    addDialog.cancel();
                    addDialog.setIndeterminate(false);
                    addDialog.show();
                    File mediaFile = new File(poviPath + mediaFileName);
                    PoviUtils.uploadFile(mContext, mediaFileName, mediaFile, new TransferListener() {
                        @Override
                        public void onStateChanged(int i, TransferState transferState) {
                            if (transferState == TransferState.COMPLETED)
                                finish();

                        }

                        @Override
                        public void onProgressChanged(int i, long bytesCurrent, long bytesTotal) {
                            int percentage = (int) (bytesCurrent/bytesTotal * 100);
                            addDialog.setProgress(percentage);
                        }

                        @Override
                        public void onError(int i, Exception e) {
                            addDialog.cancel();
                            Snackbar.make(mDescription, "Error uploading media!", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }

                // Share on facebook if required
                if (mFacebookCheckbox.isChecked()) {
                    if (ShareDialog.canShow(ShareLinkContent.class)) {
                        ShareLinkContent linkContent = new ShareLinkContent.Builder()
                                .setContentTitle("Hello Facebook")
                                .setContentDescription(
                                        "The 'Hello Facebook' sample  showcases simple Facebook integration")
                                .setContentUrl(Uri.parse("http://developers.facebook.com/android"))
                                .build();

                        shareDialog.show(linkContent);
                    }
                    // Share on facebook
                    // Get content type
                    switch(contentGroup){
                        case 1:
                            if (mediaFileName != null){
                                // Create image with overlapped text for text only comments
                            }
                            break;
                        case 2:
                            if (mediaFileName != null){
                                Bitmap image = BitmapFactory.decodeFile(poviPath + mediaFileName);
                                if (image != null) {
                                    SharePhoto photo = new SharePhoto.Builder().setBitmap(image).setCaption(mDescription.getEditableText().toString()).build();
                                    SharePhotoContent content = new SharePhotoContent.Builder()
                                            .addPhoto(photo)
                                            .build();
                                    ShareDialog.show(activity, content);
                                }
                            }
                            break;
                        case 3:
                            if (mediaFileName != null){
                                Bitmap image = BitmapFactory.decodeFile(poviPath + mediaFileName);
                                if (image != null) {
                                    SharePhoto photo = new SharePhoto.Builder().setBitmap(image).setCaption(mDescription.getEditableText().toString()).build();
                                    SharePhotoContent content = new SharePhotoContent.Builder()
                                            .addPhoto(photo)
                                            .build();
                                    ShareDialog.show(activity, content);
                                }
                            }
                            break;
                        case 4:
                            if (mediaFileName != null){
                                Uri videoFileUri = Uri.fromFile(new File(poviPath + mediaFileName));
                                if (videoFileUri != null) {
                                    ShareVideo video = new ShareVideo.Builder()
                                            .setLocalUrl(videoFileUri)
                                            .build();
                                    ShareVideoContent videoContent = new ShareVideoContent.Builder()
                                            .setVideo(video).setContentDescription(mDescription.getEditableText().toString())
                                            .build();
                                    ShareDialog.show(activity, videoContent);
                                }
                            }
                            break;
                    }
                }
                //finish();
            }
        });

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        // this part is optional
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {

            }


        });

        // Create thumbnail
        switch (contentGroup) {
            case 1:
                if (mediaFileName != null) {
                    thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(poviPath + mediaFileName), 512, 512);
                    mPicture.setImageBitmap(thumbnail);
                }
                else
                    mediaFileName = "";
                break;
            case 2:
                if (mediaFileName != null) {
                    thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(poviPath + mediaFileName), 512, 512);
                    mPicture.setImageBitmap(thumbnail);
                }
                else
                    mediaFileName = "";
                break;
            case 3:
                if (mediaFileName != null) {
                    thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(poviPath + mediaFileName), 512, 512);
                    mPicture.setImageBitmap(thumbnail);
                }
                else
                    mediaFileName = "";
                break;
            case 4:
                if (mediaFileName != null) {
                    thumbnail = ThumbnailUtils.createVideoThumbnail(poviPath + mediaFileName, MediaStore.Video.Thumbnails.MINI_KIND);
                }
                break;
        }

        addDialog = new ProgressDialog(this);
        addDialog.setIndeterminate(true);
        addDialog.setMessage("Please wait");
        addDialog.setTitle("Adding beat...");
        addDialog.setCancelable(false);

        // Setting up Toolbar
        setUpToolbar();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
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



    private Bitmap drawTextToBitmap(Context gContext,
                                   int gResId,
                                   String gText) {
        Resources resources = gContext.getResources();
        float scale = resources.getDisplayMetrics().density;
        Bitmap bitmap =
                BitmapFactory.decodeResource(resources, gResId);

        android.graphics.Bitmap.Config bitmapConfig =
                bitmap.getConfig();
        // set default bitmap config if none
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bitmap = bitmap.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(bitmap);
        // new antialised Paint
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.rgb(61, 61, 61));
        // text size in pixels
        paint.setTextSize((int) (14 * scale));
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        // draw text to the Canvas center
        Rect bounds = new Rect();
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width())/2;
        int y = (bitmap.getHeight() + bounds.height())/2;

        canvas.drawText(gText, x, y, paint);

        return bitmap;
    }

  /*  @Override
    public Intent getParentActivityIntent() {
        if (this.getParent().getClass() == RecordTextActivity.class)
            return new Intent(this, RecordTextActivity.class);
        if (this.getParent().getClass() == RecordAudioActivity.class)
            return new Intent(this, RecordAudioActivity.class);
        if (this.getParent().getClass() == RecordVideoActivity.class)
            return new Intent(this, RecordVideoActivity.class);
        return new Intent(this, DashboardActivity.class);
    }*/

}



