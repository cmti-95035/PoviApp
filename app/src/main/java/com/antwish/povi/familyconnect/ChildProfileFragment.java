package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
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
import android.widget.ImageView;
import android.widget.Spinner;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChildProfileFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    private static final String TAG = "Add child";
    private EditText name;
    private Spinner gender;
    private ProgressDialog updateDialog;
    private ProgressDialog uploadPicDialog;
    private ProgressDialog downloadPicDialog;
    private String childName;
    private String childGender;
    private DatePickerDialog datePicker;
    private long birthdateTimestamp = 0;
    private long originalBirthdateTimestamp = 0;
    private EditText birthdateText;
    private String oldName;

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int PIC_CROP = 2;
    private static final int PICK_IMAGE_REQUEST = 3;

    private Uri picUri;
    private Uri croppedUri;
    private ImageView childImage;
    private View view;
    private FloatingActionButton fab;

    private OnTitleChangeListener titleChangeListener;
    private ChildrenFragment.OnChildUpdateListener childUpdateListener;
    private ActionMode.Callback mActionModeCallback;
    private ActionMode mActionMode;

    private boolean photoChanged = false;

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            datePicker.show();
        }
    };


    public static ChildProfileFragment newInstance(String name, String gender, long birthdate) {
        Bundle bundle = new Bundle();
        bundle.putString("child_name", name);
        bundle.putString("child_gender", gender);
        bundle.putLong("child_birthdate", birthdate);
        ChildProfileFragment fragment = new ChildProfileFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public ChildProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null){
            childName = args.getString("child_name");
            childGender = args.getString("child_gender");
            originalBirthdateTimestamp = args.getLong("child_birthdate");
        }

        updateDialog = new ProgressDialog(getActivity());
        updateDialog.setIndeterminate(true);
        updateDialog.setMessage("Please wait");
        updateDialog.setTitle("Updating child profile...");
        updateDialog.setCancelable(false);
        uploadPicDialog = new ProgressDialog(getActivity());
        uploadPicDialog.setIndeterminate(true);
        uploadPicDialog.setMessage("Please wait");
        uploadPicDialog.setTitle("Uploading child picture...");
        uploadPicDialog.setCancelable(false);
        downloadPicDialog = new ProgressDialog(getActivity());
        downloadPicDialog.setIndeterminate(true);
        downloadPicDialog.setMessage("Please wait");
        downloadPicDialog.setTitle("Retrieving child picture...");
        downloadPicDialog.setCancelable(false);

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

        // Enable menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_child_profile, container, false);
        // Add genders
        String[] genders = {"male", "female", "unspecified"};
        gender = (Spinner) view.findViewById(R.id.gender);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, genders);
        gender.setAdapter(spinnerAdapter);

        // Get controls
        name = (EditText) view.findViewById(R.id.name);
        name.setText(childName);
        oldName = childName;
        if (childGender.compareTo("male") == 0)
            gender.setSelection(0);
        if (childGender.compareTo("female") == 0)
            gender.setSelection(1);
        if (childGender.compareTo("unspecified") == 0)
            gender.setSelection(2);
        birthdateText = (EditText) view.findViewById(R.id.birthday);
        birthdateText.setFocusable(false);
        //birthdateText.setOnClickListener(listener);
        if (originalBirthdateTimestamp > 0){
            // Set birthdate
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(originalBirthdateTimestamp));
            birthdateText.setText(Integer.toString(cal.get(Calendar.YEAR)) + "-" +
                    Integer.toString(cal.get(Calendar.MONTH) + 1) + "-" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
        }
        else {
            birthdateText.setText(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date (originalBirthdateTimestamp)));
        }

        // Set child image 3:2
        // Try to load existing image
        childImage = (ImageView) view.findViewById(R.id.childProfileImageView);
        childImage.post(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, "");
                File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES) + File.separator + "POVI");
                String imageFileName = storageDir + File.separator + userEmail + "_" + childName + ".jpg";
                loadBitmap(imageFileName, childImage);
            }
        });


        fab = (FloatingActionButton) view.findViewById(R.id.pictureButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //dispatchTakePictureIntent();
                // Open contextual menu
                final CharSequence[] items = {"Take a picture", "Load a picture", "Retrieve picture", "Delete picture"};

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Change child picture");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                        String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, "");
                        String fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "POVI/" + userEmail + "_" + name.getEditableText() + ".jpg";
                switch (item){
                    case 0:
                        fileName = userEmail + "_" + name.getEditableText() + ".jpg";
                        Intent takeIntent = PoviUtils.dispatchTakePictureIntent(fileName, getActivity());
                        if (takeIntent != null) {
                            picUri = (Uri) takeIntent.getExtras().get(MediaStore.EXTRA_OUTPUT);
                            startActivityForResult(takeIntent, REQUEST_TAKE_PHOTO);
                        }
                        else
                            Snackbar.make(name, "Error taking picture!", Snackbar.LENGTH_LONG).show();
                        break;
                    case 1:
                        Intent loadIntent = PoviUtils.dispatchLoadPictureIntent();
                        startActivityForResult(loadIntent,PICK_IMAGE_REQUEST);
                        break;
                    case 2:

                            // Try to retrieve file from server, if existing
                            String fileName2 = userEmail + "_" + childName + ".jpg";
                            boolean res = PoviUtils.existsS3Object(getActivity(), fileName2);
                            if (res){
                                final ProgressDialog downloadPicDialog = new ProgressDialog(getActivity());
                                downloadPicDialog.setIndeterminate(true);
                                downloadPicDialog.setMessage("Please wait");
                                downloadPicDialog.setTitle("Retrieving child picture...");
                                downloadPicDialog.setCancelable(false);
                                downloadPicDialog.show();
                                final File picFile = new File(fileName);
                                PoviUtils.downloadFile(getActivity().getApplicationContext(), fileName2, picFile, new TransferListener() {
                                    @Override
                                    public void onStateChanged(int i, TransferState transferState) {
                                        if (transferState == TransferState.COMPLETED) {
                                            // Manage success
                                            if (downloadPicDialog != null)
                                                downloadPicDialog.cancel();
                                            croppedUri = Uri.fromFile(picFile);
                                           setPic();
                                        } else if (transferState == TransferState.FAILED) {
                                            // Manage error
                                            if (downloadPicDialog != null)
                                                downloadPicDialog.cancel();
                                            Snackbar.make(name, "Pic retieval failed!", Snackbar.LENGTH_LONG).show();
                                        }
                                    }

                                    @Override
                                    public void onProgressChanged(int i, long l, long l1) {

                                    }

                                    @Override
                                    public void onError(int i, Exception e) {
                                        if (downloadPicDialog != null)
                                            downloadPicDialog.cancel();
                                        Snackbar.make(name, "Pic retrieval failed!", Snackbar.LENGTH_LONG).show();

                                    }
                                });
                            }
                            else
                                Snackbar.make(name, "No picture available!", Snackbar.LENGTH_LONG).show();

                        break;
                    case 3:
                        // Delete picture
                        final File imgFile = new File(fileName);
                        fileName = userEmail + "_" + name.getEditableText() + ".jpg";
                        res = true;
                        if (imgFile.exists()) {
                            res = imgFile.delete();
                            childImage.setImageResource(R.drawable.person);
                            PoviUtils.deleteS3Object(getActivity(), fileName);
                        }
                        if (!res)
                            Snackbar.make(name, "Pic deletion failed!", Snackbar.LENGTH_LONG).show();
                        break;
                }
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        // Hide button
        //fab.setVisibility(View.INVISIBLE);

        // Hide retype password and disable all controls by default
        name.setFocusable(false);
        name.setFocusableInTouchMode(false);
        gender.setFocusable(false);
        gender.setFocusableInTouchMode(false);
        gender.setEnabled(false);
        birthdateText.setFocusable(false);
        birthdateText.setFocusableInTouchMode(false);

        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mActionMode = mode;
                menu.clear();
                // Inflate the menu for the CAB
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_childprofile_actions, menu);
                mode.setTitle("Edit child's profile");

                // Re-enable edit fields
                fab.setVisibility(View.VISIBLE);
                name.setFocusable(true);
                name.setFocusableInTouchMode(true);
                gender.setFocusable(true);
                gender.setFocusableInTouchMode(true);
                gender.setEnabled(true);
                //birthdateText.setFocusable(true);
                //birthdateText.setFocusableInTouchMode(true);
                birthdateText.setOnClickListener(listener);
                name.requestFocus();
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
                        // Send data to server
                        childName = name.getText().toString();
                        if (!isValidName(childName)) {
                            name.setError("Invalid name!");
                            name.requestFocus();
                        }
                        else {
                            name.setError(null);
                            new UpdateChildTask().execute();
                        }
                        break;

                    case R.id.cancel:
                        mode.finish();
                }
                //mode.finish();
                // Hide retype password and disable all controls by default
               /* name.setFocusable(false);
                name.setFocusableInTouchMode(false);
                gender.setFocusable(false);
                gender.setFocusableInTouchMode(false);
                gender.setEnabled(false);
                birthdateText.setFocusable(false);
                birthdateText.setFocusableInTouchMode(false);
                birthdateText.setOnClickListener(null);
                fab.setVisibility(View.GONE);*/
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
                // Restore original data
                name.setError(null);
                name.setText(oldName);
                if (childGender.compareTo("male") == 0)
                    gender.setSelection(0);
                if (childGender.compareTo("female") == 0)
                    gender.setSelection(1);
                if (childGender.compareTo("unspecified") == 0)
                    gender.setSelection(2);
                if (originalBirthdateTimestamp > 0){
                    // Set birthdate
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new Date(originalBirthdateTimestamp));
                    birthdateText.setText(Integer.toString(cal.get(Calendar.YEAR)) + "-" +
                            Integer.toString(cal.get(Calendar.MONTH) + 1) + "-" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
                }
                mode.finish();
                name.setFocusable(false);
                name.setFocusableInTouchMode(false);
                gender.setFocusable(false);
                gender.setFocusableInTouchMode(false);
                gender.setEnabled(false);
                birthdateText.setFocusable(false);
                birthdateText.setFocusableInTouchMode(false);
                birthdateText.setOnClickListener(null);
                fab.setVisibility(View.GONE);
            }
        };

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
            childUpdateListener = (ChildrenFragment.OnChildUpdateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnChildUpdateListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        titleChangeListener = null;
        childUpdateListener = null;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (titleChangeListener != null)
            titleChangeListener.onTitleChangeListener("My children");
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        // Exit action mode
       if (mActionMode != null)
           mActionMode.finish();
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_childprofile, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.edit_child:
                // Start action mode
                getActivity().startActionMode(mActionModeCallback);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class UpdateChildTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Get data
            childName = name.getText().toString();
            childGender = gender.getSelectedItem().toString();
            updateDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (updateDialog != null)
                updateDialog.cancel();

            if (result) {
                    mActionMode.finish();
                    Snackbar.make(view, "Child update succeded!", Snackbar.LENGTH_SHORT).show();
                    childUpdateListener.onChildUpdateListener();
            } else
                Snackbar.make(view, "Child update failed!", Snackbar.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            // Get current access token
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, "");
            String token = sharedPref.getString(PoviConstants.POVI_TOKEN, "");

            // Edit child
            if(!token.isEmpty() && !userEmail.isEmpty() ) {
                boolean res = RestServer.updateChild(userEmail, oldName, childName, childGender, birthdateTimestamp, token);
                if (res) {
                    // Rename the current child picture, if existing
                    String oldImageFileName = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES) + File.separator + "POVI" + File.separator + "Povi_child_" + oldName + ".jpg";
                    File imageFile = new File(oldImageFileName);
                    if (imageFile.exists()) {
                        String imageFileName = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES) + File.separator + "POVI" + File.separator + "Povi_child_" + childName + ".jpg";
                        File newImageFile = new File(imageFileName);
                        res = imageFile.renameTo(newImageFile);
                        if (res) {
                            if (!imageFileName.equals(oldImageFileName) || photoChanged) {
                                // TODO: change file name on server
                            }
                        }
                    }
                    return true;
                } else
                    return false;
            }
            else {
                Log.e(TAG, "data corruption: token or user email missing from shared preference");
            }

            return false;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, "");
        String fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "POVI/" + userEmail + "_" + name.getEditableText() + ".jpg";
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                fileName = userEmail + "_" + name.getEditableText() + ".jpg";
                Intent cropIntent = PoviUtils.dispatchCropImageIntent(picUri.getPath(), 3, 2, fileName);
                croppedUri = cropIntent.getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
                startActivityForResult(cropIntent, PoviConstants.PIC_CROP);
            }
            if (requestCode == PIC_CROP) {
                setPic();
                // Upload picture
                File imgFile = new File(croppedUri.getPath());
                PoviUtils.uploadFile(getActivity(), imgFile.getName(), imgFile, new TransferListener() {
                    @Override
                    public void onStateChanged(int i, TransferState transferState) {
                        if (transferState == TransferState.COMPLETED) {
                            // Manage success
                            if (uploadPicDialog != null)
                                uploadPicDialog.cancel();
                        } else if (transferState == TransferState.FAILED) {
                            // Manage error
                            if (uploadPicDialog != null)
                                uploadPicDialog.cancel();
                            Snackbar.make(view, "Child's pic upload failed!", Snackbar.LENGTH_LONG).show();
                        }

                    }

                    @Override
                    public void onProgressChanged(int i, long l, long l1) {

                    }

                    @Override
                    public void onError(int i, Exception e) {
                        if (uploadPicDialog != null)
                            uploadPicDialog.cancel();
                        Snackbar.make(view, "Child's pic upload failed!", Snackbar.LENGTH_LONG).show();

                    }
                });

            }
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {

                Uri uri = data.getData();
                //Bitmap bitmap = null;
                InputStream input = null;

                File imageFile = new File(fileName);

                OutputStream outStream = null;
                try {
                    input = getActivity().getContentResolver().openInputStream(uri);
                    outStream = new FileOutputStream(imageFile);
                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        outStream.write(buffer, 0, read);
                    }
                    outStream.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Snackbar.make(name, "Error loading picture!", Snackbar.LENGTH_LONG).show();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    Snackbar.make(name, "Error opening picture file!", Snackbar.LENGTH_LONG).show();
                    return;
                }
                finally {
                    try {
                        outStream.close();
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Snackbar.make(name, "Error opening picture file!", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                }

                String fileName2 = userEmail + "_" + name.getEditableText() + ".jpg";
                Intent cropIntent = PoviUtils.dispatchCropImageIntent(fileName, 3, 2, fileName2);
                croppedUri = cropIntent.getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
                startActivityForResult(cropIntent, PoviConstants.PIC_CROP);
            }
        }
    }

    private void setPic() {
        // Get scaled image
        Bitmap bmp = PoviUtils.getScaledBitmap(croppedUri.getPath(),childImage.getWidth(), childImage.getHeight());
        if (bmp != null)
            childImage.setImageBitmap(bmp);
        photoChanged = true;
    }

    // Utility methods
    public String getChildName(){
        return childName;
    }

    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String fileName;

        public BitmapWorkerTask(ImageView imageView){
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
                else{
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                    String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, "");
                    String imageFileName = userEmail + "_" + childName + ".jpg";
                    // Try to retrieve file from server, if existing
                    boolean res = PoviUtils.existsS3Object(getActivity().getApplicationContext(), imageFileName);
                    if (res){
                        final ProgressDialog downloadPicDialog = new ProgressDialog(getActivity().getApplicationContext());
                        downloadPicDialog.setIndeterminate(true);
                        downloadPicDialog.setMessage("Please wait");
                        downloadPicDialog.setTitle("Retrieving child image picture...");
                        downloadPicDialog.setCancelable(false);
                        downloadPicDialog.show();
                        File picFile = new File(fileName);
                        PoviUtils.downloadFile(getActivity().getApplicationContext(), imageFileName, picFile, new TransferListener() {
                            @Override
                            public void onStateChanged(int i, TransferState transferState) {
                                if (transferState == TransferState.COMPLETED) {
                                    Bitmap bmp = PoviUtils.getScaledBitmap(fileName, childImage.getWidth(), childImage.getHeight());
                                    // Manage success
                                    if (downloadPicDialog != null)
                                        downloadPicDialog.cancel();


                                } else if (transferState == TransferState.FAILED) {
                                    // Manage error
                                    if (downloadPicDialog != null)
                                        downloadPicDialog.cancel();
                                    Snackbar.make(name, "Pic retieval failed!", Snackbar.LENGTH_LONG).show();
                                }

                            }

                            @Override
                            public void onProgressChanged(int i, long l, long l1) {

                            }

                            @Override
                            public void onError(int i, Exception e) {
                                if (downloadPicDialog != null)
                                    downloadPicDialog.cancel();
                                Snackbar.make(name, "Pic retrieval failed!", Snackbar.LENGTH_LONG).show();

                            }
                        });
                    }
                }
            }
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            fileName = params[0];


            // Get the dimensions of the View
            final int targetW = childImage.getWidth();
            final int targetH = childImage.getHeight();

            if (targetH == 0 || targetW == 0)
                return null;

            return  PoviUtils.getScaledBitmap(fileName, targetW, targetH);
        }
    }

    public void loadBitmap(String fileName, ImageView imageView) {
        BitmapWorkerTask task = new BitmapWorkerTask(imageView);
        task.execute(fileName);
    }

    // validating email id
    private boolean isValidName(String name) {
        String NAME_PATTERN = "[a-z][a-z]+[a-z\\s]*";
        Pattern pattern = Pattern.compile(NAME_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(name);
        return matcher.matches();
    }
    }
