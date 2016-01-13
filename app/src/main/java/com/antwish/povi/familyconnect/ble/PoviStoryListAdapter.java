package com.antwish.povi.familyconnect.ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.antwish.povi.familyconnect.R;

import java.util.ArrayList;

public class PoviStoryListAdapter extends BaseAdapter {
    private ArrayList<String> mStories;
    private LayoutInflater mInflater;

    public PoviStoryListAdapter(Activity par) {
        super();
        mStories = new ArrayList<String>();
        mInflater = par.getLayoutInflater();
    }

    public void addDevice(String story) {
        if(mStories.contains(story) == false) {
            mStories.add(story);
        }
    }

    public String getStory(int index) {
        return mStories.get(index);
    }

    public void clearList() {
        mStories.clear();
    }

    @Override
    public int getCount() {
        return mStories.size();
    }

    @Override
    public Object getItem(int position) {
        return getStory(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // get already available view or create new if necessary
        FieldReferences fields;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.activity_povi_story, null);
            fields = new FieldReferences();
            fields.storyName = (TextView)convertView.findViewById(R.id.textView);
            convertView.setTag(fields);
        } else {
            fields = (FieldReferences) convertView.getTag();
        }

        // set proper values into the view
        String story = mStories.get(position);

        fields.storyName.setText(story);

        return convertView;
    }

    private class FieldReferences {
        TextView storyName;
    }

}
