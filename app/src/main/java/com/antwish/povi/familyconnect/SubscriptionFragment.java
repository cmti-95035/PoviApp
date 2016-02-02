package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that shows the tips on the screen and on selection
 * of a tip allows us to create an entry into the journal for recording
 * comments.
 *
 * <p>This class is called by the {@link DashboardActivity}</p>
 */

public class SubscriptionFragment extends Fragment {


    public static final String FROM_JOURNAL_REMINDER = "journal_reminder";
    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";

    private static final String TAG = "SubscriptionFragment";
    private OnTitleChangeListener titleChangeListener;

    private Tracker tracker;

    private View rootView;

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private StaggeredGridLayoutManager mLayoutManager;

    private TabLayout tabs;

    private LoaderManager.LoaderCallbacks<List<PoviSubscription>> subscriptionCallback;

    private List<List<PoviSubscription>> mSubscriptionList = new ArrayList<>();
    private List<PoviSubscriptiontype> mSubscriptionTypes = new ArrayList<>();
    private OnSubscriptionsToolbarElevationListener toolbarElevationListener;

    private ProgressBar mProgressBar;

    public interface OnSubscriptionsToolbarElevationListener{
        void onToolbarElevationListener(float elevation);
    }

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given argument.
     */
    public static SubscriptionFragment newInstance() {
        SubscriptionFragment fragment = new SubscriptionFragment();
        return fragment;
    }

