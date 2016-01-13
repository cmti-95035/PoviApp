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

