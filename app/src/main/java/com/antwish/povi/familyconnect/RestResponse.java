package com.antwish.povi.familyconnect;

/**
 * Created by jianli on 7/23/15.
 */
public class RestResponse<K> {
    private int statusCode;
    private String errorMsg;
    private K entity;

    public RestResponse(int statusCode, String errorMsg, K entity) {
        this.statusCode = statusCode;
        this.errorMsg = errorMsg;
        this.entity = entity;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public K getEntity() {
        return entity;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public void setEntity(K entity) {
        this.entity = entity;
    }
}
