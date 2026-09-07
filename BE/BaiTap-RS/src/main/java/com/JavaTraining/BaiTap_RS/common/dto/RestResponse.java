package com.JavaTraining.BaiTap_RS.common.dto;

public class RestResponse<T> {

    private int statusCode;
    private String error;
    private Object message;
    private T data;

    public static <T> RestResponse<T> success(int statusCode, Object message, T data) {
        RestResponse<T> response = new RestResponse<>();
        response.setStatusCode(statusCode);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    public static RestResponse<Void> failure(int statusCode, String error, Object message) {
        RestResponse<Void> response = new RestResponse<>();
        response.setStatusCode(statusCode);
        response.setError(error);
        response.setMessage(message);
        return response;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
