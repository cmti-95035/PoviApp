package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


public class StoryFragment extends Fragment {
    private OnTitleChangeListener titleChangeListener;

    private Tracker tracker;
    private View view;
    private PoviStory story;

    public static StoryFragment newInstance() {
        StoryFragment fragment = new StoryFragment();
        return fragment;
    }

    public StoryFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) getActivity().getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        tracker.setScreenName("Conversation Starter screen");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_story, container, false);

        populateStory(RestServer.getStory());
        return view;
    }

    private void populateStory(PoviStory poviStory)
    {
        story = poviStory;

        populateTitleCard();

        ImageView imageView = (ImageView) view.findViewById(R.id.authorimage);
        imageView.setImageResource(populateImageResource(story));

        String[] questions = story.getFollowupQuestions();

        TextView q1 = (TextView)view.findViewById(R.id.followup1Question);
        q1.setText(questions[0]);

        TextView q2 = (TextView)view.findViewById(R.id.followup2Question);
        q2.setText(questions[1]);

        TextView q3 = (TextView)view.findViewById(R.id.followup3Question);
        q3.setText(questions[2]);

        Button readMore = (Button) view.findViewById(R.id.readmore);
        readMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expand(story);
            }
        });
    }

    public static int populateImageResource(PoviStory poviStory){
        switch (poviStory.getAuthor()){
            case "Daphna Ram":
                return R.drawable.daphna_headshot;
                
            case "Mallika Sankaran":
                return R.drawable.mallika_headshot;
                
            case "Anna Muggiati":
                return R.drawable.anna_headshot;
                
            case "Olya Glantsman":
                return R.drawable.olya_headshot;
                
            case "Yonit Parenti":
                return R.drawable.yonit_headshot;
                
            default:
                return R.drawable.daphna_headshot;
        }
    }
    private void populateTitleCard()
    {
        TextView category = (TextView)view.findViewById(R.id.category_title);
        category.setText(story.getCategory());

        TextView author = (TextView)view.findViewById(R.id.byauthor);
        author.setText(story.getAuthor());

        TextView title = (TextView)view.findViewById(R.id.storytitle);
        title.setText(story.getTitle());

        TextView excerpt = (TextView)view.findViewById(R.id.storyexcerpt);
        excerpt.setText(story.getFullStory().substring(0, 40) + "...");
    }

    private void expand(PoviStory currentStory){
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction trans = fragmentManager.beginTransaction();
        trans.replace(R.id.nav_contentframe, new FullStoryFragment());
        trans.addToBackStack(null);
        trans.commit();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            titleChangeListener = (OnTitleChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTitleChangeListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        titleChangeListener = null;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (titleChangeListener != null)
            titleChangeListener.onTitleChangeListener("引发对话的场景故事");
//            titleChangeListener.onTitleChangeListener("Conversation Starter");
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_story, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.refresh_story:
                populateStory(RestServer.getNextStory());
                return true;
        }
        return false;
    }
}
