package com.JavaTraining.BaiTap_RS.library.circulation.exception;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;

public class LibraryCirculationException extends AppException {

    private static final long serialVersionUID = 1L;
    private final String code;

    public LibraryCirculationException(HttpStatus status, String code, String message) {
        super(status, message);
        this.code = code;
    }

    public LibraryCirculationException(HttpStatus status, String code, String message, Throwable cause) {
        super(status, message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
