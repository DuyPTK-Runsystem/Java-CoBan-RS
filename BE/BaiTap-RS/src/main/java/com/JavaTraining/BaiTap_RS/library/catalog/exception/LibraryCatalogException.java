package com.JavaTraining.BaiTap_RS.library.catalog.exception;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;

public class LibraryCatalogException extends AppException {

    private static final long serialVersionUID = 1L;
    private final String code;

    public LibraryCatalogException(HttpStatus status, String code, String message) {
        super(status, message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
