package com.antwish.povi.familyconnect;

import android.util.Log;

import com.antwish.povi.server.BeatComment;
import com.antwish.povi.server.BeatCommentArray;
import com.antwish.povi.server.BeatComments;
import com.antwish.povi.server.Child;
import com.antwish.povi.server.ChildCreateRequestBuilder;
import com.antwish.povi.server.ChildDeleteRequestBuilder;
import com.antwish.povi.server.ChildFindByGetChildrenRequestBuilder;
import com.antwish.povi.server.ChildId;
import com.antwish.povi.server.ChildImage;
import com.antwish.povi.server.ChildImageCreateRequestBuilder;
import com.antwish.povi.server.ChildImageGetRequestBuilder;
import com.antwish.povi.server.ChildImageRequestBuilders;
import com.antwish.povi.server.ChildRequestBuilders;
import com.antwish.povi.server.ChildUpdateRequestBuilder;
import com.antwish.povi.server.Comment;
import com.antwish.povi.server.CommentId;
import com.antwish.povi.server.EventType;
import com.antwish.povi.server.ParentingTip;
import com.antwish.povi.server.ParentingTipArray;
import com.antwish.povi.server.ParentingTipDoObtainWebLinkRequestBuilder;
import com.antwish.povi.server.ParentingTipId;
import com.antwish.povi.server.ParentingTipRequestBuilders;
import com.antwish.povi.server.PoviActionsDoLoginEmailRequestBuilder;
import com.antwish.povi.server.PoviActionsDoLoginFacebookRequestBuilder;
import com.antwish.povi.server.PoviActionsDoLogoutRequestBuilder;
import com.antwish.povi.server.PoviActionsDoResetPasswordRequestBuilder;
import com.antwish.povi.server.PoviActionsDoValidateTokenRequestBuilder;
import com.antwish.povi.server.PoviActionsRequestBuilders;
import com.antwish.povi.server.PoviEvent;
import com.antwish.povi.server.PoviEventRequestBuilders;
import com.antwish.povi.server.TipCommentCreateRequestBuilder;
import com.antwish.povi.server.TipCommentDeleteRequestBuilder;
import com.antwish.povi.server.TipCommentDoAddBeatCommentRequestBuilder;
import com.antwish.povi.server.TipCommentDoDeleteBeatCommentRequestBuilder;
import com.antwish.povi.server.TipCommentDoDeleteCommentRequestBuilder;
import com.antwish.povi.server.TipCommentDoGetBeatCommentsRequestBuilder;
import com.antwish.povi.server.TipCommentDoGetBeatCommentsWithLikeStatusRequestBuilder;
import com.antwish.povi.server.TipCommentDoSetLikeStatusRequestBuilder;
import com.antwish.povi.server.TipCommentFindByGetCommentsLikedPagedRequestBuilder;
import com.antwish.povi.server.TipCommentFindByGetCommentsPagedPerChildRequestBuilder;
import com.antwish.povi.server.TipCommentFindByGetCommentsPagedRequestBuilder;
import com.antwish.povi.server.TipCommentFindByGetCommentsSharedRequestBuilder;
import com.antwish.povi.server.TipCommentRequestBuilders;
import com.antwish.povi.server.TipCommentUpdateRequestBuilder;
import com.antwish.povi.server.User;
import com.antwish.povi.server.UserCreateRequestBuilder;
import com.antwish.povi.server.UserGetRequestBuilder;
import com.antwish.povi.server.UserRequestBuilders;
import com.antwish.povi.server.UserUpdateRequestBuilder;
import com.antwish.povi.server.VoiceComment;
import com.antwish.povi.server.VoiceCommentCreateRequestBuilder;
import com.antwish.povi.server.VoiceCommentRequestBuilders;
import com.antwish.povi.server.WebLink;
import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.SetMode;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.DeleteRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.UpdateRequest;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestServer {
    // Create an HttpClient and wrap it in an abstraction layer
    private static final HttpClientFactory http = new HttpClientFactory();
    private static final Client r2Client = new TransportClientAdapter(
            http.getClient(Collections.<String, String> emptyMap()));
    private static final String BASE_URL = "http://54.183.228.194:8080/";
    //private static final String BASE_URL = "http://192.168.0.100:8080/";
    //private static final String BASE_URL = "http://52.8.44.217:8080/";
    // Create a RestClient to talk to localhost:8080
    private static RestClient restClient = new RestClient(r2Client, BASE_URL);
    private static ParentingTipRequestBuilders parentingTipRequestBuilders = new ParentingTipRequestBuilders();
    private static final String TAG = "SocialPlayRestServer";
    public static String POVI_AUTHORIZATION_HEADER = "povi-authorization";
    public static String FAKE_POVI_TOKEN = "POVI-TOKEN";    // TODO:just include something in the request for now but needs to pass the real token
    public static List<PoviStory> POVI_STORIES = new ArrayList<>();
    private static int STORY_INDEX = -1;

    public static Long createEvent(String token, String userId, String details, EventType eventType)
    {
        try{
            PoviEvent event = new PoviEvent().setEmail(userId).setEventType(eventType).setDetails(details);
            CreateIdRequest<Long, PoviEvent> createIdRequest = new PoviEventRequestBuilders().create().input(event).addHeader(POVI_AUTHORIZATION_HEADER, token).build();
            ResponseFuture<IdResponse<Long>> responseFuture = restClient.sendRequest(createIdRequest);
            Response<IdResponse<Long>> response = responseFuture.getResponse();

            return response.getEntity().getId();
        }catch (RemoteInvocationException ex)
        {
            System.out.println("Encountered error when fetching tips from selected days " + ex.getMessage());
            for(StackTraceElement element : ex.getStackTrace()){
                System.out.println(element);
            }
        }

        return null;
    }

    public static ParentingTip[] getTips(String token, String userId, String dateStr)
    {
        try
        {
            ActionRequest<ParentingTipArray> actionRequest = parentingTipRequestBuilders.actionGetTips().userIdParam(userId).isAndroidParam(true).dateStrParam(dateStr).countParam(3).addHeader(POVI_AUTHORIZATION_HEADER, token).build();
            ResponseFuture<ParentingTipArray> responseFuture = restClient.sendRequest(actionRequest);
            Response<ParentingTipArray> response = responseFuture.getResponse();

            return response.getEntity().toArray(new ParentingTip[3]);
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing getTips: " + ex.getMessage());
        }

        return new ParentingTip[0];
    }

    public static ParentingTip[] getTipsSelectedDay(String token, String userId, String dateStr)
    {
        try
        {

            ActionRequest<ParentingTipArray> actionRequest = parentingTipRequestBuilders.actionGetTipsSelectedDay().userIdParam(userId).dateStrParam(dateStr).countParam(3).addHeader(POVI_AUTHORIZATION_HEADER, token).build();
            ResponseFuture<ParentingTipArray> responseFuture = restClient.sendRequest(actionRequest);
            Response<ParentingTipArray> response = responseFuture.getResponse();

            return response.getEntity().toArray(new ParentingTip[3]);
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error when fetching tips from selected days " + ex.getMessage());
        }

        return new ParentingTip[0];
    }

    public static ParentingTip[] getRefreshTips(String token, String userId)
    {
        try
        {
            ActionRequest<ParentingTipArray> actionRequest = parentingTipRequestBuilders.actionGetRefreshTips().userIdParam(userId).countParam(3).addHeader(POVI_AUTHORIZATION_HEADER, token).build();
            ResponseFuture<ParentingTipArray> responseFuture = restClient.sendRequest(actionRequest);
            Response<ParentingTipArray> response = responseFuture.getResponse();
            return response.getEntity().toArray(new ParentingTip[3]);
        }catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error when fetching refreshing tips: " + ex.getMessage());
        }
        return new ParentingTip[0];
    }

    // Fabio's added method
    /** method for clients */
    public static RestResponse<String> registerNewAccount(final String email, final String hash, final String name, final String phone, final String address, long birthdate){
        // Construct a request for the specified fortune
        UserCreateRequestBuilder rb = new UserRequestBuilders().create();
        CreateIdRequest<String, User> registerReq = rb.input(new User().setEmail(email).setHash(hash).setName(name).setPhone(phone).setBirthdate(birthdate)).build();

        // Send the request and wait for a response
        final ResponseFuture<IdResponse<String>> getFuture = restClient.sendRequest(registerReq);
        final Response<IdResponse<String>> resp;
        RestResponse<String> result = new RestResponse<>(HttpStatus.S_200_OK.getCode(), null, null);
        try {
            resp = getFuture.getResponse();
            result.setStatusCode(resp.getStatus());
            if (resp.getError() != null) {
                result.setErrorMsg(resp.getError().getMessage());
            }
            result.setEntity(resp.getEntity().getId());
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            if (e instanceof RestLiResponseException) {
                result.setStatusCode(((RestLiResponseException) e).getStatus());
                result.setErrorMsg(((RestLiResponseException) e).getServiceErrorMessage());
            } else {
                result.setStatusCode(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
                result.setErrorMsg(e.getMessage());
            }
        }

        return result;
    }

    public static boolean updateProfile(final String token, final String oldEmail, final String newEmail, final String hash, final String name, final String nickname, final String phone, final String address, long birthdate, String gender){
        // Creating the profile update request builder
        UserUpdateRequestBuilder updateRequestBuilder = new UserRequestBuilders().update();

        UpdateRequest updateReq = updateRequestBuilder.id(oldEmail).input(new User().
                setEmail(newEmail)
                .setHash(hash)
                .setName(name, SetMode.IGNORE_NULL)
                .setPhone(phone, SetMode.IGNORE_NULL)
                .setAddress(address, SetMode.IGNORE_NULL)
                .setBirthdate(birthdate)
                .setNickName(nickname, SetMode.IGNORE_NULL)
        .setGender(gender))
                .addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();

        // Send the request and wait for a response
        final ResponseFuture getFuture = restClient.sendRequest(updateReq);

        // If you get an OK response, then the comment has been updated in the table
        final Response resp;
        try {
            resp = getFuture.getResponse();
            if (resp.getStatus() == HttpStatus.S_200_OK.getCode()) {
                return true;
            }
        } catch (RemoteInvocationException e) {
            e.printStackTrace();

        }
        return false;
    }

    public static RestResponse<String> loginEmail(final String email, final String pwd){
        // Create hash
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        digest.reset();
        try {
            String data = email.toLowerCase()+pwd;
            digest.update(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String hash = new BigInteger(1, digest.digest()).toString(16);


        // Construct a request for the specified fortune
        PoviActionsDoLoginEmailRequestBuilder rb = new PoviActionsRequestBuilders().actionLoginEmail().emailParam(email).hashParam(hash);
        ActionRequest<String> registerReq = rb.build();

        // Send the request and wait for a response
        final ResponseFuture<String> getFuture = restClient.sendRequest(registerReq);
        final Response<String> resp;
        RestResponse<String> result = new RestResponse<>(HttpStatus.S_200_OK.getCode(), null, null);
        try {
            resp = getFuture.getResponse();
            result.setStatusCode(resp.getStatus());
            if (resp.getError() != null) {
                result.setErrorMsg(resp.getError().getMessage());
            }
            result.setEntity(resp.getEntity());
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            if (e instanceof RestLiResponseException) {
                result.setStatusCode(((RestLiResponseException) e).getStatus());
                result.setErrorMsg(((RestLiResponseException) e).getServiceErrorMessage());
            } else {
                result.setStatusCode(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
                result.setErrorMsg(e.getMessage());
            }
        }

        if(result.getEntity() == null)
            result.setStatusCode(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
        return result;

    }

    /**
     * Login for facebook
     * @param facebookToken
     * @return
     */
    public static String facebookLogin (final String facebookToken){
        // Construct a request for the specified fortune
        PoviActionsDoLoginFacebookRequestBuilder rb = new PoviActionsRequestBuilders().actionLoginFacebook().facebookTokenParam(facebookToken);
        ActionRequest<String> registerReq = rb.build();

        // Send the request and wait for a response
        final ResponseFuture<String> getFuture = restClient.sendRequest(registerReq);
        final Response<String> resp;
        try {
            resp = getFuture.getResponse();
            return resp.getEntity();
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int validateToken(final String token){
        if (token == null)
            return -1;
        PoviActionsDoValidateTokenRequestBuilder rb = new PoviActionsRequestBuilders().actionValidateToken().tokenParam(token);
        ActionRequest<Boolean> validateReq = rb.addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();
        final ResponseFuture<Boolean> getFuture = restClient.sendRequest(validateReq);
        final Response<Boolean> resp;
        try {
            resp = getFuture.getResponse();
            if (resp.getStatus() == HttpStatus.S_200_OK.getCode()){
                boolean res = resp.getEntity();
                if (res)
                    return 0;
                else
                    return -1;
            }
            if (resp.getStatus() == HttpStatus.S_404_NOT_FOUND.getCode())
                return 1;
            else
                return 2;


        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return 2;
        }
    }

    public static boolean logout(final String token){
        PoviActionsDoLogoutRequestBuilder rb = new PoviActionsRequestBuilders().actionLogout().tokenParam(token);
        ActionRequest<Boolean> validateReq = rb.addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();
        final ResponseFuture<Boolean> getFuture = restClient.sendRequest(validateReq);
        final Response<Boolean> resp;
        try {
            resp = getFuture.getResponse();
            return resp.getEntity();
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Child> getChildren(final String token){
        if (token == null)
            return null;

        // Get current user from token
        // Construct a request for the specified fortune
        UserGetRequestBuilder rb = new UserRequestBuilders().get().id(token);
        GetRequest<User> getReq = rb.addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();

        // Send the request and wait for a response
        final ResponseFuture<User> getFuture = restClient.sendRequest(getReq);
        final Response<User> resp;
        try {
            resp = getFuture.getResponse();
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }

        User user = resp.getEntity();

        ChildFindByGetChildrenRequestBuilder crb = new ChildRequestBuilders().findByGetChildren().user_idParam(user.getEmail());
        FindRequest<Child> findReq = crb.addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();
        ResponseFuture<CollectionResponse<Child>> getFutureChildren = restClient.sendRequest(findReq);
        Response<CollectionResponse<Child>> childResp;
        try {
            childResp = getFutureChildren.getResponse();
            return childResp.getEntity().getElements();
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Child> listChildren(final String userId, final String token){
        try {
            if (userId!=null){
                ChildFindByGetChildrenRequestBuilder crb = new ChildRequestBuilders().findByGetChildren().user_idParam(userId);
                FindRequest<Child> findReq = crb.addHeader(POVI_AUTHORIZATION_HEADER, token).build();
                ResponseFuture<CollectionResponse<Child>> getFutureChildren = restClient.sendRequest(findReq);

                Response<CollectionResponse<Child>> response = getFutureChildren.getResponse();
                return response.getEntity().getElements();
            }
        }
        catch (RemoteInvocationException ex)
        {
            Log.e(TAG, "Encountered error doing registerAccount: " + ex.getMessage());
        }
        return null;
    }

    public static boolean registerNewChild(final String userEmail, final String childName, final String gender, long birthdate, String token){
        // Construct a request for the specified fortune
        ChildCreateRequestBuilder rb = new ChildRequestBuilders().create();
        CreateIdRequest<ComplexResourceKey<ChildId, ChildId>, Child> registerReq = rb.input(new Child().setUser_id(userEmail).setName(childName).setBirthdate(birthdate).setGender(gender)).addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();

        // Send the request and wait for a response
        final ResponseFuture<IdResponse<ComplexResourceKey<ChildId, ChildId>>> getFuture = restClient.sendRequest(registerReq);
        final Response<IdResponse<ComplexResourceKey<ChildId, ChildId>>> resp;
        try {
            resp = getFuture.getResponse();
            return true;
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateChild(final String userEmail, final String oldChildName, final String newChildName, final String gender, long birthdate, String token){
        // Construct a request for the specified fortune
        ChildId keyId = new ChildId().setUser_id(userEmail).setChild_Id(oldChildName);
        ComplexResourceKey<ChildId, ChildId> key = new ComplexResourceKey<>(keyId, keyId);
        ChildUpdateRequestBuilder rb = new ChildRequestBuilders().update();
        UpdateRequest<Child> request = rb.id(key)
                .input(new Child().setBirthdate(birthdate).setGender(gender).setUser_id(userEmail).setName(newChildName)).addHeader(POVI_AUTHORIZATION_HEADER, token).build();

        // Send the request and wait for a response
        final ResponseFuture getFuture = restClient.sendRequest(rb);
        final Response resp;
        try {
            resp = getFuture.getResponse();
            return true;
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static User getUserProfile(final String token){
        // Construct a request for the specified fortune
        UserGetRequestBuilder rb = new UserRequestBuilders().get().id(token);
        GetRequest<User> getReq = rb.addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();

        // Send the request and wait for a response
        final ResponseFuture<User> getFuture = restClient.sendRequest(getReq);
        final Response<User> resp;
        try {
            resp = getFuture.getResponse();
            return resp.getEntity();
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteChild(final String token, final String childName){
        User user = getUserProfile(token);
        if (user == null)
            return false;
        ChildId keyId = new ChildId().setUser_id(user.getEmail()).setChild_Id(childName);
        ComplexResourceKey<ChildId, ChildId> key = new ComplexResourceKey<>(keyId, keyId);
        ChildDeleteRequestBuilder rb = new ChildRequestBuilders().delete();
        DeleteRequest deleteReq = rb.id(key).addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();

        // Send the request and wait for a response
        final ResponseFuture getFuture = restClient.sendRequest(deleteReq);
        final Response resp;
        try {
            resp = getFuture.getResponse();
            if (resp.getStatus() == HttpStatus.S_200_OK.getCode()) {
                return true;
            }
        } catch (RemoteInvocationException e) {
            e.printStackTrace();

        }
        return false;
    }

    // Journal methods

    /**
     * Called when a comment needs to be created
     * @param userEmail
     * @param childName
     * @param tipId
     * @param tipString
     * @param timestamp
     * @param commentText
     * @param likeStatus
     * @param token
     * @param resourceId
     * @return boolean value indicating success/failure
     */
    public static boolean createComment(final String userEmail, final String childName,
                                        final int tipId, final String tipString,
                                        final long timestamp, final String commentText,
                                        final boolean likeStatus, final String token, int resourceId){

        // Initialize a create comment request builder
        TipCommentCreateRequestBuilder create_requestBuilder =
                new TipCommentRequestBuilders().create();


        // Initialize a create request
        CreateIdRequest<ComplexResourceKey<CommentId, CommentId>, Comment> createReq =
                create_requestBuilder.input(new Comment().setUserId(userEmail).
                        setTipId(tipId).
                        setResourceId(resourceId).
                        setTipString(tipString).
                        setTimestamp(timestamp).
                        setCommentText(commentText).
                        setLikeStatus(likeStatus).
                        setChildName(childName)).
                        addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();

        // Send the request and wait for a response
        final ResponseFuture<IdResponse<ComplexResourceKey<CommentId, CommentId>>> getFuture =
                restClient.sendRequest(createReq);

        // If you get some response, then the comment has been entered in the table
        final Response<IdResponse<ComplexResourceKey<CommentId, CommentId>>> resp;
        try {
            resp = getFuture.getResponse();
            return true;
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Called when a comment needs to be deleted
     * @param user_email
     * @param childName
     * @param timestamp
     * @param token
     * @return
     */
    public static boolean deleteComment(final String user_email, final String childName,
                                        final long timestamp, final String token){

        // Setting up the commentId key which is used to delete a record
        CommentId keyId = new CommentId().setUser_id(user_email).
                setChild_Id(childName).
                setTimestamp(timestamp);

        ComplexResourceKey<CommentId, CommentId> key = new ComplexResourceKey<>(keyId, keyId);

        // Creating the Comment Delete request builder
        TipCommentDeleteRequestBuilder delete_requestBuilder =
                new TipCommentRequestBuilders().delete();

        DeleteRequest deleteReq = delete_requestBuilder.id(key).
                addHeader(RestServer.POVI_AUTHORIZATION_HEADER,
                        token).build();

        // Send the request and wait for a response
        final ResponseFuture getFuture = restClient.sendRequest(deleteReq);

        // If you get an OK response, then the comment has been deleted from the table
        final Response resp;
        try {
            resp = getFuture.getResponse();
            if (resp.getStatus() == HttpStatus.S_200_OK.getCode()) {
                return true;
            }
        } catch (RemoteInvocationException e) {
            e.printStackTrace();

        }
        return false;
    }

    /**
     * Called when a comment needs to be updated
     * @param userEmail
     * @param childName
     * @param timestamp
     * @param tipId
     * @param commentText
     * @param likeStatus
     * @param token
     * @return
     */
    public static boolean updateComment(final String userEmail, final String childName,
                                        final int tipId, final String tipString,
                                        final long timestamp, final String commentText,
                                        final boolean likeStatus, final String token){

        // Setting up the commentId key which is used to update the record
        CommentId keyId = new CommentId().setUser_id(userEmail).
                setChild_Id(childName).
                setTimestamp(timestamp);

        ComplexResourceKey<CommentId, CommentId> key = new ComplexResourceKey<>(keyId, keyId);

        // Creating the Comment Delete request builder
        TipCommentUpdateRequestBuilder update_requestBuilder =
                new TipCommentRequestBuilders().update();

        UpdateRequest updateReq = update_requestBuilder.id(key).input(new Comment().
                setUserId(userEmail).
                setTipId(tipId).
                setTipString(tipString).
                setTimestamp(timestamp).
                setCommentText(commentText).
                setLikeStatus(likeStatus).
                setChildName(childName)).
                addHeader(RestServer.POVI_AUTHORIZATION_HEADER,
                        token).build();

        // Send the request and wait for a response
        final ResponseFuture getFuture = restClient.sendRequest(updateReq);

        // If you get an OK response, then the comment has been updated in the table
        final Response resp;
        try {
            resp = getFuture.getResponse();
            if (resp.getStatus() == HttpStatus.S_200_OK.getCode()) {
                return true;
            }
        } catch (RemoteInvocationException e) {
            e.printStackTrace();

        }
        return false;
    }

    private static byte[] readFromFile(String fileName) {
        try {
            File file = new File(fileName);
            byte[] fileData = new byte[(int) file.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();

            return fileData;
        }catch (IOException ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static boolean addChildImage(String email, String childName, String fileName, String token){
        byte[] data = readFromFile(fileName);
        if(data == null)
            return false;
        ChildImageCreateRequestBuilder childImageCreateRequestBuilder = new ChildImageRequestBuilders().create();
        CreateIdRequest<ComplexResourceKey<ChildId, ChildId>, ChildImage> createReq = childImageCreateRequestBuilder.input(new ChildImage()
                .setFileName(fileName).setFileContent(ByteString.copy(data)).setEmail(email).setChildName(childName)).addHeader(POVI_AUTHORIZATION_HEADER, token).build();

        final ResponseFuture<IdResponse<ComplexResourceKey<ChildId, ChildId>>> getFuture =
                restClient.sendRequest(createReq);

        // If you get some response, then the comment has been entered in the table
        final Response<IdResponse<ComplexResourceKey<ChildId, ChildId>>> resp;
        try {
            resp = getFuture.getResponse();
            //System.out.println("Successfully added image!");
            return true;
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ChildImage getChildImage(RestClient restClient, String email, String childName, String token){
        ChildImageGetRequestBuilder childImageGetRequestBuilder = new ChildImageRequestBuilders().get();
        ChildId keyId = new ChildId().setUser_id(email).setChild_Id(childName);
        ComplexResourceKey<ChildId, ChildId> key = new ComplexResourceKey<ChildId, ChildId>(keyId, keyId);
        GetRequest<ChildImage> getRequest = childImageGetRequestBuilder.id(key).addHeader(POVI_AUTHORIZATION_HEADER, token).build();

        final ResponseFuture<ChildImage> getFuture = restClient.sendRequest(getRequest);
        try{
            final Response<ChildImage> getResponse = getFuture.getResponse();
            return getResponse.getEntity();
        }catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ChildImage getChildImage(String email, String childName, String token){
        ChildId keyId = new ChildId().setUser_id(email).setChild_Id(childName);
        ComplexResourceKey<ChildId, ChildId> key = new ComplexResourceKey<>(keyId, keyId);
        ChildImageGetRequestBuilder childImageGetRequestBuilder = new ChildImageRequestBuilders().get();
        GetRequest<ChildImage> getReq = childImageGetRequestBuilder.id(key).addHeader(POVI_AUTHORIZATION_HEADER, token).build();
        final ResponseFuture<ChildImage> getFuture =
                restClient.sendRequest(getReq);

        final Response<ChildImage> resp;
        try {
            resp = getFuture.getResponse();
            return resp.getEntity();
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean createVoiceComment(final String userEmail, final String childName,
                                             final long timestamp,
                                             final String fileName, final byte[] content, final String token) {

        VoiceCommentCreateRequestBuilder voiceCommentCreateRequestBuilder = new VoiceCommentRequestBuilders().create();

        // Initialize a create request
        CreateIdRequest<ComplexResourceKey<CommentId, CommentId>, VoiceComment> createReq =
                voiceCommentCreateRequestBuilder.input(new VoiceComment().
                        setEmail(userEmail).
                        setTimestamp(timestamp).
                        setFileContent(ByteString.copy(content)).
                        setChildName(childName).setFileName(fileName)).
                        addHeader(POVI_AUTHORIZATION_HEADER,
                                token).build();

        // Send the request and wait for a response
        final ResponseFuture<IdResponse<ComplexResourceKey<CommentId, CommentId>>> getFuture =
                restClient.sendRequest(createReq);

        final Response<IdResponse<ComplexResourceKey<CommentId, CommentId>>> resp;
        try {
            resp = getFuture.getResponse();
            return true;
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static RestResponse<Boolean> regenerateLoginPassword(final String email){
        // Construct a request for the specified fortune
        PoviActionsDoResetPasswordRequestBuilder rb = new PoviActionsRequestBuilders().actionResetPassword().emailParam(email);
        ActionRequest<Boolean> resetReq = rb.build();

        // Send the request and wait for a response
        final ResponseFuture<Boolean> getFuture = restClient.sendRequest(resetReq);
        final Response<Boolean> resp;
        RestResponse<Boolean> result = new RestResponse<>(HttpStatus.S_200_OK.getCode(), null, null);
        try {
            resp = getFuture.getResponse();
            result.setStatusCode(resp.getStatus());
            if (resp.getError() != null) {
                result.setErrorMsg(resp.getError().getMessage());
            }
            result.setEntity(resp.getEntity());
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            if (e instanceof RestLiResponseException) {
                result.setStatusCode(((RestLiResponseException) e).getStatus());
                result.setErrorMsg(((RestLiResponseException) e).getServiceErrorMessage());
            } else {
                result.setStatusCode(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
                result.setErrorMsg(e.getMessage());
            }
        }

        return result;
    }

    public static WebLink getWebLink(String token){
        ParentingTipDoObtainWebLinkRequestBuilder requestBuilder = new ParentingTipRequestBuilders().actionObtainWebLink();
        ActionRequest<WebLink> actionRequest = requestBuilder.addHeader(POVI_AUTHORIZATION_HEADER, token).build();
        final ResponseFuture<WebLink> responseFuture = restClient.sendRequest(actionRequest);
        try
        {
            return responseFuture.getResponseEntity();

        }catch (RemoteInvocationException ex){
            ex.printStackTrace();
        }

        return null;
    }

    public static List<PoviSubscription> getSubscriptionByType(String token, int start, int count, Long lastSubscriptionId, PoviSubscriptiontype subscriptiontype){
        //TODO: hook up server to provide subscription list per user
        List<PoviSubscription> subscriptions = new ArrayList<>();

        switch (subscriptiontype){
            case CATEGORY:
                subscriptions.add(new PoviSubscription("Friendship is a relationship of mutual affection between two or more people.", R.drawable.friendship, 3, 5.99f, PoviSubscriptiontype.CATEGORY, "Friendship"));
                subscriptions.add(new PoviSubscription("New school is challenging", R.drawable.newschool, 3, 5.99f, PoviSubscriptiontype.CATEGORY, "New School"));
                subscriptions.add(new PoviSubscription("Povi and Gooba", R.drawable.poviandgooba, 3, 5.99f, PoviSubscriptiontype.CATEGORY, "Povi and Gooba"));
                subscriptions.add(new PoviSubscription("Summer is Here", R.drawable.summerishere, 3, 5.99f, PoviSubscriptiontype.CATEGORY, "Summer is Here"));
                subscriptions.add(new PoviSubscription("Povi and Gooba travel around the world", R.drawable.aroundtheworld, 12, 5.99f, PoviSubscriptiontype.CATEGORY, "Around The World"));
                subscriptions.add(new PoviSubscription("Weekend Fun", R.drawable.weekendfun, 3, 5.99f, PoviSubscriptiontype.CATEGORY, "Weekend Fun"));

                break;
            case AUTHOR:
                subscriptions.add(new PoviSubscription("Journalist and Entrepreneur, Content Manager at POVI and Owner of Red Basket Personal Chef Certificate in Publicity and Design (High School- CSVP- Brazil), BA in Journalism (Universidade Federal Rio de Janeiro, Brazil) , MA in International Studies (The University of Birmingham, UK),  Personal Chef Certificate (USPCA, US)\n", R.drawable.anna_headshot, 12, 5.99f, PoviSubscriptiontype.AUTHOR, "Anna Muggiati"));
                subscriptions.add(new PoviSubscription("PhD, Developmental Psychology, Cornell University. Daphna loves kids and is a proud aunt! She's also extremely passionate about promoting children’s wellbeing through helping them understand and cope with their emotions.\n", R.drawable.daphna_headshot, 3, 5.99f, PoviSubscriptiontype.AUTHOR, "Daphna Ram"));
                subscriptions.add(new PoviSubscription("Mother of two (teenager and adult).  BA in Sociology, MS in Advertising. Mallika's parenting has improved with experience and she is still learning. All her knowledge comes from reading parenting books and integrating with her childhood - which was in India, at home with extended family (mostly Unstructured and Unsupervised) and at a convent boarding school Super Structured and Supervised!) She was parented in an Authoritarian way (you obeyed the adults) and her parenting style is Authoritative (guide but do not control).\n", R.drawable.mallika_headshot, 3, 5.99f, PoviSubscriptiontype.AUTHOR, "Mallika Sankaran"));
                subscriptions.add(new PoviSubscription("Community Psychology PhD. She is a mother of a 6 year girl and an 8 year old boy. Healthy Social Emotional Development in young children is related to their well-being and is a predictor of later academic, social, and emotional success. Just like kids learn to read and do math, they can and should learn how to, among others, identify and control their emotions, form and maintain relationships, and work through conflicts. By helping our children build social-emotional confidence, we are setting them on a path of becoming happier and healthier adults.\n", R.drawable.olya_headshot, 3, 5.99f, PoviSubscriptiontype.AUTHOR, "Olya Glantsman"));
                subscriptions.add(new PoviSubscription("MBA Babson College, MSEE Clarkson University. Huz loves to spend time with his kids - 8 year old boy and 3 year old girl. Parenting is full of challenges but the rewards he gets when his kids hug him and say that they love him is no comparison to anything else in the world. He takes his talents in creativity into my communications with his children, and loves to also share it with the world.", R.drawable.huz_head, 3, 5.99f, PoviSubscriptiontype.AUTHOR, "Huz Dalal"));
                subscriptions.add(new PoviSubscription("Public School Teacher,  K-8 Public School Counselor, Easterbrook Discovery School, San Jose, CA, BA in Elementary Education, MA in Education, MA in School Counseling. Yonit loves everything about working with children. She loves being able to see children develop and grow with appropriate support and intervention.  It is so rewarding to work with a population that want to feel better and are present to listen, learn, and make positive changes that improve their ability to function in all areas of their life.\n", R.drawable.yonit, 3, 5.99f, PoviSubscriptiontype.AUTHOR, "Yonit Parenti"));
                break;
            case LIBRARY:
                subscriptions.add(new PoviSubscription("Around The World", R.drawable.aroundtheworld, 12, 5.99f, PoviSubscriptiontype.LIBRARY, "Travel Around The World"));
                subscriptions.add(new PoviSubscription("Friendship", R.drawable.friendship, 3, 5.99f, PoviSubscriptiontype.LIBRARY, "Friendship"));
                subscriptions.add(new PoviSubscription("New School", R.drawable.newschool, 3, 5.99f, PoviSubscriptiontype.LIBRARY, "New School"));
                subscriptions.add(new PoviSubscription("Weekend Fun", R.drawable.weekendfun, 3, 5.99f, PoviSubscriptiontype.LIBRARY, "Weekend Fun"));
                subscriptions.add(new PoviSubscription("Summer is Here", R.drawable.summerishere, 3, 5.99f, PoviSubscriptiontype.LIBRARY, "Summer is Here"));
                subscriptions.add(new PoviSubscription("Povi and Gooba", R.drawable.poviandgooba, 3, 5.99f, PoviSubscriptiontype.LIBRARY, "Povi and Gooba"));
                break;
        }

        return subscriptions;
    }
    public static List<Comment> getCommentsShared(ParentingTipId tipId, String token, int start, int count, Long lastCommentId) {
        String COMMENTID = "comment_id";
        TipCommentFindByGetCommentsSharedRequestBuilder tipBuilder = new TipCommentRequestBuilders().findByGetCommentsShared();

        if (lastCommentId != null)
            tipBuilder.lastCommentIdParam(lastCommentId);

        FindRequest<Comment> findRequest = tipBuilder.tipIdParam(tipId).paginate(start, count).addHeader(POVI_AUTHORIZATION_HEADER, token).build();
        ResponseFuture<CollectionResponse<Comment>> getFutureComments = restClient.
                sendRequest(findRequest);

        // Start collecting the results of the response in commentResp
        Response<CollectionResponse<Comment>> commentResp;
        try {
            commentResp = getFutureComments.getResponse();
            CollectionResponse<Comment> response = commentResp.getEntity();
            DataMap dataMap = response.getMetadataRaw();
            return response.getElements();

            //if (dataMap != null && dataMap.containsKey(COMMENTID))
            //    return dataMap.getInteger(COMMENTID);
            //else
            //    return null;
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static PoviStory getStory()
    {
        // TODO: get story and associated follow up questions from server
        populateStories();
        return POVI_STORIES.get(STORY_INDEX);
    }

    public static PoviStory getNextStory(){
        populateStories();
        if(STORY_INDEX == POVI_STORIES.size() - 1){
            STORY_INDEX = 0;
        } else {
            STORY_INDEX++;
        }

        return POVI_STORIES.get(STORY_INDEX);
    }

    public static void populateStories()
    {
        if(STORY_INDEX == -1){
            STORY_INDEX = 0;
            POVI_STORIES = new ArrayList<>();

            String[] questions = new String[3];

            String author = "Daphna Ram";
            String category = "Relationship, Emotion Recognition";
            questions[0] = "How does it feel to be left out of a game?";
            questions[1] = "Is it hard to ask to be included? Why? What should someone do if they can’t be included?";
            questions[2] = "What do you do when things don't go your way? How do you make yourself feel better when things don't go your way?";
            String fullStory = "Today, I was left out of a game of four square on the playground. Even though I tried to be included, the kids said there was no room for me in the game.\n" +
                    "Could you help me figure out how I feel?";
            String response = "I feel sad that I wasn’t included in the game, and maybe a little mad too. I ended up going up to other kids on the playground who were playing tag and asked them if I could play with them, and they let me. I think I’ll try and ask if I can be in the four-square game again tomorrow.";
            String title = "Left Out Of A Game";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            author = "Mallika Sankaran";
            category = "Friendship, Perception Of Others";
            questions = new String[3];
            questions[0] = "What type of friend is Annie?";
            questions[1] = "What if you played with someone else today?";
            questions[2] = "What does your friend Carla say?";
            fullStory = "Annie is always telling us what to do. And she is taller than us so we feel we should listen to her. Yesterday when i didn’t listen to her, she said people like me are bad. What did she mean? Should I tell my mom? Can you help me with how I feel? I don’t want to lose all my friends who are also scared of Annie.";
            response = "I asked Carla if we could play on the slide today as I did not want to play Annie’s game today. She said ok. I promised her that tomorrow we could still try Annie’s game. I am hoping it will be ok.";
            title = "Dealing With The Bossy Friends";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            author = "Anna Muggiati";
            category = "Empathy, Self-Esteem";
            questions = new String[3];
            questions[0] = "Do you think Gubba can be nicer to Povi?";
            questions[1] = "What should their parents do about this?";
            questions[2] = "Can Povi explain that he doesn’t know that he talks while sleeping?";
            fullStory = "Last night I had to share a hotel bed with my older brother Gubba. He says I talked while I was dreaming. He yelled at me to make me stop. I cried out loud and my parents woke up and said he won’t get to use his camera to take photos in Florence! I am scared of sleeping with him again. How can I make him understand that I don’t know I am talking when asleep?";
            response = "After my parents took his camera, Gubba was really upset. I told him it was not my fault if I sleep-talk, and that he could just ignore this and keep sleeping. My mom told him he could use her earplugs. I am hopeful that he understands it’s not my choice to talk in the middle of the night, and he says he’s going to be better tonight.";
            title = "First Night In Italy (The Series Of Povi World Travels)";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            author = "Olya Glantsman";
            category = "Perception Of Others, Critical thinking";
            questions = new String[3];
            questions[0] = "Do you have a favorite toy?";
            questions[1] = "How do you think you would feel if someone broke it? Would it matter if they apologized?";
            questions[2] = "Does it make a difference if that person is someone you love or a member of your family?";
            fullStory = "While I was at a Karate class on Saturday morning, my little sister was playing with my favorite toy and she broke it. When I got home and saw the broken toy, she said she was sorry.\n" +
                    "How do you think I felt? How do you think you would feel if it happened to YOU?\n";
            response = "At first, I was really mad. Why was she touching my stuff without asking me. My dad said it was OK to be angry. Then I remembered that I too have broken some of my sister’s things before. It made me feel a little less mad, but I still wanted to be alone for a while.";
            title = "My Favorite Toy";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            author = "Yonit Parenti";
            category = "Friendship, Perception Of Others";
            questions = new String[3];
            questions[0] = "Do you want Cooper to play nicely with everyone?";
            questions[1] = "What should you say to him when he acts this way?";
            questions[2] = "What else do you think Povi can do?";
            fullStory = "When I play with my good friend Cooper he can make bad choices. Today he knocked over other kids Lego towers and roared like a dinosaur at my friend Grayson. He started to cry. He is usually nice to me. The teacher had to give him a time out. How can I help him be a better friend?";
            response = "After his time out, Cooper was really sad and he sat alone and cried. I went to sit beside him and told him that I still want to be his friend but I think that he should go and say sorry to our other friends.";
            title = "Cooper Can Make Bad Choices";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            author = "Daphna Ram";
            category = "Perception Of Others, Critical Thinking";
            questions = new String[3];
            questions[0] = "What’s fun about going out and doing things with people you like?";
            questions[1] = "What is hard about losing something?";
            questions[2] = "When was the last time you lost something you really liked?";
            fullStory = "I went out for ice cream today with my parents and best friend. I got 3 whole scoops of chocolate ice cream AND had fudge! We were walking and I was almost through my first scoop when I tripped. I was okay, but my ice cream fell!!\n" +
                    "Could you help me figure out how I feel?";
            response = "I was really sad. I even cried. Ice cream is my absolute favorite and I was so upset that I lost it! My mom also told me I had to be more careful and didn’t buy me any more ice cream, but it wasn’t my fault!!! I got really mad at my mom.";
            title = "My Ice Cream Fell";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            category = "Empathy, Relationship";
            questions = new String[3];
            questions[0] = "How can you tell when someone is sad?";
            questions[1] = "Is it important to help people? Why?";
            questions[2] = "Is it sometimes hard to help people? Easy to help people? Why?";
            fullStory = "I was at the library today and I saw this little girl who was crying and did not have her parents around. My mom and I went up to her and asked her what was wrong. She said that she came to the library with her mom and dad but couldn't find them.\n" +
                    "My mom and I took her to talk to the security officer at the library who helped her find her parents.\n";
            response = "I was so scared for her because I know I get really sad when I can’t find my mom and am really scared. I was happy that my mom went up to her to help because I wanted to help but didn’t know how and my mom showed me what to do. I’m proud of myself that I went with my mom to make her feel better!\n" +
                    "I’m glad we stayed with her until she found her parents because I didn’t want her to be lonely.\n";
            title = "A Girl Was Lost In The Library";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            category = "Perspective taking, Perception Of Others";
            questions = new String[3];
            questions[0] = "When is it easy to share? Difficult to share?";
            questions[1] = "What does it mean to be selfish?";
            questions[2] = "Is there such a thing as being \"too nice?\"?";
            fullStory = "My friends and I were talking today at lunch and I saw that one of them got Oreo cookies in his lunch and I didn't. He said he wouldn’t share with me because he likes Oreos too much. But then I looked in my lunch bag and saw that my mom had left me a note that said \"I love you! Hope you're having fun at school!\"\n" +
                    "Could you help me figure out how I feel?\n";
            response = "At first I was mad at my friend because I thought it was nice to share and I didn’t think he was being nice. Then I saw the note from my mom and that made me feel very happy and loved, which also made me less mad at my friend. I thought about it and realized that it’s hard to share sometimes, even with your friend—especially when it’s one of your favorite things! I wish that next time he’d let me have some. I’ll probably share something with him just to show him how nice it is to do that!";
            title = "My Friend Did Not Want To Share";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            category = "Resiliency, Critical Thinking";
            questions = new String[3];
            questions[0] = "How does it feel to try hard and not succeed?";
            questions[1] = "When is it important to do things carefully? Why?";
            questions[2] = "Why do you think you practice things at school?";
            fullStory = "I worked really hard on our math problems today at school and was the first to finish! But then when the teacher went over our answers, it turns out I got some of the answers wrong.\n" +
                    "Could you help me figure out how I feel?\n";
            response = "I’m a little ashamed that I thought I had done so well and then didn’t. I’m also sad too, because I thought I knew more about math. I did make some careless mistakes, so I guess next time I should do the math problems more slowly. There were also some questions I didn’t understand, so I should take more time to study.";
            title = "Did Poorly In A Test";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            category = "Resilience, Emotion Recognition";
            questions = new String[3];
            questions[0] = "What do you do when things don't go your way? How do you make yourself feel better when things don't go your way?";
            questions[1] = "Is it important to feel “special?” Why or why not?";
            questions[2] = "Why do some people like getting attention?";
            fullStory = "I was so excited today to wear my regular clothes for the costume parade because I didn't want to wear any costume. But then I saw 4 boys in my school with the same shirt! Could you help me figure out how I feel? ";
            response = "At first, I was pretty upset because I thought my shirt was so cool and special but it wasn’t. But these other kids came over and asked me if I wanted to join their parade team. I did and we received lots of cheers from our friends.";
            title = "My Costume Is Not Special";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            category = "Perception Of Others, Friendship";
            questions = new String[3];
            questions[0] = "What does it mean to “miss” someone?";
            questions[1] = "Is it possible to feel like someone is a close friend even though you don’t see them every day?";
            questions[2] = "Do you wish you could control what other people do? What would that be like? What would you make them do?";
            fullStory = "Becky has been my best friend ever since we were really really little! But she had to move away today because her mom and dad wanted to. Who will I be friends with now?? Who will I do things with now?? Could you help me figure out how I feel?";
            response = "I was really sad and worried. What if she forgets about me? What if she makes new friends and likes them more?";
            title = "My Best Friend Is Moving Away";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            category = "Perspective Taking, Emotional Recognition";
            questions = new String[3];
            questions[0] = "Would you rather give someone good news or bad news? Why?";
            questions[1] = "Is it possible to be happy for someone else but disappointed in yourself?";
            questions[2] = "What do you do when things don't go your way? How do you make yourself feel better when things don't go your way?";
            fullStory = "Jessica and I both tried out for the school play. Today they put up a list of who made it and Jessica and I both did! Even better, Jessica got the biggest part! She was home sick today though so I get to call her and tell her the good news!";
            response = "I was so happy that we both got in the play! We worked super hard and helped each other practice. Now I get to hang out with Jessica even more because we’ll both be doing rehearsal after school. Maybe next time I’ll get the lead in the play?";
            title = "I Love The School Play";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            category = "Emotion Recognition, Perspective Taking";
            questions = new String[3];
            questions[0] = "How does it feel to be made fun of?";
            questions[1] = "Why do people compare themselves to others? Are there times when it’s a good idea? Times when it’s a bad idea?";
            questions[2] = "Are there times when you’re happy when someone else is sad? Or you’re sad when someone else is happy?";
            fullStory = "Jacob has been super mean to me—telling me I can’t dance, that I’m not good at four-square, that my answers during class are stupid and that I can’t spell. Then today we had track for P.E. and Jacob was last in the sprint in his race.\n" +
                    "Could you tell me how I feel?\n";
            response = "I was excited that Jacob didn’t do well! He deserved it after being so mean to me. Now I get to make fun of him for being bad at something! But I also tell myself that I shouldn’t be like Jacob, making fun of people. What should I do?";
            title = "I Have A Really Mean Friend";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            category = "Friendship, Teamwork";
            questions = new String[3];
            questions[0] = "What does it mean to be ‘fair’ when doing a project?";
            questions[1] = "Do we have to like everything about our friends?";
            questions[2] = "Is there such a thing as being “too helpful” to others?";
            fullStory = "My friend Sam is super cool. We laugh a lot and have a ton of fun playing pretend. At school Sam and I were paired up to do a book report together.  I was really excited! But then I ended up doing almost everything- I read the book and wrote a summary of the plot AND thought about why the book is important. All Sam did was help me draw the picture for the report.\n" +
                    "Could you help me figure out how I feel?\n";
            response = "I was super frustrated and disappointed. I thought Sam was my friend! How come he didn’t help me with our project? I’m never going to work with Sam again. I didn’t tell the teacher because I don’t want Sam to be mad at me though. I did tell Sam I was upset that he didn’t help, but he didn’t think it was a big deal. I am going to find a different partner for my next project.";
            title = "Book Report With Sam";
            POVI_STORIES.add(new PoviStory(author, category, questions, fullStory, response, title));

            category = "";
            questions[0] = "";
            questions[1] = "";
            questions[2] = "";
            fullStory = "";
            response = "";
            title = "";
        }
    }

    public static BeatCommentArray getBeatComments(final String token, final Long commentId) {
        TipCommentDoGetBeatCommentsRequestBuilder tipCommentDoGetBeatCommentsRequestBuilder = new TipCommentRequestBuilders().actionGetBeatComments();
        ActionRequest<BeatCommentArray> actionRequest = tipCommentDoGetBeatCommentsRequestBuilder.commentIdParam(commentId).addHeader(POVI_AUTHORIZATION_HEADER, token).build();

        try{
            ResponseFuture<BeatCommentArray> responseFuture = restClient.sendRequest(actionRequest);
            Response<BeatCommentArray> response = responseFuture.getResponse();

            //System.out.println("\n getBeatComments returns: " + response.getEntity().size());
            return response.getEntity();
        }catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Long addBeatComment(final String token, final Long commentId, final String commenterEmail, final String commentText){
        TipCommentDoAddBeatCommentRequestBuilder tipCommentDoAddBeatCommentRequestBuilder = new TipCommentRequestBuilders().actionAddBeatComment();
        ActionRequest<Long> actionRequest = tipCommentDoAddBeatCommentRequestBuilder.commenterEmailParam(commenterEmail).commentIdParam(commentId).commentTextParam(commentText).addHeader(POVI_AUTHORIZATION_HEADER, token).build();

        try{
            ResponseFuture<Long> responseFuture = restClient.sendRequest(actionRequest);
            Response<Long> response = responseFuture.getResponse();
            //System.out.println("\n addBeatComment returns: " + response.getEntity());
            return response.getEntity();
        }catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteBeatComment(final String token, final Long beatCommentId){
        TipCommentDoDeleteBeatCommentRequestBuilder tipCommentDoDeleteBeatCommentRequestBuilder = new TipCommentRequestBuilders().actionDeleteBeatComment();
        ActionRequest<Boolean> actionRequest = tipCommentDoDeleteBeatCommentRequestBuilder.beatCommentIdParam(beatCommentId).addHeader(POVI_AUTHORIZATION_HEADER, token).build();

        try{
            ResponseFuture<Boolean> responseFuture = restClient.sendRequest(actionRequest);
            Response<Boolean> response = responseFuture.getResponse();
            //System.out.println("\n addBeatComment returns: " + response.getEntity());
            return response.getEntity();
        }catch (RemoteInvocationException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Boolean setLikeStatus(final Long commentId, final Boolean isBeat, final Boolean isLike, final String token){
        TipCommentDoSetLikeStatusRequestBuilder tipCommentDoSetLikeStatusRequestBuilder = new TipCommentRequestBuilders().actionSetLikeStatus();
        ActionRequest<Boolean> actionRequest = tipCommentDoSetLikeStatusRequestBuilder.commentIdParam(commentId).isBeatParam(isBeat).isLikeParam(isLike).addHeader(POVI_AUTHORIZATION_HEADER, token).build();

        try{
            ResponseFuture<Boolean> responseFuture = restClient.sendRequest(actionRequest);
            Response<Boolean> response = responseFuture.getResponse();
            //System.out.println("\n setLikeStatus returns: " + response.getEntity());
            return response.getEntity();
        }catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BeatComments getBeatCommentsWithLikeStatus(final String token, final Long commentId) {
        TipCommentDoGetBeatCommentsWithLikeStatusRequestBuilder tipCommentDoGetBeatCommentsWithLikeStatusRequestBuilder = new TipCommentRequestBuilders().actionGetBeatCommentsWithLikeStatus();
        ActionRequest<BeatComments> actionRequest = tipCommentDoGetBeatCommentsWithLikeStatusRequestBuilder.commentIdParam(commentId).addHeader(POVI_AUTHORIZATION_HEADER, token).build();

        try{
            ResponseFuture<BeatComments> responseFuture = restClient.sendRequest(actionRequest);
            Response<BeatComments> response = responseFuture.getResponse();

            //System.out.println("\n getBeatCommentsWithLikeStatus returns: " + response.getEntity().getBeatComments().size());
            //System.out.println("\n getBeatCommentsWithLikeStatus returns: " + response.getEntity().isLikeStatus());
            //for(BeatComment beatComment : response.getEntity().getBeatComments()){
            //    System.out.println(beatComment);
            //}
            return response.getEntity();
        }catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean createBeat(final String userEmail, final String childName,
                                     final int tipId, final int resourceId, final int contentGroup,
                                     final long timestamp, final String commentText,
                                     final boolean publicStatus, final String token, final String mediaFileName, final String contentDescription, final ByteString thumbnail){

        // Initialize a create comment request builder
        TipCommentCreateRequestBuilder create_requestBuilder =
                new TipCommentRequestBuilders().create();

        String comment;
        if (commentText == null)
            comment = "";
        else
        comment = commentText;

        // Initialize a create request
        CreateIdRequest<ComplexResourceKey<CommentId, CommentId>, Comment> createReq =
                create_requestBuilder.input(new Comment()
                        //.setAuthorNickName()
                        .setChildName(childName)
                                //.setCommentCount()
                                //.setCommentId()
                        .setCommentText(comment)
                        .setContentGroups(contentGroup)
                        .setIsPublic(publicStatus)
                                //.setLikeCount()
                                .setLikeStatus(false)
                                //.setLocalFileName()
                        .setMediaUrl("")
                                //.setRemoteFileName()
                        .setRemoteFileName(mediaFileName, SetMode.IGNORE_NULL)
                        .setResourceId(resourceId)
                        .setTextDescription(contentDescription, SetMode.IGNORE_NULL)
                        .setThumbnailImage(thumbnail, SetMode.IGNORE_NULL)
                        .setTimestamp(timestamp)
                        .setTipId(tipId)
                                .setTipString("")
                        .setUserId(userEmail))
                        .addHeader(RestServer.POVI_AUTHORIZATION_HEADER, token).build();

        // Send the request and wait for a response
        final ResponseFuture<IdResponse<ComplexResourceKey<CommentId, CommentId>>> getFuture =
                restClient.sendRequest(createReq);

        // If you get some response, then the comment has been entered in the table
        final Response<IdResponse<ComplexResourceKey<CommentId, CommentId>>> resp;
        try {
            resp = getFuture.getResponse();
            return true;
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Boolean deleteComment(final String token, final Long commentId){
        TipCommentDoDeleteCommentRequestBuilder tipCommentDoDeleteCommentRequestBuilder = new TipCommentRequestBuilders().actionDeleteComment();
        ActionRequest<Boolean> actionRequest = tipCommentDoDeleteCommentRequestBuilder.commentIdParam(commentId).addHeader(POVI_AUTHORIZATION_HEADER, token).build();

        try{
            ResponseFuture<Boolean> responseFuture = restClient.sendRequest(actionRequest);
            Response<Boolean> response = responseFuture.getResponse();

            //System.out.println("\n deleteComment " + (response.getEntity() ? "succeeded" : "failed"));
            return response.getEntity();
        }catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Comment> getCommentsPagedPerChild(String childName, String token, int start, int count, Long lastCommentId) {
        TipCommentFindByGetCommentsPagedPerChildRequestBuilder tipBuilder = new TipCommentRequestBuilders().findByGetCommentsPagedPerChild();

        if (lastCommentId != null)
            tipBuilder.lastCommentIdParam(lastCommentId);

        FindRequest<Comment> findRequest = tipBuilder.childNameParam(childName).paginate(start, count).addHeader(POVI_AUTHORIZATION_HEADER, token).build();
        //System.out.println("getCommentsShared request: " + findRequest);
        ResponseFuture<CollectionResponse<Comment>> getFutureComments = restClient.
                sendRequest(findRequest);

        // Start collecting the results of the response in commentResp
        Response<CollectionResponse<Comment>> commentResp;
        try {
            commentResp = getFutureComments.getResponse();
            CollectionResponse<Comment> response = commentResp.getEntity();
            return response.getElements();
        } catch (RemoteInvocationException e) {
            e.printStackTrace();
            return null;
        }
    }

}

