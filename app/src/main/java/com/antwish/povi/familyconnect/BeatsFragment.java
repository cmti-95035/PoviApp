package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.antwish.povi.server.Comment;
import com.antwish.povi.server.EventType;
import com.antwish.povi.server.ParentingTip;
import com.antwish.povi.server.ParentingTipCategory;
import com.antwish.povi.server.ParentingTipId;
import com.antwish.povi.server.User;
import com.antwish.povi.server.WebLink;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.linkedin.data.template.GetMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A fragment that shows the tips on the screen and on selection
 * of a tip allows us to create an entry into the journal for recording
 * comments.
 *
 * <p>This class is called by the {@link DashboardActivity}</p>
 */

public class BeatsFragment extends Fragment {
    public static final String FROM_JOURNAL_REMINDER = "journal_reminder";
    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";

    private static final String TAG = "BeatsFragment";
    private OnTitleChangeListener titleChangeListener;

    private Tracker tracker;

    private View rootView;

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private StaggeredGridLayoutManager mLayoutManager;

    private TabLayout tabs;
    private TextView beatCategory;
    private TextView beatText;
    private ImageButton textButton;
    private ImageButton pictureButton;
    private ImageButton videoButton;
    private ImageButton audioButton;
    private Spinner mDateSpinner;
    private  TextView expert_view;
    private TextView age5less;
    private TextView age510;
    private TextView ageTeen;

    private LoaderManager.LoaderCallbacks<List<ParentingTip>> beatCallback;
    private LoaderManager.LoaderCallbacks<List<Comment>> commentCallback;

    private List<List<Comment>> mComments = new ArrayList<>();
    private List<ParentingTip> mBeats = new ArrayList<>();
    private OnBeatsToolbarElevationListener toolbarElevationListener;

    private ProgressBar mProgressBar;

    public interface OnBeatsToolbarElevationListener{
        void onToolbarElevationListener(float elevation);
    }

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given argument.
     */
    public static BeatsFragment newInstance() {
        BeatsFragment fragment = new BeatsFragment();
        return fragment;
    }