    public SubscriptionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) getActivity().getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        Log.i("GAV4", "got tracker");
        tracker.setScreenName("Subscription screen");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
        Log.i("GAV4", "sent screen");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        final String currentToken = sharedPref.getString(POVI_TOKEN, "ccfb52c193e57a7745d6e7e434ea206446b1c739");
        final String currentUser = sharedPref.getString(POVI_USERID, "a1@gmail.com");

        setHasOptionsMenu(true);

        mSubscriptionTypes.add(PoviSubscriptiontype.CATEGORY);
        mSubscriptionTypes.add(PoviSubscriptiontype.AUTHOR);
        mSubscriptionTypes.add(PoviSubscriptiontype.LIBRARY);
        subscriptionCallback = new LoaderManager.LoaderCallbacks<List<PoviSubscription>>() {
            @Override
            public Loader<List<PoviSubscription>> onCreateLoader(int id, Bundle args) {
                int start = args.getInt("start");
                int count = args.getInt("count");
                long lastSubscriptionId = args.getLong("lastSubscriptionId");
                PoviSubscriptiontype subscriptiontype = PoviSubscriptiontype.valueOf(args.getString("subscriptionType"));
                return new PoviSubscriptionLoader(getActivity(), start, count, lastSubscriptionId, subscriptiontype);
            }

            @Override
            public void onLoadFinished(Loader<List<PoviSubscription>> loader, List<PoviSubscription> data) {
                mAdapter.notifyDataSetChanged();
                tabs.removeAllTabs();
                tabs.addTab(tabs.newTab().setText("CATEGORIES"));
                tabs.addTab(tabs.newTab().setText("AUTHORS"));
                tabs.addTab(tabs.newTab().setText("LIBRARY"));
                tabs.getTabAt(0).select();
                mSubscriptionList.get(loader.getId() - 1).addAll(data);
                if (loader.getId() - 1 == tabs.getSelectedTabPosition())
                    mAdapter.notifyItemRangeInserted(mAdapter.getItemCount(), data.size());
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoaderReset(Loader<List<PoviSubscription>> loader) {
                // Set new data


            }
        };


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_subscription, container, false);

        tabs = (TabLayout) rootView.findViewById(R.id.tab);
        tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Replace contents
                switch(mSubscriptionTypes.get(tab.getPosition())){
                    case CATEGORY:
                        break;
                    case AUTHOR:
                        break;
                    case LIBRARY:
                        break;
                    default: // CATEGORY
                        break;
                }
                mAdapter.changeContents(mSubscriptionList.get(tab.getPosition()));
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
        mLayoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter
        mAdapter = new MyAdapter(null);
        mRecyclerView.setAdapter(mAdapter);

        // implement scrolllistener to fetch new paged data from the server
        mRecyclerView.addOnScrollListener(new MyRecyclerViewOnScrollListener());

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        final String currentToken = sharedPref.getString(POVI_TOKEN, "ccfb52c193e57a7745d6e7e434ea206446b1c739");

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
            toolbarElevationListener = (OnSubscriptionsToolbarElevationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSubscriptionsToolbarElevationListener");
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
            titleChangeListener.onTitleChangeListener("Subscriptions");
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

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    // NEW CODE
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(0, null, subscriptionCallback);
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<PoviSubscription> mMediaContents;

        public void changeContents(List<PoviSubscription> newContents){
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
                bundle.putInt("avatar_color",  PoviUtils.generateRainbowColor(getAdapterPosition()));
                bundle.putInt("callerID",0);
                Intent detailsActivity = new Intent(getActivity(), BeatContentActivity.class);
                detailsActivity.putExtras(bundle);
                startActivity(detailsActivity);
            }

            @Override
            public boolean onLongClick(View v) {
                return true;
            }

        }

        public MyAdapter(List<PoviSubscription> mediaContents){
            mMediaContents = mediaContents;
        }

        @Override
        public int getItemViewType(int position) {
            // Just as an example, return 0 or 2 depending on position
            // Note that unlike in ListView adapters, types don't have to be contiguous
            return mMediaContents.get(position).getSubscriptiontype().ordinal();
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

    public static class PoviSubscriptionLoader extends AsyncTaskLoader<List<PoviSubscription>> {
        private List<PoviSubscription> poviSubscriptions;
        private Context mContext;
        private int start;
        private int count;
        private long lastSubscriptionId;
        private PoviSubscriptiontype subscriptiontype;

        public PoviSubscriptionLoader(Context context, int start, int count, long lastSubscriptionId, PoviSubscriptiontype subscriptiontype) {
            super(context);
            mContext = context;
            this.start = start;
            this.count = count;
            this.lastSubscriptionId = lastSubscriptionId;
            this.subscriptiontype = subscriptiontype;
        }

        @Override
        public List<PoviSubscription> loadInBackground() {
            // Retrieve Subscriptions from server
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String currentToken = sharedPref.getString(POVI_TOKEN, "ccfb52c193e57a7745d6e7e434ea206446b1c739");
            List<PoviSubscription> data = null;
                    data = RestServer.getSubscriptionByType(currentToken, start, count, lastSubscriptionId, subscriptiontype);

            if (data == null)
                data = new ArrayList<>();
            return data;
        }

        @Override
        public void deliverResult(List<PoviSubscription> data) {
            if (isReset()) {
                // The Loader has been reset; ignore the result and invalidate the data.
                //releaseResources(data);
                return;
            }

            // Hold a reference to the old data so it doesn't get garbage collected.
            // We must protect it until the new data has been delivered.
            List<PoviSubscription> oldData = poviSubscriptions;
            poviSubscriptions = data;

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
            if (poviSubscriptions != null) {
                // Deliver any previously loaded data immediately.
                deliverResult(poviSubscriptions);
            }

            if (takeContentChanged() || poviSubscriptions == null) {
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
            //if (poviSubscriptions != null) {
            //    releaseResources(poviSubscriptions);
            //    poviSubscriptions = null;
            //}

            // The Loader is being reset, so we should stop monitoring for changes.
            //if (mObserver != null) {
            // TODO: unregister the observer
            //    mObserver = null;
            //}
        }

        @Override
        public void onCanceled(List<PoviSubscription> data) {
            // Attempt to cancel the current asynchronous load.
            super.onCanceled(data);

            // The load has been canceled, so we should release the resources
            // associated with 'data'.
            releaseResources(data);
        }

        private void releaseResources(List<PoviSubscription> data) {
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
            int[] lastVisibleItem = mLayoutManager.findLastCompletelyVisibleItemPositions(null);
            // Find biggest index
            int lastIndex = lastVisibleItem[0] > lastVisibleItem[1] ? lastVisibleItem[0] : lastVisibleItem[1];
            // Check last index value and in case load next page
            if (mAdapter.getItemCount() > 0 && lastIndex == mAdapter.getItemCount() -1) {
                mProgressBar.setVisibility(View.VISIBLE);
                Bundle bundle = new Bundle();

                bundle.putInt("start", mSubscriptionList.get(tabs.getSelectedTabPosition()).size());
                bundle.putInt("count", 10);
                bundle.putLong("lastSubscriptionId", 0);
                getLoaderManager().restartLoader(tabs.getSelectedTabPosition() + 1, bundle, subscriptionCallback);
            }
        }

    }
}
