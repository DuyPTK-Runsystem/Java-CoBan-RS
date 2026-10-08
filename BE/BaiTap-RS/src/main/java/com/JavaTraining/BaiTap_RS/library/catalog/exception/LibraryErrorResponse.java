package com.JavaTraining.BaiTap_RS.library.catalog.exception;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LibraryErrorResponse(int statusCode, String error, String code, Object message,
        Map<String, String> fieldErrors) {

    @JsonProperty("data")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public Object data() {
        return null;
    }
}
