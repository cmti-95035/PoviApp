package com.antwish.povi.familyconnect;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static java.io.File.createTempFile;
import static java.io.File.separator;

public class PoviUtils {
    private static final String TAG = "PoviUtils";

    // We only need one instance of the clients and credentials provider
    private static AmazonS3Client sS3Client;
    private static CognitoCachingCredentialsProvider sCredProvider;
    private static TransferUtility sTransferUtility;

    /**
     * Gets an instance of CognitoCachingCredentialsProvider which is
     * constructed using the given Context.
     *
     * @param context An Context instance.
     * @return A default credential provider.
     */
    private static CognitoCachingCredentialsProvider getCredProvider(Context context) {
        if (sCredProvider == null) {
            sCredProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    PoviConstants.COGNITO_POOL_ID,
                    Regions.US_EAST_1);
        }
        return sCredProvider;
    }

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @param context An Context instance.
     * @return A default S3 client.
     */
    public static AmazonS3Client getS3Client(Context context) {
        if (sS3Client == null) {
            sS3Client = new AmazonS3Client(getCredProvider(context.getApplicationContext()));
            // Set the region of your S3 bucket
            sS3Client.setRegion(Region.getRegion(Regions.US_EAST_1));
        }
        return sS3Client;
    }

    /**
     * Gets an instance of the TransferUtility which is constructed using the
     * given Context
     *
     * @param context
     * @return a TransferUtility instance
     */
    public static TransferUtility getTransferUtility(Context context) {
        if (sTransferUtility == null) {
            sTransferUtility = new TransferUtility(getS3Client(context.getApplicationContext()),
                    context.getApplicationContext());
        }

        return sTransferUtility;
    }

    public static URL generateMediaUrl(Context context, String objectName){
        AmazonS3Client s3Client = getS3Client(context);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(PoviConstants.BUCKET_NAME, objectName);
        return  s3Client.generatePresignedUrl(request);
    }


    public static Intent dispatchLoadPictureIntent() {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return intent;
    }

    public static Intent dispatchLoadVideoIntent() {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("video/mp4");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return intent;
    }

    public static Intent dispatchLoadAudioIntent() {
        Intent intent = new Intent();
        intent.setType("audio/amr");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return intent;
    }

    public static Intent dispatchTakePictureIntent(String fileName, Context context) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                // Create Povi dir if not existing
                File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES) + separator + "POVI");
                if (!storageDir.exists()){
                    // Try to create dir
                    boolean res =  storageDir.mkdirs();
                    if (!res) {
                        return null;
                    }
                }
                photoFile = createTempFile(
                        fileName,  /* prefix */
                        ".jpg",         /* suffix */
                        storageDir      /* directory */
                );

                // Save a file: path for use with ACTION_VIEW intents
                //mCurrentPhotoPath = "file:" + photoFile.getAbsolutePath();

            } catch (IOException ex) {
                // Error occurred while creating the File
                return null;
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
//                Uri picUri = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
                return takePictureIntent;
            }

        }

        return null;
    }

    public static Intent dispatchRecordVideoIntent(String fileName, Context context) {
        Intent recordVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (recordVideoIntent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
            File videoFile = null;
            // Create Povi dir if not existing
            File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES) + separator + "POVI");
            if (!storageDir.exists()){
                // Try to create dir
                boolean res =  storageDir.mkdirs();
                if (!res) {
                    return null;
                }
            }
            videoFile = new File (fileName);

            // Save a file: path for use with ACTION_VIEW intents
            //mCurrentPhotoPath = "file:" + photoFile.getAbsolutePath();

            // Continue only if the File was successfully created
            if (videoFile != null) {
//                Uri picUri = Uri.fromFile(photoFile);
                recordVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(videoFile));
                //recordVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60);
                //recordVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
                recordVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT,10485760L);
                return recordVideoIntent;
            }

        }

        return null;
    }

    public static Intent dispatchRecordAudioIntent(String fileName, Context context) {
        Intent recordAudioIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        // Ensure that there's a camera activity to handle the intent
        if (recordAudioIntent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
           recordAudioIntent.putExtra(MediaStore.Audio.Media.EXTRA_MAX_BYTES,1048576L);
            return recordAudioIntent;
        }
        return null;
    }

    public static void uploadFile(Context context, String fileName, File file, TransferListener listener){
        // Get transfer utility
        TransferUtility transferUtility = getTransferUtility(context);
        TransferObserver observer = transferUtility.upload(PoviConstants.BUCKET_NAME, fileName,
                file);
        observer.setTransferListener(listener);
    }

    public static void downloadFile(Context context, String fileName, File file, TransferListener listener){
        // Get transfer utility
        TransferUtility transferUtility = getTransferUtility(context);
        TransferObserver observer = transferUtility.download(PoviConstants.BUCKET_NAME, fileName, file);
        observer.setTransferListener(listener);
    }

    public static boolean deleteS3Object(Context context, String fileName) {
        AmazonS3 s3Client = getS3Client(context.getApplicationContext());
        try {
            s3Client.deleteObject(new DeleteObjectRequest(PoviConstants.BUCKET_NAME, fileName));
            return true;
        } catch (AmazonServiceException ase) {
            Log.e(TAG, "Caught an AmazonServiceException.");
            Log.e(TAG, "Error Message:    " + ase.getMessage());
            Log.e(TAG, "HTTP Status Code: " + ase.getStatusCode());
            Log.e(TAG, "AWS Error Code:   " + ase.getErrorCode());
            Log.e(TAG, "Error Type:       " + ase.getErrorType());
            Log.e(TAG, "Request ID:       " + ase.getRequestId());
            return false;
        } catch (AmazonClientException ace) {
            Log.e(TAG, "Caught an AmazonClientException.");
            Log.e(TAG, "Error Message: " + ace.getMessage());
            return false;
        }
    }

    public static boolean existsS3Object(Context context, String fileName) {
        AmazonS3 s3Client = getS3Client(context.getApplicationContext());
        try {
            S3Object object = s3Client.getObject(PoviConstants.BUCKET_NAME, fileName);
            return object != null;
        } catch (AmazonServiceException ase) {
            Log.e(TAG, "Caught an AmazonServiceException.");
            Log.e(TAG, "Error Message:    " + ase.getMessage());
            Log.e(TAG, "HTTP Status Code: " + ase.getStatusCode());
            Log.e(TAG, "AWS Error Code:   " + ase.getErrorCode());
            Log.e(TAG, "Error Type:       " + ase.getErrorType());
            Log.e(TAG, "Request ID:       " + ase.getRequestId());
            return false;
        } catch (AmazonClientException ace) {
            Log.e(TAG, "Caught an AmazonClientException.");
            Log.e(TAG, "Error Message: " + ace.getMessage());
            return false;
        }
    }

    public static void renameS3Object(Context context, String oldFileName, String newFileName) {
        AmazonS3 s3Client = getS3Client(context.getApplicationContext());
        try {
            // unfortunately there's no direct way to rename a file in a S3 bucket so first copy to the new name and then delete the old one
            s3Client.copyObject(new CopyObjectRequest(PoviConstants.BUCKET_NAME, oldFileName, PoviConstants.BUCKET_NAME, newFileName));
            s3Client.deleteObject(new DeleteObjectRequest(PoviConstants.BUCKET_NAME, oldFileName));
        } catch (AmazonServiceException ase) {
            Log.e(TAG, "Caught an AmazonServiceException.");
            Log.e(TAG, "Error Message:    " + ase.getMessage());
            Log.e(TAG, "HTTP Status Code: " + ase.getStatusCode());
            Log.e(TAG, "AWS Error Code:   " + ase.getErrorCode());
            Log.e(TAG, "Error Type:       " + ase.getErrorType());
            Log.e(TAG, "Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            Log.e(TAG, "Caught an AmazonClientException.");
            Log.e(TAG, "Error Message: " + ace.getMessage());
        }
    }

    public static Bitmap getScaledBitmap(String fileName, int targetW, int targetH){
        File imgFile = new File (fileName);
        if (!imgFile.exists())
            // Picture not existing
            return null;

        // Picture exists
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();

        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;



        ExifInterface exif = null;
        try {
            exif = new ExifInterface(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String orient = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orient != null ? Integer.parseInt(orient) :  ExifInterface.ORIENTATION_NORMAL;

        int rotationAngle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;
        Bitmap bitmap = BitmapFactory.decodeFile(fileName, bmOptions);

        Matrix m = new Matrix();
        m.setRotate(rotationAngle,
                (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

        Bitmap bmp = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

        return bmp;
    }

    public static Intent dispatchCropImageIntent(String imageFileName, int aspectRatioX, int aspectRatioY, String croppedImageFilename){
        File imageFile = new File(imageFileName);
        if (!imageFile.exists())
            return null;

        // Else file exists
        //call the standard crop action intent (the user device may not support it)
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        //indicate image type and Uri
        cropIntent.setDataAndType(Uri.fromFile(imageFile), "image/*");
        //set crop properties
        cropIntent.putExtra("crop", "true");
        //indicate aspect of desired crop
        cropIntent.putExtra("aspectX", aspectRatioX);
        cropIntent.putExtra("aspectY", aspectRatioY);
        cropIntent.putExtra("scale", "true");
        //indicate output X and Y
        // cropIntent.putExtra("outputX", 96);
        // cropIntent.putExtra("outputY", 96);
        //retrieve data on return
        //cropIntent.putExtra("return-data", true);
        //start the activity - we handle returning in onActivityResult
        File cropFile = null;
        // Create Povi dir if not existing
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "POVI");

        if(!storageDir.exists())
        {
            // create the directory if it doesn't exist yet
            storageDir.mkdir();
        }

        cropFile = new File(storageDir, croppedImageFilename);
        Uri croppedUri = Uri.fromFile(cropFile);
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, croppedUri);

        return cropIntent;
    }

    public static int generateRainbowColor(int pos){
        int position = pos % 20;
        int color = 0;
        switch (position){
            case 0: color = Color.parseColor("#f44336"); break;
            case 1: color = Color.parseColor("#e91e63"); break;
            case 2: color = Color.parseColor("#9c27b0"); break;
            case 3: color = Color.parseColor("#673ab7"); break;
            case 4: color = Color.parseColor("#3f51b5"); break;
            case 5: color = Color.parseColor("#2196f3"); break;
            case 6: color = Color.parseColor("#03a9f4"); break;
            case 7: color = Color.parseColor("#00bcd4"); break;
            case 8: color = Color.parseColor("#009688"); break;
            case 9: color = Color.parseColor("#4caf50"); break;
            case 10: color = Color.parseColor("#8bc34a"); break;
            case 11: color = Color.parseColor("#cddc39"); break;
            case 12: color = Color.parseColor("#ffeb3b"); break;
            case 13: color = Color.parseColor("#ffc107"); break;
            case 14: color = Color.parseColor("#ff9800"); break;
            case 15: color = Color.parseColor("#ff5722"); break;
            case 16: color = Color.parseColor("#795548"); break;
            case 17: color = Color.parseColor("#9e9e9e"); break;
            case 18: color = Color.parseColor("#607d8b"); break;
            case 19: color = Color.parseColor("#000000"); break;
        }
        return color;
    }

    public static int generateMaterialColor(int pos){
        int position = pos % 4;
        int color = 0;
        switch (position){
            case 0: color = Color.parseColor("#009688"); break;
            case 1: color = Color.parseColor("#f44336"); break;
            case 2: color = Color.parseColor("#ffc107"); break;
            case 3: color = Color.parseColor("#00bcd4"); break;
        }
        return color;
    }

    public static Bitmap compensateBitmapRotation(String fileName){
        File imgFile = new File (fileName);
        if (!imgFile.exists())
            // Picture not existing
            return null;

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String orient = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orient != null ? Integer.parseInt(orient) :  ExifInterface.ORIENTATION_NORMAL;

        int rotationAngle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;
        Bitmap bitmap = BitmapFactory.decodeFile(fileName);

        Matrix m = new Matrix();
        m.setRotate(rotationAngle,
                (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

        Bitmap bmp = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

        return bmp;
    }
}
