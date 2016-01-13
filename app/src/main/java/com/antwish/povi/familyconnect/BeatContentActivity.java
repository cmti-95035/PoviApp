package com.antwish.povi.familyconnect;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.antwish.povi.server.BeatComment;
import com.antwish.povi.server.BeatCommentArray;
import com.antwish.povi.server.BeatComments;
import com.antwish.povi.server.User;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.linkedin.data.template.GetMode;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BeatContentActivity extends AppCompatActivity implements SurfaceHolder.Callback{
    private static final String TAG = "BeatContentActivity";
    private Toolbar mToolbar;
    private Tracker mTracker;
    private TextView mFirstLetter;
    private TextView mAuthor;
    private TextView mComment;
    private TextView mLikesNr;
    private TextView mCommentsNr;
    private TextView mDescription;
    private ImageView mPreview;
    private FloatingActionButton mPostButton;
    private EditText mCommentText;
    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private LoaderManager.LoaderCallbacks<BeatComments> repliesCallback;
    private BeatCommentArray mReplies = new BeatCommentArray();
    private int totalReplies = 0;
    private long contentId;
    private String contentOwnerId;
    private Context context;
    private int avatarColor;
    private Button mLikeButton;
    private boolean mLikeStatus;
    private String mMediaFileName;
    private LinearLayout mControls;
    private ImageButton mPlayButton;
    private ImageButton mPauseButton;
    private ImageButton mStopButton;
    private MediaPlayer mPlayer;
    private SurfaceView mVideoPreview;
    private URL mediaUrl;
    private Content content;
    private SurfaceHolder holder;
    private FrameLayout mFrame;
    private boolean paused = false;
    private int callerID = -1;


    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        mTracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        mTracker.setScreenName("Beat content screen");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Set layout
        setContentView(R.layout.activity_beat_content);

        context = this;

        // Get bundle
        Bundle bundle = this.getIntent().getExtras();
        content = bundle.getParcelable("content");
        contentId = content.commentId;
        contentOwnerId = content.userId;
        avatarColor = bundle.getInt("avatar_color");
        callerID = bundle.getInt("callerID");

        // Set up controls
        mFirstLetter = (TextView) findViewById(R.id.firstLetter);
        mAuthor = (TextView) findViewById(R.id.author);
        mLikesNr = (TextView) findViewById(R.id.likesNr);
        mCommentsNr = (TextView) findViewById(R.id.commentsNr);
        mDescription = (TextView) findViewById(R.id.description);
        mComment = (TextView) findViewById(R.id.comment);
        mPreview = (ImageView) findViewById(R.id.picturePreview);
        mControls = (LinearLayout) findViewById(R.id.controls);
        mPlayButton = (ImageButton) findViewById(R.id.playButton);
        mPauseButton = (ImageButton) findViewById(R.id.pauseButton);
        mStopButton = (ImageButton) findViewById(R.id.stopButton);
        mVideoPreview = (SurfaceView) findViewById(R.id.videoPreview);
        mFrame = (FrameLayout) findViewById(R.id.frameLayout);
        holder = mVideoPreview.getHolder();
        //holder.setFixedSize(0, 0);
        holder.setKeepScreenOn(true);
        holder.addCallback(this);

        if (content.displayedName != null)
            mFirstLetter.setText(String.valueOf(content.displayedName.charAt(0)).toUpperCase());
        else
            mFirstLetter.setText(String.valueOf(content.userId.charAt(0)).toUpperCase());
        GradientDrawable circle = (GradientDrawable) mFirstLetter.getBackground();
        circle.setColor(avatarColor);
        mAuthor.setText(content.displayedName);
        mLikesNr.setText(Integer.toString(content.likeCount));
        mCommentsNr.setText(Integer.toString(content.commentCount));
        mComment.setText(content.commentText);
        mDescription.setText(content.description);

        mCommentText = (EditText) findViewById(R.id.commentText);

        // Disable unused controls
        mMediaFileName = content.mediaFileName;
        //mMediaFileName = "neohack87@yahoo.it_Cenerella_52_20150823.mp4";

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlaying();
                //playStatus = 1; // playing
                mPlayButton.setEnabled(false);
                mPauseButton.setEnabled(true);
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlaying();
                //playStatus = 2;
                mPlayButton.setEnabled(true);
                mPauseButton.setEnabled(false);
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.pause();
                paused = true;
                mPlayButton.setEnabled(true);
                mPauseButton.setEnabled(false);
            }
        });


        switch (content.commentGroup){
            case 1:
                mDescription.setVisibility(View.GONE);
                mFrame.setVisibility(View.GONE);
                mControls.setVisibility(View.GONE);
                mPreview.setVisibility(View.GONE);

                    break;
            case 2:
                    mComment.setVisibility(View.GONE);
                    mDescription.setVisibility(View.VISIBLE);
                    mPreview.setVisibility(View.VISIBLE);
                    mVideoPreview.setVisibility(View.GONE);
                    mControls.setVisibility(View.VISIBLE);
                    mPreview.setImageResource(R.drawable.audio_header);

                    // retrieve file uri and set media player
                    mediaUrl = PoviUtils.generateMediaUrl(this, mMediaFileName);
                    break;

            case 3:
                mComment.setVisibility(View.GONE);
                mFrame.setVisibility(View.GONE);
                mDescription.setVisibility(View.VISIBLE);
                mPreview.setVisibility(View.VISIBLE);
                mVideoPreview.setVisibility(View.GONE);
                mControls.setVisibility(View.GONE);
                if (content.thumbnail != null)
                    mPreview.setImageBitmap(BitmapFactory.decodeByteArray(content.thumbnail, 0, content.thumbnail.length));
                else
                    mPreview.setImageResource(R.drawable.tip_header);
                break;

            case 4:
                    mComment.setVisibility(View.GONE);
                    mDescription.setVisibility(View.VISIBLE);
                    mPreview.setVisibility(View.GONE);
                    mVideoPreview.setVisibility(View.VISIBLE);
                    mControls.setVisibility(View.GONE);
                    //mControls.setVisibility(View.VISIBLE);
                // retrieve file uri and set media player
                mediaUrl = PoviUtils.generateMediaUrl(this, mMediaFileName);


                    break;
        }

        mPostButton = (FloatingActionButton) findViewById(R.id.postButton);
        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCommentText.getEditableText().toString() != null){
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    final String currentToken = sharedPref.getString(POVI_TOKEN, "");
                    final String currentUser = sharedPref.getString(POVI_USERID, "");
                    // Send post
                    User user = RestServer.getUserProfile(currentToken);
                    if (user == null){
                        Snackbar.make(mCommentText, "Error publishing comment!", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Long replyId = RestServer.addBeatComment(currentToken, contentId, user.getEmail(), mCommentText.getEditableText().toString());
                    if (replyId == null)
                        Snackbar.make(mCommentText, "Error publishing comment!", Snackbar.LENGTH_LONG).show();
                    else{
                        // Add comment to recycler view
                        Date todayDate = new Date();
                        Calendar date = Calendar.getInstance();
                        date.setTime(todayDate);
                        // Set timestamp
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                        //String dateStr = sdf.format(date.getTime());
                        mReplies.add(new BeatComment().setBeatCommentId(replyId).setBeatId(0).setTimestamp(date.getTimeInMillis()).setCommentorEmail(currentUser).setCommentorName(user.getNickName(GetMode.NULL)).setCommentText(mCommentText.getEditableText().toString()));
                        mAdapter.notifyItemInserted(mReplies.size() - 1);
                        mCommentsNr.setText(Integer.toString(mAdapter.getItemCount()));
                        mCommentText.setText(null);
                    }

                }
            }
        });

        mLikeButton = (Button) findViewById(R.id.likeButton);
        mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final String currentToken = sharedPref.getString(POVI_TOKEN, "");
                mLikeStatus = !mLikeStatus;
                RestServer.setLikeStatus(content.commentId, true, mLikeStatus, currentToken);
                    if (mLikeStatus) {
                        mLikeButton.setText("I don't like it anymore");
                        // Increment like counter
                        int likes = Integer.parseInt(mLikesNr.getText().toString());
                        mLikesNr.setText(Integer.toString(likes + 1));
                    }
                    else{
                        mLikeButton.setText("I like it");
                        // Decrement like counter
                        int likes = Integer.parseInt(mLikesNr.getText().toString());
                        mLikesNr.setText(Integer.toString(likes - 1));
                    }
                }

        });

        // Setting up Toolbar
        setUpToolbar();

        repliesCallback = new LoaderManager.LoaderCallbacks<BeatComments>() {
            @Override
            public Loader<BeatComments> onCreateLoader(int id, Bundle args) {
                return new BeatCommentLoader(getApplicationContext(), contentId, args.getInt("start"),args.getInt("stop"),args.getLong("lastreplyid"));
            }

            @Override
            public void onLoadFinished(Loader<BeatComments> loader, BeatComments data) {
                if (data != null) {
                    mReplies.addAll(data.getBeatComments());
                    mAdapter.notifyItemRangeInserted(mAdapter.getItemCount(), data.getBeatComments().size());
                    mCommentsNr.setText(Integer.toString(mAdapter.getItemCount()));
                    // Set like status
                    mLikeStatus = data.isLikeStatus();
                    if (mLikeStatus)
                        mLikeButton.setText("I don't like it anymore");
                    else
                        mLikeButton.setText("I like it");
                }
            }

            @Override
            public void onLoaderReset(Loader<BeatComments> loader) {
                // Set new data
            }
        };

        mRecyclerView = (RecyclerView) findViewById(R.id.repliesList);
        //mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter
        mAdapter = new MyAdapter(mReplies);
        mRecyclerView.setAdapter(mAdapter);

        // implement scrolllistener to fetch new paged data from the server
        mRecyclerView.addOnScrollListener(new MyRecyclerViewOnScrollListener());

        mPlayer = new MediaPlayer();

        Bundle args = new Bundle();
        args.putLong("contentId", contentId);
        args.putInt("start", 0);
        args.putInt("count", 10);
        args.putLong("lastreplyid", -1);
        getLoaderManager().initLoader(0, args, repliesCallback);

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
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mPlayer.setDisplay(surfaceHolder);
        mControls.setVisibility(View.VISIBLE);

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<BeatComment> mReplies;

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener{
            private TextView firstLetter;
            private TextView author;
            private TextView reply;
            private TextView date;
            private String authorId;
            private long beatCommentId;

            public ViewHolder(View itemView) {
                super(itemView);
                firstLetter = (TextView) itemView.findViewById(R.id.firstLetter);
                author = (TextView) itemView.findViewById(R.id.author);
                reply = (TextView) itemView.findViewById(R.id.reply);
                date = (TextView) itemView.findViewById(R.id.date);
                itemView.setOnLongClickListener(this);
            }


            @Override
            public boolean onLongClick(View v) {
                // Open popup menu
                final CharSequence[] options = {"Delete reply"};

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, "");
                final String userToken = sharedPref.getString(PoviConstants.POVI_TOKEN, "");

                // Open dialog if the user is the owner of the reply or the content
                if (userEmail.compareTo(contentOwnerId) == 0 || userEmail.compareTo(mReplies.get(getAdapterPosition()).getCommentorEmail()) == 0){
                    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Set reply action");
                    // Discriminate the fact the user owns the content
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            // Delete beat comment
                            boolean res = RestServer.deleteBeatComment(userToken, beatCommentId);
                            if (!res)
                                Snackbar.make(mRecyclerView, "Error deleting comment!", Snackbar.LENGTH_LONG).show();
                            else{
                                // Update adapter
                                mReplies.remove(getAdapterPosition());
                                mAdapter.notifyItemRemoved(getAdapterPosition());
                                mCommentsNr.setText(Integer.toString(mAdapter.getItemCount()));
                            }

                        }
                    });
                    builder.create().show();
                }
                return true;
            }
        }

        public MyAdapter(BeatCommentArray replies){
            mReplies = replies;
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reply_list_item, parent, false);
            ViewHolder vh = new ViewHolder(view);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (mReplies != null && mReplies.size() > position) {
                holder.beatCommentId = mReplies.get(position).getBeatCommentId();
                holder.reply.setText(mReplies.get(position).getCommentText());
                // Set circle fill color
                GradientDrawable circle = (GradientDrawable) holder.firstLetter.getBackground();
                // TODO: set nick name initial
                if (mReplies.get(position).hasCommentorName()) {
                    holder.firstLetter.setText(String.valueOf(mReplies.get(position).getCommentorName().charAt(0)).toUpperCase());
                    holder.author.setText(mReplies.get(position).getCommentorName());
                }
                circle.setColor(PoviUtils.generateMaterialColor(position));
                // TODO: set author's profile picture if available
                if (mReplies.get(position).hasTimestamp()) {
                    Date date = new Date(mReplies.get(position).getTimestamp());
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                    holder.date.setText(sdf.format(cal.getTime()));
                }
                //holder.authorId = mReplies.get(position)
                // TODO: set "aging" instead of date (e.g. 1h, 1 day, 1 month, 1 year...)
            }
        }

        @Override
        public int getItemCount() {
            if (mReplies != null)
                return mReplies.size();
            else
                return 0;
        }
    } // end MyAdapter

    public static class BeatCommentLoader extends AsyncTaskLoader<BeatComments> {
        private BeatComments mReplies;
        private Context mContext;
        private int start;
        private int count;
        private long lastreplyid;
        private long contentId;

        public BeatCommentLoader(Context context, long contentId, int start, int count, long lastreplyid) {
            super(context);
            mContext = context;
            this.start = start;
            this.count = count;
            this.lastreplyid = lastreplyid;
            this.contentId = contentId;
        }

        @Override
        public BeatComments loadInBackground() {
            // Retrieve beats from server
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String currentToken = sharedPref.getString(POVI_TOKEN, "");
            return RestServer.getBeatCommentsWithLikeStatus(currentToken, contentId);
            }



        @Override
        public void deliverResult(BeatComments data) {
            if (isReset()) {
                // The Loader has been reset; ignore the result and invalidate the data.
                //releaseResources(data);
                return;
            }

            // Hold a reference to the old data so it doesn't get garbage collected.
            // We must protect it until the new data has been delivered.
            BeatComments oldData = mReplies;
            mReplies = data;

            if (isStarted()) {
                // If the Loader is in a started state, deliver the results to the
                // client. The superclass method does this for us.
                super.deliverResult(data);
            }

            // Invalidate the old data as we don't need it any more.
            //if (oldData != null && data != null && oldData != data.getBeatComments()) {
            //    releaseResources(oldData);
            //}
        }

        @Override
        protected void onStartLoading() {
            if (mReplies != null) {
                // Deliver any previously loaded data immediately.
                deliverResult(mReplies);
            }

            // Begin monitoring the underlying data source.
            // if (mObserver == null) {
            //    mObserver = new SampleObserver();
            // TODO: register the observer
            // }

            if (takeContentChanged() || mReplies == null) {
                // When the observer detects a change, it should call onContentChanged()
                // on the Loader, which will cause the next call to takeContentChanged()
                // to return true. If this is ever the case (or if the current data is
                // null), we force a new load.
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            // The Loader is in a stopped state, so we should attempt to cancel the
            // current load (if there is one).
            cancelLoad();

            // Note that we leave the observer as is. Loaders in a stopped state
            // should still monitor the data source for changes so that the Loader
            // will know to force a new load if it is ever started again.
        }

        @Override
        protected void onReset() {
            // Ensure the loader has been stopped.
            onStopLoading();

            // At this point we can release the resources associated with 'mData'.
            //if (mComments != null) {
            //    releaseResources(mComments);
            //    mComments = null;
            //}

            // The Loader is being reset, so we should stop monitoring for changes.
            //if (mObserver != null) {
            // TODO: unregister the observer
            //    mObserver = null;
            //}
        }

        @Override
        public void onCanceled(BeatComments data) {
            // Attempt to cancel the current asynchronous load.
            super.onCanceled(data);

            // The load has been canceled, so we should release the resources
            // associated with 'data'.
            releaseResources(data);
        }

        private void releaseResources(BeatComments data) {
            // For a simple List, there is nothing to do. For something like a Cursor, we
            // would close it in this method. All resources associated with the Loader
            // should be released here.
        }
        //private SampleObserver mObserver;
    }

    private class MyRecyclerViewOnScrollListener extends RecyclerView.OnScrollListener{
        @Override
        public void onScrollStateChanged (RecyclerView recyclerView, int newState){
            super.onScrollStateChanged(recyclerView, newState);

        }

        @Override
        public void onScrolled (RecyclerView recyclerView, int dx, int dy){
            super.onScrolled(recyclerView, dx, dy);

            int lastVisibleItem = mLayoutManager.findLastCompletelyVisibleItemPosition();
            // Check last index value and in case load next page
            if (lastVisibleItem == mAdapter.getItemCount() -1 && mAdapter.getItemCount() < totalReplies) {
                Bundle bundle = new Bundle();
                bundle.putLong("contentId", contentId);
                bundle.putInt("start", mReplies.size());
                bundle.putInt("count", 10);
                bundle.putLong("lastreplyid", mReplies.get(mReplies.size()-1).getBeatCommentId());
                getLoaderManager().restartLoader(0, bundle, repliesCallback);
            }
        }

    }

    private void startPlaying() {
        if (mPlayer != null && mPlayer.isPlaying())
            return;

        if (paused) {
            mPlayer.start();
            paused = false;
            return;
        }

        try {
            Uri.Builder builder = Uri.parse(mediaUrl.toString()).buildUpon();
            Uri uri = builder.build();
            mPlayer.setDataSource(this, uri);
           // mPlayer.setDisplay(mVideoPreview.getHolder());
            if (content.commentGroup == 2)
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mPlayer.prepare();
            ViewGroup.LayoutParams lp = mVideoPreview.getLayoutParams();
            lp.width = mPlayer.getVideoWidth();
            lp.height = mPlayer.getVideoHeight();
            mVideoPreview.setLayoutParams(lp);
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
            Snackbar.make(mToolbar, "Unable to play the recorded comment!", Snackbar.LENGTH_LONG).show();
        }
    }

    private void stopPlaying() {
        if (mPlayer == null)
            return;
        mPlayer.stop();
        //mPlayer.release();
        //mPlayer = null;
    }

    @Override
    public void onBackPressed(){
        if (callerID < 0)
            super.onBackPressed();
        else {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("callerID", callerID);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_beatcontent, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
               // if (callerID >= 0) {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.putExtra("callerID", callerID);
                startActivity(intent);
                    finish();
         //   }
                return true;


        }
        return false;
    }

}



