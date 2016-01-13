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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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

import com.antwish.povi.server.Child;
import com.antwish.povi.server.Comment;
import com.antwish.povi.server.User;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.linkedin.data.template.GetMode;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that shows the tips on the screen and on selection
 * of a tip allows us to create an entry into the journal for recording
 * comments.
 *
 * <p>This class is called by the {@link DashboardActivity}</p>
 */

public class TreasureChestFragment extends Fragment {
    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";

    private static final String TAG = "TreasureChestFragment";
    private OnTitleChangeListener titleChangeListener;

    private Tracker tracker;

    private View rootView;

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private StaggeredGridLayoutManager mLayoutManager;

    private TabLayout tabs;
    private LoaderManager.LoaderCallbacks<List<Child>> childrenCallback;
    private LoaderManager.LoaderCallbacks<List<Comment>> beatCallback;
    private List<Child> mChildren = new ArrayList<>();
    private List<List<Comment>> mBeats = new ArrayList<>();
    private OnBeatsToolbarElevationListener toolbarElevationListener;
    public interface OnBeatsToolbarElevationListener{
        void onToolbarElevationListener(float elevation);
    }

    private ProgressBar mProgressBar;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given argument.
     */
    public static TreasureChestFragment newInstance() {
        TreasureChestFragment fragment = new TreasureChestFragment();
        return fragment;
    }

