package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddChildActivity extends AppCompatActivity {
    // TODO: Rename parameter arguments, choose names that match
    private static final String TAG = "Add child";
    private EditText name;
    private Spinner gender;
    private ProgressDialog addDialog;
    private String childName;
    private String childGender;
    private DatePickerDialog datePicker;
    private long birthdateTimestamp = 0;
    private EditText birthdateText;
    private Context context;

    private Uri picUri;
    private Uri croppedUri;
    private ImageView childImage;

    final CharSequence[] items = {"Take a picture", "Load a picture", "Delete picture"};
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int PIC_CROP = 2;
    private static final int PICK_IMAGE_REQUEST = 3;
    private ProgressDialog uploadPicDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_addchild);

        context = getApplicationContext();



        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            // Add top padding if required
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){
                final float scale = getResources().getDisplayMetrics().density;
                int top = (int) (30 * scale + 0.5f);
                mToolbar.setPadding(0,top,0,0);
            }
            setSupportActionBar(mToolbar);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        addDialog = new ProgressDialog(this);
        addDialog.setIndeterminate(true);
        addDialog.setMessage("Please wait");
        addDialog.setTitle("Adding child...");
        addDialog.setCancelable(false);

        Calendar cal = Calendar.getInstance();

        datePicker = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
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

        // Add genders
        String[] genders = {"male", "female", "unspecified"};
        gender = (Spinner) findViewById(R.id.gender);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, genders);
        gender.setAdapter(spinnerAdapter);

        // Get controls
        name = (EditText) findViewById(R.id.name);
        birthdateText = (EditText) findViewById(R.id.birthday);
        birthdateText.setFocusable(false);
        birthdateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker.show();
            }
        });

        // Set child image 3:2
        childImage = (ImageView) findViewById(R.id.childProfileImageView);
        //childImage.setMinimumHeight((int) (childImage.getWidth() / 3.0) * 2);
        //childImage.setMaxHeight((int) (childImage.getWidth() / 3.0) * 2);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set child picture");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, "");
                String fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "POVI/" + userEmail + "_" + name.getEditableText() + ".jpg";
                switch (item){
                    case 0:
                        fileName = userEmail + "_" + name.getEditableText() + ".jpg";
                        Intent takeIntent = PoviUtils.dispatchTakePictureIntent(fileName, context);
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
                        // Delete picture
                        final File imgFile = new File(fileName);
                        fileName = userEmail + "_" + name.getEditableText() + ".jpg";
                        boolean res = true;
                        if (imgFile.exists()) {
                            res = imgFile.delete();
                            childImage.setImageResource(R.drawable.person);
                        }
                        if (!res)
                            Snackbar.make(name, "Pic deletion failed!", Snackbar.LENGTH_LONG).show();
                        break;
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.pictureButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (name.getText().toString() == null || name.getText().toString().length() < 1)
                    Snackbar.make(childImage, "Child's name not set!", Snackbar.LENGTH_SHORT).show();
                else{

                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }
        });

        uploadPicDialog = new ProgressDialog(this);
        uploadPicDialog.setIndeterminate(true);
        uploadPicDialog.setMessage("Please wait");
        uploadPicDialog.setTitle("Uploading child picture...");
        uploadPicDialog.setCancelable(false);

        fab.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_addchild, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.accept:
                // send add child notification to activity
                childName = name.getText().toString();
                if (!isValidName(childName)) {
                    name.setError("Invalid name!");
                    name.requestFocus();
                }
                else
                    new AddChildTask().execute();
                return true;
            case R.id.cancel:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class AddChildTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Get data
            childName = name.getText().toString();
            childGender = gender.getSelectedItem().toString();
            addDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (addDialog != null)
                addDialog.cancel();

            if (result) {
                //Snackbar.make(childImage, "Child creation succeded!", Snackbar.LENGTH_SHORT).show();
                finish();
            } else
            if (childName != null && childName.length() > 0)
                Snackbar.make(childImage, "Child creation failed!", Snackbar.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... params) {

            // Check child's name
            if (childName == null || childName.length() < 1) {
                Snackbar.make(childImage, "Child's name not set!", Snackbar.LENGTH_SHORT).show();
                return false;
            }

            // Get current access token
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String token = sharedPref.getString(PoviConstants.POVI_TOKEN, null);
            String userEmail = sharedPref.getString(PoviConstants.POVI_USERID, null);

//            // Get user e-mail
//            User user = RestServer.getUserProfile(token);
//            if (user == null)
//                return false;
//
//            String userEmail = user.getEmail();
            if(token == null || userEmail == null)
                return false;
            // Add child
            // Collect data

            boolean res = RestServer.registerNewChild(userEmail, childName, childGender, birthdateTimestamp, token);
            if (res) {
                if(croppedUri != null) {
                    // also needs to upload the child's picture
                    //uploadChildImageInBackground(userEmail, childName, croppedUri.getPath(), token);
                    // Upload picture
                    File imgFile = new File(croppedUri.getPath());
                    PoviUtils.uploadFile(context, imgFile.getName(), imgFile, new TransferListener() {
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
                                Snackbar.make(name, "Child's pic upload failed!", Snackbar.LENGTH_LONG).show();
                            }

                        }

                        @Override
                        public void onProgressChanged(int i, long l, long l1) {

                        }

                        @Override
                        public void onError(int i, Exception e) {
                            if (uploadPicDialog != null)
                                uploadPicDialog.cancel();
                            Snackbar.make(name, "Child's pic upload failed!", Snackbar.LENGTH_LONG).show();

                        }
                    });
                }
                return true;
            }
            else
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
                uploadPicDialog.show();
                // Upload picture
                File imgFile = new File(croppedUri.getPath());
                PoviUtils.uploadFile(this, imgFile.getName(), imgFile, new TransferListener() {
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
                            Snackbar.make(name, "Child's pic upload failed!", Snackbar.LENGTH_LONG).show();
                        }

                    }

                    @Override
                    public void onProgressChanged(int i, long l, long l1) {

                    }

                    @Override
                    public void onError(int i, Exception e) {
                        if (uploadPicDialog != null)
                            uploadPicDialog.cancel();
                        Snackbar.make(name, "Child's pic upload failed!", Snackbar.LENGTH_LONG).show();

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
                    input = getContentResolver().openInputStream(uri);
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
    }

    // validating email id
    private boolean isValidName(String name) {
        String NAME_PATTERN = "[a-z][a-z]+[a-z\\s]*";
        Pattern pattern = Pattern.compile(NAME_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(name);
        return matcher.matches();
    }
}