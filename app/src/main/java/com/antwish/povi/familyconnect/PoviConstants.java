package com.antwish.povi.familyconnect;

/**
 * For all publicly available constants
 */
public interface PoviConstants {
    // child image related
    int REQUEST_TAKE_PHOTO = 1;
    int PIC_CROP = 2;
    int PICK_IMAGE_REQUEST = 3;
    int REQUEST_VIDEO_CAPTURE = 4;
    int PICK_VIDEO_REQUEST = 5;
    int REQUEST_AUDIO_CAPTURE = 6;
    int PICK_AUDIO_REQUEST = 7;

    // to be stored in shared preferences
    String POVI_TOKEN = "povi_token";
    String POVI_USERID = "povi_userid";
    String POVI_USERNAME = "povi_username";
    String POVI_USERPIC = "povi_userpic";

    String COGNITO_POOL_ID = "us-east-1:c1af80b6-3c54-4d6f-8b93-fcb24d2bc51a";
    String BUCKET_NAME = "povifamilyconnectchildrenimages";

    //String COGNITO_POOL_ID = "us-east-1:e553901f-3e21-42a9-a90f-8961c5c8f59b";
    //String BUCKET_NAME = "androidapplication";

}