    public TreasureChestFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) getActivity().getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        Log.i("GAV4", "got tracker");
        tracker.setScreenName("Treasure chest screen");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
        Log.i("GAV4", "sent screen");

        setHasOptionsMenu(true);

        childrenCallback = new LoaderManager.LoaderCallbacks<List<Child>>() {
            @Override
            public Loader<List<Child>> onCreateLoader(int id, Bundle args) {
                mProgressBar.setVisibility(View.VISIBLE);
                return new ChildrenLoader(getActivity(), args.getString("userId"), args.getString("accessToken"));
            }

            @Override
            public void onLoadFinished(Loader<List<Child>> loader, List<Child> data) {
                mBeats = new ArrayList<>();
                for (Child child:data)
                    mBeats.add(new ArrayList<Comment>());
                mAdapter.notifyDataSetChanged();
                mChildren = data;
                tabs.removeAllTabs();
                for (int i = 0; i < data.size(); ++i)
                    tabs.addTab(tabs.newTab().setText(data.get(i).getName()));
                tabs.getTabAt(0).select();
                if (data != null && data.size() > 0){
                    for (int i = 0; i < data.size(); ++i) {
                        // Load comments
                        Bundle bundle = new Bundle();
                        bundle.putString("userId", data.get(i).getUser_id());
                        bundle.putString("childName", data.get(i).getName());
                        bundle.putInt("start", 0);
                        bundle.putInt("count", 10);
                        bundle.putLong("lastcommentid", -1);
                        getLoaderManager().restartLoader(i + 1, bundle, beatCallback);
                    }
                }
            }

            @Override
            public void onLoaderReset(Loader<List<Child>> loader) {

            }
        };

        beatCallback = new LoaderManager.LoaderCallbacks<List<Comment>>() {
            @Override
            public Loader<List<Comment>> onCreateLoader(int id, Bundle args) {
                String userId = args.getString("userId");
                String childName = args.getString("childName");
                int start = args.getInt("start");
                int count = args.getInt("count");
                long lastcommentid = args.getLong("lastcommentid");
                return new BeatLoader(getActivity(), childName, start, count, lastcommentid);
            }

            @Override
            public void onLoadFinished(Loader<List<Comment>> loader, List<Comment> data) {
                mBeats.get(loader.getId()-1).addAll(data);
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
        rootView = inflater.inflate(R.layout.fragment_treasurechest, container, false);
        tabs = (TabLayout) rootView.findViewById(R.id.tab);
        tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Replace contents
                mAdapter.changeContents(mBeats.get(tab.getPosition()));
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

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        return rootView;
    }

    /**
     * Called once the fragment is associated with its activity
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            titleChangeListener = (OnTitleChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnTitleChangeListener");
        }
        try {
            toolbarElevationListener = (OnBeatsToolbarElevationListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnBeatsToolbarElevationListener");
        }
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        try {
            titleChangeListener = (OnTitleChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnTitleChangeListener");
        }
        try {
            toolbarElevationListener = (OnBeatsToolbarElevationListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
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
            titleChangeListener.onTitleChangeListener("My treasure chest");
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_treasurechest, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.home:
                int a = 0;
                return true;

        }
        return false;
    }

    // NEW CODE
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        // Prepare the loader. Either re-connect with an existing one, or start a new one
        // Start a loader for each child

        Bundle bundle = new Bundle();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String currentToken = sharedPref.getString(POVI_TOKEN, "");
        User currentUser = RestServer.getUserProfile(currentToken);
        String currentEmail = currentUser.getEmail(GetMode.DEFAULT);
        bundle.putString("userId", currentEmail);
        bundle.putString("accessToken", currentToken);
        getLoaderManager().initLoader(0, bundle, childrenCallback);
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<Comment> mBeats;

        public void changeContents(List<Comment> newBeats){
            mBeats = newBeats;
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
                bundle.putParcelable("content", new Content(mBeats.get(getAdapterPosition())));
                bundle.putInt("avatar_color", PoviUtils.generateRainbowColor(getAdapterPosition()));
                bundle.putInt("callerID",1);
                Intent detailsActivity = new Intent(getActivity(), BeatContentActivity.class);
                detailsActivity.putExtras(bundle);
                startActivityForResult(detailsActivity, 0);
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
                if (userEmail.compareTo(mBeats.get(getAdapterPosition()).getUserId()) == 0){
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Set beat action");
                    // Discriminate the fact the user owns the content
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            // Delete media before, if any
                            boolean res = true;
                            if (mBeats.get(getAdapterPosition()).hasRemoteFileName() && mBeats.get(getAdapterPosition()).getRemoteFileName().length() > 0)
                                res = PoviUtils.deleteS3Object(getActivity(),mBeats.get(getAdapterPosition()).getRemoteFileName() );
                            if (!res)
                                Snackbar.make(mRecyclerView, "Error deleting beat media!", Snackbar.LENGTH_LONG).show();
                            // Delete beat
                            res = RestServer.deleteComment(userToken, mBeats.get(getAdapterPosition()).getCommentId());
                            if (!res)
                                Snackbar.make(mRecyclerView, "Error deleting beat!", Snackbar.LENGTH_LONG).show();
                            else{
                                // Update adapter
                                mBeats.remove(getAdapterPosition());
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
            mBeats = mediaContents;
        }

        @Override
        public int getItemViewType(int position) {
            // Just as an example, return 0 or 2 depending on position
            // Note that unlike in ListView adapters, types don't have to be contiguous
            return mBeats.get(position).getContentGroups();
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
            if (mBeats != null && mBeats.size() > position) {
                holder.comment.setText(mBeats.get(position).getCommentText());
                // Set circle fill color
                GradientDrawable circle = (GradientDrawable) holder.firstLetter.getBackground();
                // TODO: set nick name initial
                if (mBeats.get(position).hasAuthorNickName() && mBeats.get(position).getAuthorNickName().length() > 0){
                    holder.firstLetter.setText(String.valueOf(mBeats.get(position).getAuthorNickName().charAt(0)).toUpperCase());
                    holder.author.setText(mBeats.get(position).getAuthorNickName(GetMode.NULL));
                }
                else
                    holder.firstLetter.setText(String.valueOf(mBeats.get(position).getUserId().charAt(0)).toUpperCase());

                circle.setColor(PoviUtils.generateMaterialColor(position));
                //if (mMediaContents.get(position).hasLikeCount())
                holder.likesNr.setText(Integer.toString(mBeats.get(position).getLikeCount(GetMode.NULL)));
                //if (mMediaContents.get(position).hasCommentCount())
                holder.commentsNr.setText(Integer.toString(mBeats.get(position).getCommentCount(GetMode.NULL)));
                //if (mBeats.get(position).getThumbnailImage(GetMode.NULL) != null)
                //    holder.preview.setImageBitmap(BitmapFactory.decodeStream(mBeats.get(position).getThumbnailImage().asInputStream()));
                //if (mBeats.get(position).getContentGroups() != 1){
                //    holder.comment.setText(mBeats.get(position).getTextDescription());
                //}
                switch (mBeats.get(position).getContentGroups()) {
                    case 1:
                        holder.comment.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
                        holder.comment.setText(mBeats.get(position).getCommentText());
                        holder.comment.setBackgroundColor(PoviUtils.generateMaterialColor(position));
                        break;
                    case 2:
                        holder.comment.setTextColor(ContextCompat.getColor(getActivity(), R.color.black));
                        holder.comment.setText(mBeats.get(position).getTextDescription());
                        holder.preview.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.audio_header));
                        holder.preview.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        holder.comment.setTextColor(ContextCompat.getColor(getActivity(), R.color.black));
                        holder.comment.setText(mBeats.get(position).getTextDescription());
                        holder.preview.setImageBitmap(BitmapFactory.decodeStream(mBeats.get(position).getThumbnailImage().asInputStream()));
                        holder.preview.setVisibility(View.VISIBLE);
                        break;
                    case 4:
                        holder.comment.setTextColor(ContextCompat.getColor(getActivity(), R.color.black));
                        holder.comment.setText(mBeats.get(position).getTextDescription());
                        holder.preview.setImageBitmap(BitmapFactory.decodeStream(mBeats.get(position).getThumbnailImage().asInputStream()));
                        holder.preview.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }

        @Override
        public int getItemCount() {
            if (mBeats != null)
                return mBeats.size();
            else
                return 0;
        }
    } // end MyAdapter

    public static class BeatLoader extends AsyncTaskLoader<List<Comment>> {
        private List<Comment> mComments;
        private String mChildId;
        private Context mContext;
        private int start;
        private int count;
        private long lastcommentid;

        public BeatLoader(Context context, String childId, int start, int count, long lastcommentid) {
            super(context);
            mContext = context;
            mChildId = childId;
            this.start = start;
            this.count = count;
            this.lastcommentid = lastcommentid;
        }

        @Override
        public List<Comment> loadInBackground() {
            // Retrieve beats from server
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String currentToken = sharedPref.getString(POVI_TOKEN, "");
            List<Comment> data = null;
            if (mChildId != null)
                if (lastcommentid != -1)
                    data = RestServer.getCommentsPagedPerChild(mChildId, currentToken, start, count, lastcommentid);
                else
                    data = RestServer.getCommentsPagedPerChild(mChildId, currentToken, start, count, null);

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
                bundle.putString("childName", mChildren.get(tabs.getSelectedTabPosition()).getName());
                bundle.putInt("start", mBeats.get(tabs.getSelectedTabPosition()).size());
                bundle.putInt("count", 10);
                bundle.putLong("lastcommentid", mBeats.get(tabs.getSelectedTabPosition()).get(mBeats.get(tabs.getSelectedTabPosition()).size() - 1).getCommentId());
                getLoaderManager().restartLoader(tabs.getSelectedTabPosition() + 1, bundle, beatCallback);
            }
        }

    }

    public static class ChildrenLoader extends AsyncTaskLoader<List<Child>>{
        private List<Child> mChildren;
        private Context mContext;
        private String mUserId;
        private String mAccessToken;

        public ChildrenLoader(Context context, String userId, String accessToken) {
            super(context);
            mContext = context;
            mUserId = userId;
            mAccessToken = accessToken;
        }

        @Override
        public List<Child> loadInBackground() {
            // Retrieve children from server
            List<Child> data = RestServer.getChildren(mAccessToken);
            return data;
        }

        @Override
        public void deliverResult(List<Child> data) {
            if (isReset()) {
                // The Loader has been reset; ignore the result and invalidate the data.
                releaseResources(data);
                return;
            }

            // Hold a reference to the old data so it doesn't get garbage collected.
            // We must protect it until the new data has been delivered.
            List<Child> oldData = mChildren;
            mChildren = data;

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
            if (mChildren != null) {
                // Deliver any previously loaded data immediately.
                deliverResult(mChildren);
            }

            // Begin monitoring the underlying data source.
            // if (mObserver == null) {
            //    mObserver = new SampleObserver();
            // TODO: register the observer
            // }

            if (takeContentChanged() || mChildren == null) {
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
            if (mChildren != null) {
                releaseResources(mChildren);
                mChildren = null;
            }

            // The Loader is being reset, so we should stop monitoring for changes.
            //if (mObserver != null) {
            // TODO: unregister the observer
            //    mObserver = null;
            //}
        }

        @Override
        public void onCanceled(List<Child> data) {
            // Attempt to cancel the current asynchronous load.
            super.onCanceled(data);

            // The load has been canceled, so we should release the resources
            // associated with 'data'.
            releaseResources(data);
        }

        private void releaseResources(List<Child> data) {
            // For a simple List, there is nothing to do. For something like a Cursor, we
            // would close it in this method. All resources associated with the Loader
            // should be released here.
        }
    }




}
