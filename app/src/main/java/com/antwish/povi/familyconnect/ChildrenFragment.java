package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.antwish.povi.server.Child;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class ChildrenFragment extends Fragment {
    private static final String POVI_TOKEN = "povi_token";
    private static final String TAG = "Children" ;
    private ProgressDialog refreshDialog;
    private View view;
    private  TabLayout tab;
    private ProgressDialog deleteDialog;
    private List<Child> children;
    private List<Fragment> childrenFragments;
    private String childName;

    private OnTitleChangeListener titleChangeListener;
    private OnChildrenToolbarElevationListener toolbarElevationListener;

    private MyGestures myGestureDetector;
    private GestureDetector mGestureDetector;
    private int selectedTabIdx = 0;

    public interface OnChildrenToolbarElevationListener{
        void onToolbarElevationListener(float elevation);
    }

    public interface OnChildUpdateListener {
        void onChildUpdateListener();
    }

    public ChildrenFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        childrenFragments = new ArrayList<>();

        refreshDialog = new ProgressDialog(getActivity());
        refreshDialog.setIndeterminate(true);
        refreshDialog.setMessage("Please wait");
        refreshDialog.setTitle("Retrieving children...");
        refreshDialog.setCancelable(false);
        deleteDialog = new ProgressDialog(getActivity());
        deleteDialog.setIndeterminate(true);
        deleteDialog.setMessage("Please wait");
        deleteDialog.setTitle("Removing child profile...");
        deleteDialog.setCancelable(false);

        setHasOptionsMenu(true);

        myGestureDetector = new MyGestures();
        mGestureDetector = new GestureDetector(getActivity(), myGestureDetector);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_children, container, false);

        tab = (TabLayout) view.findViewById(R.id.tab);
        tab.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                childName = tab.getText().toString();
                // Change fragment
                FragmentManager fragmentManager = getFragmentManager();
                Fragment fragment = childrenFragments.get(tab.getPosition());
                FragmentTransaction trans = fragmentManager.beginTransaction();
                trans.replace(R.id.container, fragment);
                //trans.addToBackStack(null);
                trans.commit();
                selectedTabIdx = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // Set tab layout position and colors
        tab.setTabTextColors(getResources().getColor(R.color.white), getResources().getColor(R.color.povi_accent));

        new RefreshTask().execute();

        // Inflate the layout for this fragment
        return view;
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
        try {
            toolbarElevationListener = (OnChildrenToolbarElevationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnChildrenToolbarElevationListener");
        }
    }

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
            titleChangeListener.onTitleChangeListener("My children");
        // Disable toolbar elevation
        toolbarElevationListener.onToolbarElevationListener(0);
        //new RefreshTask().execute();
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        // Re-enable toolbar elevation
        final float scale = getResources().getDisplayMetrics().density;
        int elevation = (int) (4 * scale + 0.5f);
        toolbarElevationListener.onToolbarElevationListener(elevation);
    }

    private  class RefreshTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            refreshDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            childName = null;
            childrenFragments.clear();
            tab.removeAllTabs();

            if (refreshDialog != null)
                refreshDialog.dismiss();

            FrameLayout fl = (FrameLayout) view.findViewById(R.id.container);
            fl.removeAllViews();
            fl.setOnTouchListener(null);

            if (result){
                for (Child child:children)
                childrenFragments.add(ChildProfileFragment.newInstance(child.getName(), child.getGender(), child.getBirthdate()));

                for (Child child:children)
                tab.addTab(tab.newTab().setText(child.getName()));

                selectedTabIdx = 0;

           /*     fl.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        mGestureDetector.onTouchEvent(event);
                        return true;
                    }
                });*/

               // Snackbar.make(view, "Children retieval succeded!", Snackbar.LENGTH_SHORT).show();
            }
            else{
                Snackbar.make(view, "Children retrieval failed!", Snackbar.LENGTH_SHORT).show();
            }

            // Set child place holder if required
            if (!result || children.size() == 0) {
                LayoutInflater.from(getActivity()).inflate(R.layout.child_placeholder, fl, true);
            }

        }

        @Override
        protected Boolean doInBackground(String... params) {
            // Get children list
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String currentToken = sharedPref.getString(POVI_TOKEN, "");
            children = RestServer.getChildren(currentToken);

            if (children != null) {
                return true;
            }
            else
                return false;
        }
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_children, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.add_profile:
                // send add child notification to activity
                //addChildListener.onAddChildListener();
                Intent nextActivity = new Intent(getActivity(), AddChildActivity.class);
                startActivityForResult(nextActivity, 0);
                return true;

            case R.id.delete_profile:
                // Get data
                if (childName != null) {
                    // Ask user for permission
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Delete " + childName + "'s profile?").setMessage("This will also delete the child's journal entries")
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    new DeleteChildTask().execute(childName);
                                }
                            })
                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                }
                            });
                    // Create the AlertDialog object and return it
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                return true;
            case R.id.refresh_children:
                new RefreshTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class DeleteChildTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            deleteDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (deleteDialog != null)
                deleteDialog.cancel();

            if (result) {
                //Snackbar.make(view, "Child removal succeded!", Snackbar.LENGTH_SHORT).show();
                // Signal removal to viewpager
                new RefreshTask().execute();
            } else
                Snackbar.make(view, "Child removal failed!", Snackbar.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String currentToken = sharedPref.getString(POVI_TOKEN, "");
            String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, "");
            String fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "POVI/" + userEmail + "_" + childName + ".jpg";
            String fileName2 = userEmail + "_" + childName + ".jpg";
            boolean res = RestServer.deleteChild(currentToken, params[0]);
            // Delete picture
            final File imgFile = new File(fileName);
            if (imgFile.exists()) {
                res = imgFile.delete();
                PoviUtils.deleteS3Object(getActivity(), fileName2);
            }
            return res;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Refresh children list
        new RefreshTask().execute();
    }

    private class MyGestures implements GestureDetector.OnGestureListener{

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() < e2.getX()) {
                //Log.d(TAG, "Left to Right swipe performed");
                tab.getTabAt((selectedTabIdx - 1 + tab.getTabCount()) % tab.getTabCount()).select();
            }

            if (e1.getX() > e2.getX()) {
                //Log.d(TAG, "Right to Left swipe performed");
                tab.getTabAt((selectedTabIdx + 1 + tab.getTabCount()) % tab.getTabCount()).select();
            }
            return true;
        }
    }

    public void refreshChildren(){
        new RefreshTask().execute();
    }
}