    public BeatsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) getActivity().getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        Log.i("GAV4", "got tracker");
        tracker.setScreenName("Beats screen");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
        Log.i("GAV4", "sent screen");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        final String currentToken = sharedPref.getString(POVI_TOKEN, "ccfb52c193e57a7745d6e7e434ea206446b1c739");
        final String currentUser = sharedPref.getString(POVI_USERID, "a1@gmail.com");

        if(!currentToken.isEmpty() && !currentUser.isEmpty()) {
            boolean journalNotification = getActivity().getIntent().getBooleanExtra(FROM_JOURNAL_REMINDER, false);
           // if (journalNotification)
               // new SendEventTask().execute(currentToken, currentUser, "From journal reminder", EventType.NOTIFICATION.toString());

            //boolean tipNotification = getActivity().getIntent().getBooleanExtra(FROM_TIP_REMINDER, false);
           // if (tipNotification)
               // new SendEventTask().execute(currentToken, currentUser, "From tip reminder", EventType.NOTIFICATION.toString());
        }

        setHasOptionsMenu(true);

        beatCallback = new LoaderManager.LoaderCallbacks<List<ParentingTip>>() {
            @Override
            public Loader<List<ParentingTip>> onCreateLoader(int id, Bundle args) {
                mProgressBar.setVisibility(View.VISIBLE);
                return new BeatsLoader(getActivity(), args.getString("date"), args.getBoolean("refresh_tips"));
            }

            @Override
            public void onLoadFinished(Loader<List<ParentingTip>> loader, List<ParentingTip> data) {
                mComments = new ArrayList<>();
                for (ParentingTip beat:data)
                    mComments.add(new ArrayList<Comment>());
                mAdapter.notifyDataSetChanged();
                mBeats = data;
                tabs.removeAllTabs();
                for (int i = 0; i < data.size(); ++i)
                    tabs.addTab(tabs.newTab().setText("Beat " + Integer.toString(i + 1)));
                tabs.getTabAt(0).select();
                if (data != null && data.size() > 0){
                    //mComments = new ArrayList<>();
                    for (int i = 0; i < data.size(); ++i) {
                        //mComments.add(new ArrayList<Comment>());
                        // Load comments
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("beatId", new BeatId(data.get(i).getTipId()));
                        bundle.putInt("start", 0);
                        bundle.putInt("count", 10);
                        bundle.putLong("lastcommentid", -1);
                        getLoaderManager().restartLoader(i + 1, bundle, commentCallback);
                    }
                }
            }

            @Override
            public void onLoaderReset(Loader<List<ParentingTip>> loader) {

            }
        };

        commentCallback = new LoaderManager.LoaderCallbacks<List<Comment>>() {
            @Override
            public Loader<List<Comment>> onCreateLoader(int id, Bundle args) {
                BeatId beatId = args.getParcelable("beatId");
                ParentingTipId tipId = new ParentingTipId();
                tipId.setTipResourceId(beatId.resourceId).setTipSequenceId(beatId.sequenceId);
                int start = args.getInt("start");
                int count = args.getInt("count");
                long lastcommentid = args.getLong("lastcommentid");
                return new CommentLoader(getActivity(), tipId, start, count, lastcommentid);
            }

            @Override
            public void onLoadFinished(Loader<List<Comment>> loader, List<Comment> data) {
                mComments.get(loader.getId()-1).addAll(data);
                if (loader.getId() - 1 == tabs.getSelectedTabPosition())
                    mAdapter.notifyItemRangeInserted(mAdapter.getItemCount(), data.size());
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoaderReset(Loader<List<Comment>> loader) {
                // Set new data


            }
        };


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_beats, container, false);
        beatCategory = (TextView) rootView.findViewById(R.id.beatCategory);
        beatText = (TextView) rootView.findViewById(R.id.beatText);
        age5less = (TextView) rootView.findViewById(R.id.ageLess5);
        age510 = (TextView) rootView.findViewById(R.id.age510);
        ageTeen = (TextView) rootView.findViewById(R.id.ageTeen);
        // Set tip category and text
        beatCategory.setText("");
        beatText.setText("");

        tabs = (TabLayout) rootView.findViewById(R.id.tab);
        tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Replace contents
                beatCategory.setText(categoryEnumToString(mBeats.get(tab.getPosition()).getTipCategory()));
                beatText.setText(mBeats.get(tab.getPosition()).getTipDetail());
                switch(mBeats.get(tab.getPosition()).getTipAgeGroups()){
                    case 1: // preschool
                        age5less.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age5less.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        age510.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        age510.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        ageTeen.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        ageTeen.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        break;
                    case 2: // 5-10
                        age5less.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        age5less.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age510.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age510.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        ageTeen.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        ageTeen.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        break;
                    case 3:
                        age5less.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age5less.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        age510.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age510.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        ageTeen.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        ageTeen.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        break;
                    case 4: // teen
                        age5less.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        age5less.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age510.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        age510.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        ageTeen.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        ageTeen.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        break;
                    case 6: // teen
                        age5less.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        age5less.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age510.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age510.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        ageTeen.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        ageTeen.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        break;
                    case 7: // teen
                        age5less.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age5less.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        age510.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age510.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        ageTeen.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        ageTeen.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        break;
                    default: // teen
                        age5less.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        age5less.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        age510.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        age510.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        ageTeen.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
                        ageTeen.setTextColor(ContextCompat.getColor(getActivity(), R.color.red));
                        break;
                }
                mAdapter.changeContents(mComments.get(tab.getPosition()));
                // Populate recycler view
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.mediaView);
        //mRecyclerView.setHasFixedSize(true);

        // use a staggered grid layout manager
        mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter
        mAdapter = new MyAdapter(null);
        mRecyclerView.setAdapter(mAdapter);

        // implement scrolllistener to fetch new paged data from the server
        mRecyclerView.addOnScrollListener(new MyRecyclerViewOnScrollListener());

        // Link buttons
        textButton = (ImageButton) rootView.findViewById(R.id.textButton);
        pictureButton = (ImageButton) rootView.findViewById(R.id.pictureButton);
        videoButton = (ImageButton) rootView.findViewById(R.id.videoButton);
        audioButton = (ImageButton) rootView.findViewById(R.id.audioButton);

        textButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start video recording activity
                Intent recordsActivity = new Intent(getActivity(), RecordTextActivity.class);
                recordsActivity.putExtra("beat", new Beat(mBeats.get(tabs.getSelectedTabPosition())));
                startActivity(recordsActivity);
            }
        });
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start picture recording activity
                Intent recordsActivity = new Intent(getActivity(), RecordPictureActivity.class);
                recordsActivity.putExtra("beat", new Beat(mBeats.get(tabs.getSelectedTabPosition())));
                startActivity(recordsActivity);
            }
        });
        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start video recording activity
                Intent recordsActivity = new Intent(getActivity(), RecordVideoActivity.class);
                recordsActivity.putExtra("beat", new Beat(mBeats.get(tabs.getSelectedTabPosition())));
                startActivity(recordsActivity);
            }
        });
        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start audio recording activity
                Intent recordsActivity = new Intent(getActivity(), RecordAudioActivity.class);
                recordsActivity.putExtra("beat", new Beat(mBeats.get(tabs.getSelectedTabPosition())));
                startActivity(recordsActivity);
            }
        });

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        mDateSpinner = (Spinner) getActivity().findViewById(R.id.date_spinner);
        ArrayList<String> options = new ArrayList<>();
        options.add("Today");
        options.add("Yesterday");
        options.add("Two days ago");
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.toolbar_simple_spinner_item, options); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(R.layout.toolbar_spinner_dropdown_item);
        mDateSpinner.setAdapter(spinnerArrayAdapter);
        mDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Generate date
                Date todayDate = new Date();
                Calendar date = Calendar.getInstance();
                date.setTime(todayDate);
                date.add(Calendar.DATE, -position);  // number of days to subtract
                Bundle bundle = new Bundle();
                // Set timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                String dateStr = sdf.format(date.getTime());
                bundle.putString("date", dateStr);
                bundle.putBoolean("refresh_tips", false);
                getLoaderManager().restartLoader(0, bundle, beatCallback);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mDateSpinner.setVisibility(View.VISIBLE);
        expert_view = (TextView)rootView.findViewById(R.id.expertsLink);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        final String currentToken = sharedPref.getString(POVI_TOKEN, "ccfb52c193e57a7745d6e7e434ea206446b1c739");
        final WebLink webLink = RestServer.getWebLink(currentToken);
        if (webLink != null){
            String text="<a href='" + webLink.getLink() + "'> <b>" + webLink.getTitle() + "</b></a>";
            expert_view.setText(Html.fromHtml(text));
            expert_view.setClickable(true);
            expert_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openExpertBlog(webLink);

                    // send an event to indicate user visits web link provided from the tip of the day page
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                    final String currentToken = sharedPref.getString(POVI_TOKEN, "ccfb52c193e57a7745d6e7e434ea206446b1c739");
                    final String currentUser = sharedPref.getString(POVI_USERID, "a1@gmail.com");
                    if(!currentToken.isEmpty() && !currentUser.isEmpty())
                        new SendEventTask().execute(currentToken, currentUser, "Visit weblink: " + webLink.getLink(GetMode.NULL), EventType.TIPOFTHEDAY.toString());
                }
            });
        }

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        return rootView;
    }

    /**
     * Called once the fragment is associated with its activity
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            titleChangeListener = (OnTitleChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTitleChangeListener");
        }
        try {
            toolbarElevationListener = (OnBeatsToolbarElevationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnBeatsToolbarElevationListener");
        }
    }

    /**
     * Called immediately prior to fragment being no longer associated with its activity
     */
    @Override
    public void onDetach() {
        super.onDetach();
        titleChangeListener = null;
        toolbarElevationListener = null;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (titleChangeListener != null)
            titleChangeListener.onTitleChangeListener("My beats");
        // Disable toolbar elevation
        toolbarElevationListener.onToolbarElevationListener(0);
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        // Re-enable toolbar elevation
        final float scale = getResources().getDisplayMetrics().density;
        int elevation = (int) (4 * scale + 0.5f);
        toolbarElevationListener.onToolbarElevationListener(elevation);

        // Hide spinner
        mDateSpinner.setVisibility(View.GONE);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_beats, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        final String currentToken = sharedPref.getString(POVI_TOKEN, "ccfb52c193e57a7745d6e7e434ea206446b1c739");
        switch(item.getItemId()){
            case R.id.refresh_tips:
                // Get date
                // Restart loader
                Date todayDate = new Date();
                Calendar date = Calendar.getInstance();
                date.setTime(todayDate);
                Bundle bundle = new Bundle();
                // Set timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                String dateStr = sdf.format(date.getTime());
                bundle.putString("date", dateStr);
                bundle.putBoolean("refresh_tips", true);
                getLoaderManager().restartLoader(0, bundle, beatCallback);
                return true;

            case R.id.tipjar_invite:
                // Open e-mail intent
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "I am using Povi Family Connect app");
                intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(getResources().getString(R.string.invitation_email)));
                startActivity(Intent.createChooser(intent, "Invite friends"));
                return true;
        }
        return false;
    }

    // NEW CODE
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        // Prepare the loader. Either re-connect with an existing one, or start a new one
        // Generate date
        Date todayDate = new Date();
        Calendar date = Calendar.getInstance();
        date.setTime(todayDate);
        Bundle bundle = new Bundle();
        // Set timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        String dateStr = sdf.format(date.getTime());
        bundle.putString("date", dateStr);
        bundle.putBoolean("refresh_tips", false);
        getLoaderManager().initLoader(0, bundle, beatCallback);
    }

    public static class BeatsLoader extends AsyncTaskLoader<List<ParentingTip>>{
        private List<ParentingTip> mBeats;
        private Context mContext;
        private String dateStr;
        private boolean refreshTips;

        public BeatsLoader(Context context, String dateStr, boolean refreshTips) {
            super(context);
            mContext = context;
            this.dateStr = dateStr;
            this.refreshTips = refreshTips;
        }

        @Override
        public List<ParentingTip> loadInBackground() {
            // Retrieve beats from server
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String currentToken = sharedPref.getString(POVI_TOKEN, "ccfb52c193e57a7745d6e7e434ea206446b1c739");
            User currentUser = RestServer.getUserProfile(currentToken);
            String currentEmail = currentUser.getEmail(GetMode.DEFAULT);
            List<ParentingTip> data = null;
            if (refreshTips)
                data = Arrays.asList(RestServer.getRefreshTips(currentToken, currentEmail));
            else
                data = Arrays.asList(RestServer.getTipsSelectedDay(currentToken, currentEmail, dateStr));
            return data;
        }

        @Override
        public void deliverResult(List<ParentingTip> data) {
            if (isReset()) {
                // The Loader has been reset; ignore the result and invalidate the data.
                releaseResources(data);
                return;
            }

            // Hold a reference to the old data so it doesn't get garbage collected.
            // We must protect it until the new data has been delivered.
            List<ParentingTip> oldData = mBeats;
            mBeats = data;

            if (isStarted()) {
                // If the Loader is in a started state, deliver the results to the
                // client. The superclass method does this for us.
                super.deliverResult(data);
            }

            // Invalidate the old data as we don't need it any more.
            if (oldData != null && oldData != data) {
                releaseResources(oldData);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mBeats != null) {
                // Deliver any previously loaded data immediately.
                deliverResult(mBeats);
            }

            // Begin monitoring the underlying data source.
           // if (mObserver == null) {
            //    mObserver = new SampleObserver();
                // TODO: register the observer
           // }

            if (takeContentChanged() || mBeats == null) {
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
            if (mBeats != null) {
                releaseResources(mBeats);
                mBeats = null;
            }

            // The Loader is being reset, so we should stop monitoring for changes.
            //if (mObserver != null) {
                // TODO: unregister the observer
            //    mObserver = null;
            //}
        }

        @Override
        public void onCanceled(List<ParentingTip> data) {
            // Attempt to cancel the current asynchronous load.
            super.onCanceled(data);

            // The load has been canceled, so we should release the resources
            // associated with 'data'.
            releaseResources(data);
        }

        private void releaseResources(List<ParentingTip> data) {
            // For a simple List, there is nothing to do. For something like a Cursor, we
            // would close it in this method. All resources associated with the Loader
            // should be released here.
        }

        //private SampleObserver mObserver;


    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<Comment> mMediaContents;

        public void changeContents(List<Comment> newContents){
            mMediaContents = newContents;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
            private TextView firstLetter;
            private TextView author;
            private TextView comment;
            private ImageView preview;
            private TextView likesNr;
            private TextView commentsNr;

            public ViewHolder(View itemView) {
                super(itemView);
                firstLetter = (TextView) itemView.findViewById(R.id.firstLetter);
                author = (TextView) itemView.findViewById(R.id.author);
                comment = (TextView) itemView.findViewById(R.id.comment);
                likesNr = (TextView) itemView.findViewById(R.id.likesNr);
                commentsNr = (TextView) itemView.findViewById(R.id.commentsNr);
                preview = (ImageView) itemView.findViewById(R.id.preview);
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View v) {
                // Open content details activity
                Bundle bundle = new Bundle();
                bundle.putParcelable("content", new Content(mMediaContents.get(getAdapterPosition())));
                bundle.putInt("avatar_color",  PoviUtils.generateRainbowColor(getAdapterPosition()));
                bundle.putInt("callerID",0);
                Intent detailsActivity = new Intent(getActivity(), BeatContentActivity.class);
                detailsActivity.putExtras(bundle);
                startActivity(detailsActivity);
            }

            @Override
            public boolean onLongClick(View v) {
                // Only show menu if the current user is the actual beat author
                // Open popup menu
                final CharSequence[] options = {"Delete beat"};

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                final String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, "");
                final String userToken = sharedPref.getString(PoviConstants.POVI_TOKEN, "");

                // Open dialog if the user is the owner of the the content
                if (userEmail.compareTo(mMediaContents.get(getAdapterPosition()).getUserId()) == 0){
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Set beat action");
                    // Discriminate the fact the user owns the content
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            // Delete media before, if any
                            boolean res = true;
                            if (mMediaContents.get(getAdapterPosition()).getRemoteFileName().length() > 0)
                                res = PoviUtils.deleteS3Object(getActivity(),mMediaContents.get(getAdapterPosition()).getRemoteFileName() );
                            if (!res)
                                Snackbar.make(mRecyclerView, "Error deleting beat media!", Snackbar.LENGTH_LONG).show();
                            // Delete beat
                            res = RestServer.deleteComment(userToken, mMediaContents.get(getAdapterPosition()).getCommentId());
                            if (!res)
                                Snackbar.make(mRecyclerView, "Error deleting beat!", Snackbar.LENGTH_LONG).show();
                            else{
                                // Update adapter
                                mMediaContents.remove(getAdapterPosition());
                                mAdapter.notifyItemRemoved(getAdapterPosition());
                                commentsNr.setText(Integer.toString(mAdapter.getItemCount()));
                            }
                        }
                    });
                    builder.create().show();
                }
                return true;
            }
        }

        public MyAdapter(List<Comment> mediaContents){
            mMediaContents = mediaContents;
        }

        @Override
        public int getItemViewType(int position) {
            // Just as an example, return 0 or 2 depending on position
            // Note that unlike in ListView adapters, types don't have to be contiguous
            return mMediaContents.get(position).getContentGroups();
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View view = view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_card, parent, false);
            ViewHolder vh = new ViewHolder(view);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // Get element from dataset at current position
            // Replace content view with element
            //holder.firstLetter.setText(mMediaContents.get(position).get);
            if (mMediaContents != null && mMediaContents.size() > position) {
                //holder.comment.setText(mMediaContents.get(position).getCommentText());
                // Set circle fill color
                GradientDrawable circle = (GradientDrawable) holder.firstLetter.getBackground();
                // TODO: set nick name initial
                if (mMediaContents.get(position).hasAuthorNickName() && mMediaContents.get(position).getAuthorNickName().length() > 0){
                    holder.firstLetter.setText(String.valueOf(mMediaContents.get(position).getAuthorNickName().charAt(0)).toUpperCase());
                    holder.author.setText(mMediaContents.get(position).getAuthorNickName(GetMode.NULL));
                }
                else
                    holder.firstLetter.setText(String.valueOf(mMediaContents.get(position).getUserId().charAt(0)).toUpperCase());

                circle.setColor(PoviUtils.generateMaterialColor(position));
                //if (mMediaContents.get(position).hasLikeCount())
                    holder.likesNr.setText(Integer.toString(mMediaContents.get(position).getLikeCount(GetMode.NULL)));
                //if (mMediaContents.get(position).hasCommentCount())
                    holder.commentsNr.setText(Integer.toString(mMediaContents.get(position).getCommentCount(GetMode.NULL)));
                //if (mMediaContents.get(position).getThumbnailImage(GetMode.NULL) != null)
                //    holder.preview.setImageBitmap(BitmapFactory.decodeStream(mMediaContents.get(position).getThumbnailImage().asInputStream()));
                switch (mMediaContents.get(position).getContentGroups()) {
                    case 1:
                        holder.comment.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        holder.comment.setText(mMediaContents.get(position).getCommentText());
                        holder.comment.setBackgroundColor(PoviUtils.generateMaterialColor(position));
                        break;
                    case 2:
                        holder.comment.setTextColor(ContextCompat.getColor(getActivity(), R.color.black));
                        holder.comment.setText(mMediaContents.get(position).getTextDescription());
                        holder.preview.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.audio_header));
                        holder.preview.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        holder.comment.setTextColor(ContextCompat.getColor(getActivity(), R.color.black));
                        holder.comment.setText(mMediaContents.get(position).getTextDescription());
                        holder.preview.setImageBitmap(BitmapFactory.decodeStream(mMediaContents.get(position).getThumbnailImage().asInputStream()));
                        holder.preview.setVisibility(View.VISIBLE);
                        break;
                    case 4:
                        holder.comment.setTextColor(ContextCompat.getColor(getActivity(), R.color.black));
                        holder.comment.setText(mMediaContents.get(position).getTextDescription());
                        holder.preview.setImageBitmap(BitmapFactory.decodeStream(mMediaContents.get(position).getThumbnailImage().asInputStream()));
                        holder.preview.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }

        @Override
        public int getItemCount() {
            if (mMediaContents != null)
                return mMediaContents.size();
            else
                return 0;
        }
    } // end MyAdapter

    public static class CommentLoader extends AsyncTaskLoader<List<Comment>> {
        private List<Comment> mComments;
        private ParentingTipId mTipId;
        private Context mContext;
        private int start;
        private int count;
        private long lastcommentid;

        public CommentLoader(Context context, ParentingTipId id, int start, int count, long lastcommentid) {
            super(context);
            mContext = context;
            mTipId = id;
            this.start = start;
            this.count = count;
            this.lastcommentid = lastcommentid;
        }

        @Override
        public List<Comment> loadInBackground() {
            // Retrieve beats from server
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String currentToken = sharedPref.getString(POVI_TOKEN, "ccfb52c193e57a7745d6e7e434ea206446b1c739");
            List<Comment> data = null;
            if (mTipId != null)
                if (lastcommentid != -1)
                data = RestServer.getCommentsShared(mTipId, currentToken, start, count, lastcommentid);
            else
                    data = RestServer.getCommentsShared(mTipId, currentToken, start, count, null);

            if (data == null)
                data = new ArrayList<>();
            return data;
        }

        @Override
        public void deliverResult(List<Comment> data) {
            if (isReset()) {
                // The Loader has been reset; ignore the result and invalidate the data.
                //releaseResources(data);
                return;
            }

            // Hold a reference to the old data so it doesn't get garbage collected.
            // We must protect it until the new data has been delivered.
            List<Comment> oldData = mComments;
            mComments = data;

            if (isStarted()) {
                // If the Loader is in a started state, deliver the results to the
                // client. The superclass method does this for us.
                super.deliverResult(data);
            }

            // Invalidate the old data as we don't need it any more.
            if (oldData != null && oldData != data) {
                releaseResources(oldData);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mComments != null) {
                // Deliver any previously loaded data immediately.
                deliverResult(mComments);
            }

            // Begin monitoring the underlying data source.
            // if (mObserver == null) {
            //    mObserver = new SampleObserver();
            // TODO: register the observer
            // }

            if (takeContentChanged() || mComments == null) {
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
        public void onCanceled(List<Comment> data) {
            // Attempt to cancel the current asynchronous load.
            super.onCanceled(data);

            // The load has been canceled, so we should release the resources
            // associated with 'data'.
            releaseResources(data);
        }

        private void releaseResources(List<Comment> data) {
            // For a simple List, there is nothing to do. For something like a Cursor, we
            // would close it in this method. All resources associated with the Loader
            // should be released here.
        }

        //private SampleObserver mObserver;


    }

    private String categoryEnumToString(ParentingTipCategory category)
    {
        switch (category){
            case SELFESTEEM:
                return "Self esteem";
            case ALTRUISM:
                return "Altruism";
            case EMOTION_RECOGNITION:
                return "Emotion recognition";
            case PERSPECTIVE_TAKING:
                return "Perspective taking";
            case CRITICAL_THINKING:
                return "Critical thinking";
            case SOCIAL_CONVENTION:
                return "Social convention";
            case PERCEPTION_OF_OTHERS:
                return "Perception of others";
            case STORY_TELLING:
                return "Story telling";
            case IMAGINATION_CREATIVITY:
                return "Imagination/Creativity";
            default:
                return "";
        }
    }

    private class MyRecyclerViewOnScrollListener extends RecyclerView.OnScrollListener{
        @Override
        public void onScrollStateChanged (RecyclerView recyclerView, int newState){
            super.onScrollStateChanged(recyclerView, newState);

        }

        @Override
        public void onScrolled (RecyclerView recyclerView, int dx, int dy){
            super.onScrolled(recyclerView, dx, dy);
            //int totalItemCount = mLayoutManager.getItemCount();
            //int visibleItemCount = mLayoutManager.getChildCount();
            //int[] pastVisibleItems = mLayoutManager.findFirstVisibleItemPositions(null);
            int[] lastVisibleItem = mLayoutManager.findLastCompletelyVisibleItemPositions(null);
            // Find biggest index
            int lastIndex = lastVisibleItem[0] > lastVisibleItem[1] ? lastVisibleItem[0] : lastVisibleItem[1];
            // Check last index value and in case load next page
            if (mAdapter.getItemCount() > 0 && lastIndex == mAdapter.getItemCount() -1 && mAdapter.getItemCount() < mBeats.get(tabs.getSelectedTabPosition()).getCommentCount()) {
                mProgressBar.setVisibility(View.VISIBLE);
                Bundle bundle = new Bundle();
                bundle.putParcelable("beatId", new BeatId(mBeats.get(tabs.getSelectedTabPosition()).getTipId()));
                bundle.putInt("start", mComments.get(tabs.getSelectedTabPosition()).size());
                bundle.putInt("count", 10);
                bundle.putLong("lastcommentid", mComments.get(tabs.getSelectedTabPosition()).get(mComments.get(tabs.getSelectedTabPosition()).size() - 1).getCommentId());
                getLoaderManager().restartLoader(tabs.getSelectedTabPosition() + 1, bundle, commentCallback);
            }
        }

    }

    private void openExpertBlog(WebLink webLink) {

        // Send event to GA
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Experts_blogs")
                .setAction("Click")
                .setLabel(webLink.getTitle())
                .build());
        // Clear screen name
        tracker.setScreenName(null);

        Uri webpage = Uri.parse(webLink.getLink());
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private  class SendEventTask extends AsyncTask<String, Integer, Long> {
        @Override
        protected void onPostExecute(Long result) {
            super.onPostExecute(result);

            // Directly go to the dashboard in case of success
            if (result == null) {
                Log.e(TAG, "failed to send event to server");
            }
        }

        @Override
        protected Long doInBackground(String... params) {
            return RestServer.createEvent(params[0], params[1], params[2], EventType.valueOf(params[3]));
        }
    }

}
