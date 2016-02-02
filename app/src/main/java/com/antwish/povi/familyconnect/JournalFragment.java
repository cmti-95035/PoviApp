package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class JournalFragment extends PreferenceFragment {
    private OnTitleChangeListener titleChangeListener;

    private Tracker tracker;
    private ListView mListView;

    private SimpleAdapter journalAdapter;

    public static JournalFragment newInstance() {
        JournalFragment fragment = new JournalFragment();
        return fragment;
    }

    public JournalFragment() {
        // Required empty public constructor
    }

    private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) getActivity().getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        tracker.setScreenName("Journal screen");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());


        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_journal, container, false);


        Context context = view.getContext();
        mListView = (ListView) view.findViewById(R.id.povijourna_listView);
        populateListView(populateStories(), context);
        return view;
    }

    public void populateListView(List<PoviJournal> journals, Context context) {
        String[] keys = new String[]{"title", "date"};
        int[] uiComponents = {R.id.journal_title, R.id.journal_date};

        List<HashMap<String, Object>> storyData = new ArrayList<HashMap<String, Object>>(journals.size());

        // build the data for the table view.
        for (PoviJournal journal : journals) {
            HashMap<String, Object> d = new HashMap<String, Object>();
            d.put("title", journal.getTitle());
            d.put("date", journal.getDate());
            storyData.add(d);
        }

        journalAdapter = new SimpleAdapter(context, storyData, R.layout.journal_card, keys, uiComponents);
        mListView.setAdapter(journalAdapter);

        mListView.setOnItemClickListener(listClickListener);
    }

    // TODO: in the future this has to be retrieved from server
    // it may cache in the app as well to avoid pulling this content too frequently
    private List<PoviJournal> populateStories() {
        List<PoviJournal> journals = new ArrayList<PoviJournal>();
        journals.add(new PoviJournal("Playground", "2015-12-31 14:57:28"));
        journals.add(new PoviJournal("Oreo", "2015-12-31 15:57:28"));
        journals.add(new PoviJournal("Math Class", "2015-12-31 18:57:28"));
        journals.add(new PoviJournal("Library", "2015-12-31 20:57:28"));
        journals.add(new PoviJournal("Dropping Ice Cream", "2015-12-31 21:57:28"));

        return journals;
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
            titleChangeListener.onTitleChangeListener("Journals");
    }


    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_settings, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
