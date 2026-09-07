package com.JavaTraining.BaiTap_RS.student.service;

import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class StudentCodeGenerator {

    private static final int RANDOM_DIGIT_BOUND = 10_000_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateCandidate() {
        int digits = SECURE_RANDOM.nextInt(RANDOM_DIGIT_BOUND);
        return "STU" + String.format("%07d", digits);
    }

    public Set<String> generateCandidates(int count) {
        Set<String> candidates = new LinkedHashSet<>();
        while (candidates.size() < count) {
            candidates.add(generateCandidate());
        }
        return candidates;
    }
}
