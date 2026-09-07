package com.JavaTraining.BaiTap_RS.student.service;

import java.text.Normalizer;
import java.util.Locale;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StudentUsernameGenerator {

    private static final int STUDENT_CODE_SUFFIX_LENGTH = 7;
    private static final int MAX_USERNAME_LENGTH = 20;

    public String generate(String studentName, String studentCode) {
        String normalizedName = normalize(studentName);
        String suffix = studentCode.substring(studentCode.length() - STUDENT_CODE_SUFFIX_LENGTH);
        if (normalizedName.length() + suffix.length() > MAX_USERNAME_LENGTH) {
            normalizedName = abbreviate(studentName);
        }
        if (normalizedName.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể tạo tên đăng nhập từ tên sinh viên");
        }
        int maxNameLength = MAX_USERNAME_LENGTH - suffix.length();
        return normalizedName.substring(0, Math.min(normalizedName.length(), maxNameLength)) + suffix;
    }

    private String abbreviate(String studentName) {
        StringBuilder initials = new StringBuilder();
        for (String word : studentName.trim().split("\\s+")) {
            String normalizedWord = normalize(word);
            if (!normalizedWord.isEmpty()) {
                initials.append(normalizedWord.charAt(0));
            }
        }
        return initials.toString();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace("đ", "d")
                .replace("Đ", "D")
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
