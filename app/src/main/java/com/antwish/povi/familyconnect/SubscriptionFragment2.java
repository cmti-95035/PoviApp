package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that shows the tips on the screen and on selection
 * of a tip allows us to create an entry into the journal for recording
 * comments.
 *
 * <p>This class is called by the {@link DashboardActivity}</p>
 */

public class SubscriptionFragment2 extends Fragment {
    private static final String TAG = "SubscriptionFragment";
    private OnTitleChangeListener titleChangeListener;

    private View rootView;

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private StaggeredGridLayoutManager mLayoutManager;

    private TabLayout tabs;

    private LoaderManager.LoaderCallbacks<List<PoviSubscription>> subscriptionCallback;

    private List<List<PoviSubscription>> mSubscriptionList = new ArrayList<>();
    private List<PoviSubscriptiontype> mSubscriptionTypes = new ArrayList<>();

    private ProgressBar mProgressBar;
    private TextView title, numberOfStories, description;
    private ImageView subscriptionImage;
    private Button priceButton;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given argument.
     */
    public static SubscriptionFragment2 newInstance() {
        SubscriptionFragment2 fragment = new SubscriptionFragment2();
        return fragment;
    }

    public SubscriptionFragment2() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        mSubscriptionTypes.add(PoviSubscriptiontype.CATEGORY);
        mSubscriptionTypes.add(PoviSubscriptiontype.AUTHOR);
        mSubscriptionTypes.add(PoviSubscriptiontype.LIBRARY);

        mSubscriptionList = new ArrayList<>();
        mSubscriptionList.add(RestServer.getSubscriptionByType(null, 0, 0, 0L, PoviSubscriptiontype.CATEGORY));
        mSubscriptionList.add(RestServer.getSubscriptionByType(null, 0, 0, 0L, PoviSubscriptiontype.AUTHOR));
        mSubscriptionList.add(RestServer.getSubscriptionByType(null, 0, 0, 0L, PoviSubscriptiontype.LIBRARY));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_subscription, container, false);

        tabs = (TabLayout) rootView.findViewById(R.id.tab);
        tabs.addTab(tabs.newTab().setText("CATEGORIES"));
        tabs.addTab(tabs.newTab().setText("AUTHORS"));
        tabs.addTab(tabs.newTab().setText("LIBRARY"));
        tabs.getTabAt(0).select();

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

                title.setText(mSubscriptionList.get(tabs.getSelectedTabPosition()).get(0).getTitle());
                numberOfStories.setText("Number Of Stories: " + mSubscriptionList.get(tabs.getSelectedTabPosition()).get(0).getNumberOfStories());
                description.setText(mSubscriptionList.get(tabs.getSelectedTabPosition()).get(0).getDescription());
                priceButton.setText("$" + mSubscriptionList.get(tabs.getSelectedTabPosition()).get(0).getPrice());
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

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        mAdapter.changeContents(mSubscriptionList.get(0));
        // Populate recycler view
        mAdapter.notifyDataSetChanged();

        title = (TextView) rootView.findViewById(R.id.subscriptionTitle);
        title.setText(mSubscriptionList.get(0).get(0).getTitle());
        numberOfStories = (TextView) rootView.findViewById(R.id.numberOfSubs);
        numberOfStories.setText("Number Of Stories: " + mSubscriptionList.get(0).get(0).getNumberOfStories());
        description = (TextView) rootView.findViewById(R.id.storytitle);
        description.setText(mSubscriptionList.get(0).get(0).getDescription());

        subscriptionImage = (ImageView) rootView.findViewById(R.id.subscriptionImage);
        priceButton = (Button) rootView.findViewById(R.id.price);

        priceButton.setText("$" + mSubscriptionList.get(0).get(0).getPrice());
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
            titleChangeListener.onTitleChangeListener("Subscriptions");
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTitleChangeListener");
        }
    }

    /**
     * Called immediately prior to fragment being no longer associated with its activity
     */
    @Override
    public void onDetach() {
        super.onDetach();
        titleChangeListener = null;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (titleChangeListener != null)
            titleChangeListener.onTitleChangeListener("Subscriptions");
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        // Re-enable toolbar elevation
        final float scale = getResources().getDisplayMetrics().density;
        int elevation = (int) (4 * scale + 0.5f);

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
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<PoviSubscription> poviSubscriptions;

        public void changeContents(List<PoviSubscription> newContents){
            poviSubscriptions = newContents;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
            private TextView category;
            private TextView numberOfStories;
            private ImageView preview;

            public ViewHolder(View itemView) {
                super(itemView);
                category = (TextView) itemView.findViewById(R.id.category);
                numberOfStories = (TextView) itemView.findViewById(R.id.numberOfStories);
                preview = (ImageView) itemView.findViewById(R.id.subscriptionPreview);
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View v) {
                // Open content details activity
                title.setText(mSubscriptionList.get(tabs.getSelectedTabPosition()).get(getAdapterPosition()).getTitle());
                numberOfStories.setText("Number Of Stories: " + mSubscriptionList.get(tabs.getSelectedTabPosition()).get(getAdapterPosition()).getNumberOfStories());
                description.setText(mSubscriptionList.get(tabs.getSelectedTabPosition()).get(getAdapterPosition()).getDescription());
                priceButton.setText("$" + mSubscriptionList.get(tabs.getSelectedTabPosition()).get(getAdapterPosition()).getPrice());
            }

            @Override
            public boolean onLongClick(View v) {
                return true;
            }

        }

        public MyAdapter(List<PoviSubscription> mediaContents){
            poviSubscriptions = mediaContents;
        }

        @Override
        public int getItemViewType(int position) {
            // Just as an example, return 0 or 2 depending on position
            // Note that unlike in ListView adapters, types don't have to be contiguous
            return poviSubscriptions.get(position).getSubscriptiontype().ordinal();
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscription_card, parent, false);
            ViewHolder vh = new ViewHolder(view);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (poviSubscriptions != null && poviSubscriptions.size() > position) {
                holder.category.setText(poviSubscriptions.get(position).getTitle());
                holder.numberOfStories.setText("Number Of Stories: " + poviSubscriptions.get(position).getNumberOfStories());
            }
        }

        @Override
        public int getItemCount() {
            if (poviSubscriptions != null)
                return poviSubscriptions.size();
            else
                return 0;
        }
    } // end MyAdapter

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
        }

    }
}
