package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import com.antwish.povi.server.User;
import com.linkedin.data.template.GetMode;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;


public class ProfileFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";
    private static final String POVI_USERNAME = "povi_username";

    private EditText name;
    private EditText email;
    private EditText password;
    private EditText retypepassword;
    private Spinner genderSelector;
    private String gender;
    private DatePickerDialog datePicker;
    private long birthdateTimestamp = 0;
    private EditText birthdateText;
    private EditText nicknameText;

    private ProgressDialog refreshDialog;
    private ProgressDialog updateDialog;

    private User profile;
    private View view;

    private String oldEmail;

    private ActionMode.Callback mActionModeCallback;

    private ActionMode mActionMode;

    private OnTitleChangeListener titleChangeListener;
    private OnProfileUpdateListener profileUpdateListener;
    public interface OnProfileUpdateListener{
        void onProfileUpdateListener();
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            datePicker.show();
        }
    };

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        // Enable menu
        setHasOptionsMenu(true);

        refreshDialog = new ProgressDialog(getActivity());
        refreshDialog.setIndeterminate(true);
        refreshDialog.setMessage("Please wait");
        refreshDialog.setTitle("Retrieving user's data...");
        refreshDialog.setCancelable(false);
        updateDialog = new ProgressDialog(getActivity());
        updateDialog.setIndeterminate(true);
        updateDialog.setMessage("Please wait");
        updateDialog.setTitle("Updating user's profile ...");
        updateDialog.setCancelable(false);

        Calendar cal = Calendar.getInstance();

        datePicker = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                int yr = view.getYear();
                int mo = view.getMonth();
                int day = view.getDayOfMonth();

                cal.set(yr, mo, day);
                birthdateTimestamp = cal.getTime().getTime();
                birthdateText.setText(Integer.toString(yr) + "-" + Integer.toString(mo+1) + "-" + Integer.toString(day));
            }
        }, cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH));
        // Set limit to calendar
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Add genders
        String[] genders = {"male", "female", "unspecified"};
        genderSelector = (Spinner) view.findViewById(R.id.gender);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, genders);
        genderSelector.setAdapter(spinnerAdapter);

        // Get controls
        name = (EditText) view.findViewById(R.id.name);
        email = (EditText) view.findViewById(R.id.email);
        password = (EditText) view.findViewById(R.id.password);
        retypepassword = (EditText) view.findViewById(R.id.retypePassword);
        birthdateText = (EditText) view.findViewById(R.id.birthday);
        birthdateText.setFocusable(false);
        nicknameText = (EditText) view.findViewById(R.id.nickName);
        //birthdateText.setOnClickListener(listener);

        // Hide retype password and disable all controls by default
        name.setFocusable(false);
        name.setFocusableInTouchMode(false);
        nicknameText.setFocusable(false);
        nicknameText.setFocusableInTouchMode(false);
        email.setFocusable(false);
        email.setFocusableInTouchMode(false);
        password.setFocusable(false);
        password.setFocusableInTouchMode(false);
        retypepassword.setFocusable(false);
        retypepassword.setFocusableInTouchMode(false);
        retypepassword.setVisibility(View.GONE);
        genderSelector.setFocusableInTouchMode(false);
        genderSelector.setEnabled(false);
        birthdateText.setFocusable(false);
        birthdateText.setFocusableInTouchMode(false);

        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mActionMode = mode;
                menu.clear();
                // Inflate the menu for the CAB
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_profile_actions, menu);
                mode.setTitle("Edit user's profile");

                // Re-enable edit fields
                name.setFocusable(true);
                name.setFocusableInTouchMode(true);
                nicknameText.setFocusable(true);
                nicknameText.setFocusableInTouchMode(true);
                email.setFocusable(true);
                email.setFocusableInTouchMode(true);
                password.setFocusable(true);
                password.setFocusableInTouchMode(true);
                retypepassword.setFocusable(true);
                retypepassword.setFocusableInTouchMode(true);
                retypepassword.setVisibility(View.VISIBLE);
                genderSelector.setFocusable(true);
                genderSelector.setFocusableInTouchMode(true);
                genderSelector.setEnabled(true);
                //birthdateText.setFocusable(true);
                //birthdateText.setFocusableInTouchMode(true);
                birthdateText.setOnClickListener(listener);
                //name.requestFocus();
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.save:
                        // Accept edit and update profile on server
                        if (retypepassword.getText().toString().compareTo(password.getText().toString()) != 0) {
                            Snackbar.make(view, "Passwords not matching!", Snackbar.LENGTH_SHORT).show();
                            return true;
                        }
                        else{
                            // Send data to server
                            new UpdateTask().execute();
                        }
                        break;
                    case R.id.cancel:
                        mode.finish();
                        break;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
                // Restore previous data
                if (profile != null){
                    name.setText(profile.getName());
                    nicknameText.setText(profile.getNickName(GetMode.NULL));
                    email.setText(profile.getEmail());
                    oldEmail = profile.getEmail();
                    //password.setText(profile.getHash());
                    //retypepassword.setText(profile.getHash());
                    gender = profile.getGender();
                    if(gender!= null) {
                        if (gender.compareTo("male") == 0)
                            genderSelector.setSelection(0);
                        if (gender.compareTo("female") == 0)
                            genderSelector.setSelection(1);
                        if (gender.compareTo("unspecified") == 0)
                            genderSelector.setSelection(2);
                    }
                    else {
                        genderSelector.setSelection(2);
                    }
                    birthdateTimestamp = profile.getBirthdate();
                    if (birthdateTimestamp > 0){
                        // Set birthdate
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(new Date(birthdateTimestamp));
                        birthdateText.setText(Integer.toString(cal.get(Calendar.YEAR)) + "-" +
                                Integer.toString(cal.get(Calendar.MONTH) + 1) + "-" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
                    }

                }

                // Hide retype password and disable all controls by default
                name.setFocusable(false);
                name.setFocusableInTouchMode(false);
                nicknameText.setFocusable(false);
                nicknameText.setFocusableInTouchMode(false);
                email.setFocusable(false);
                email.setFocusableInTouchMode(false);
                password.setFocusable(false);
                password.setFocusableInTouchMode(false);
                retypepassword.setFocusable(false);
                retypepassword.setFocusableInTouchMode(false);
                retypepassword.setVisibility(View.GONE);
                genderSelector.setFocusable(false);
                genderSelector.setFocusableInTouchMode(false);
                genderSelector.setEnabled(false);
                birthdateText.setFocusable(false);
                birthdateText.setFocusableInTouchMode(false);
                birthdateText.setOnClickListener(null);
            }
        };

        new RefreshTask().execute();

        return view;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        // Exit action mode
        if (mActionMode != null)
            mActionMode.finish();
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
            profileUpdateListener = (OnProfileUpdateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnProfileUpdateListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        titleChangeListener = null;
        profileUpdateListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    /*
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }*/

    @Override
    public void onResume(){
        super.onResume();
        if (titleChangeListener != null)
            titleChangeListener.onTitleChangeListener("My profile");
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_profile, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.refresh_profile:
                // send add child notification to activity
                new RefreshTask().execute();
                return true;
            case R.id.edit_profile:
                // Start action mode
                getActivity().startActionMode(mActionModeCallback);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

            if (refreshDialog != null)
                refreshDialog.dismiss();

            // Directly go to the dashboard in case of success
            if (profile != null){
                name.setText(profile.getName());
                nicknameText.setText(profile.getNickName(GetMode.NULL));
                email.setText(profile.getEmail());
                oldEmail = profile.getEmail();
                //password.setText(profile.getHash());
                //retypepassword.setText(profile.getHash());
                gender = profile.getGender();
                if(gender!= null) {
                    if (gender.compareTo("male") == 0)
                        genderSelector.setSelection(0);
                    if (gender.compareTo("female") == 0)
                        genderSelector.setSelection(1);
                    if (gender.compareTo("unspecified") == 0)
                        genderSelector.setSelection(2);
                }
                else {
                    genderSelector.setSelection(2);
                }
                birthdateTimestamp = profile.getBirthdate();
                if (birthdateTimestamp > 0){
                    // Set birthdate
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new Date(birthdateTimestamp));
                    birthdateText.setText(Integer.toString(cal.get(Calendar.YEAR)) + "-" +
                            Integer.toString(cal.get(Calendar.MONTH) + 1) + "-" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
                }

            }

            if (result){
                //Snackbar.make(view, "User's profile retrieval succeded!", Snackbar.LENGTH_SHORT).show();
            }
            else{
                Snackbar.make(view, "User's profile retrieval failed!", Snackbar.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            // Get user profile
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String currentToken = sharedPref.getString("povi_token", "");
            profile = RestServer.getUserProfile(currentToken);
            if (profile != null)
                return true;
            else
                return false;
        }
    }

    private  class UpdateTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Get data
            updateDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (updateDialog != null)
                updateDialog.dismiss();

            if (result){
                mActionMode.finish();
                Snackbar.make(view, "User's profile update succeded!", Snackbar.LENGTH_SHORT).show();
                new RefreshTask().execute();
            }
            else{
                Snackbar.make(view, "User's profile update failed!", Snackbar.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String newName = name.getText().toString();
            String newPassword = password.getText().toString();
            String newEmail = email.getText().toString();
            String newGender = genderSelector.getSelectedItem().toString();
            String newNickname = nicknameText.getText().toString();
            String hash = profile.getHash();

            // Generate hash if password is not null
            if (newPassword != null && newPassword.length() > 0) {
                String data = newEmail + newPassword;
                MessageDigest digest = null;
                try {
                    digest = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                digest.reset();
                try {
                    digest.update(data.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                hash = new BigInteger(1, digest.digest()).toString(16);
            }

            // Get access token
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String token = sharedPref.getString(POVI_TOKEN, null);

            // Send data to rest server and wait for the response
            boolean res = RestServer.updateProfile(token, oldEmail, newEmail, hash, newName, newNickname, "555", "", birthdateTimestamp, newGender);

            if(res){
                // successfully updated the user profile. it needs to update the email and name in shared preference as well
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(POVI_USERID, newEmail);
                editor.putString(POVI_USERNAME, newName);
                editor.commit();
                // Ask dashboard to update navigation view
                profileUpdateListener.onProfileUpdateListener();
            }

            return res;
        }
    }

    }