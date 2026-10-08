package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.net.URI;
import java.util.Locale;

import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import org.springframework.http.HttpStatus;

final class BookValidation {

    private BookValidation() {
    }

    /* default */ static String normalizeIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }
        String normalized = isbn.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        boolean valid = normalized.length() == 10 ? validIsbn10(normalized)
                : normalized.length() == 13 && asciiDigits(normalized, 0, 13) && validIsbn13(normalized);
        if (!valid) {
            throw new LibraryCatalogException(HttpStatus.BAD_REQUEST, "INVALID_ISBN", "ISBN checksum is invalid");
        }
        return normalized;
    }

    /* default */ static String normalizeText(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    /* default */ static String validateCoverUrl(String value) {
        String url = normalizeText(value);
        if (url == null) {
            return null;
        }
        boolean validUrl;
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            validUrl = uri.isAbsolute() && uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException exception) {
            validUrl = false;
        }
        if (validUrl) {
            return url;
        }
        throw new LibraryCatalogException(HttpStatus.BAD_REQUEST, "INVALID_COVER_URL",
                "Cover URL must be an absolute HTTP or HTTPS URL");
    }

    private static boolean validIsbn10(String value) {
        int sum = 0;
        for (int index = 0; index < 10; index++) {
            char digit = value.charAt(index);
            int number = digit == 'X' && index == 9 ? 10 : asciiDigit(digit);
            if (number < 0) {
                return false;
            }
            sum += number * (10 - index);
        }
        return sum % 11 == 0;
    }

    private static boolean validIsbn13(String value) {
        int sum = 0;
        for (int index = 0; index < 12; index++) {
            sum += asciiDigit(value.charAt(index)) * (index % 2 == 0 ? 1 : 3);
        }
        return (10 - sum % 10) % 10 == asciiDigit(value.charAt(12));
    }

    private static boolean asciiDigits(String value, int start, int end) {
        for (int index = start; index < end; index++) {
            if (asciiDigit(value.charAt(index)) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int asciiDigit(char value) {
        return value >= '0' && value <= '9' ? value - '0' : -1;
    }
}
