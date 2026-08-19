package com.JavaTraining.BaiTap_RS.batch.studentcsv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class StudentCsvExportResultStore {

    private final ConcurrentMap<Long, byte[]> results = new ConcurrentHashMap<>();

    public void store(long executionId, byte[] content) {
        results.put(executionId, content);
    }

    public byte[] take(long executionId) {
        return results.remove(executionId);
    }

    public void discard(long executionId) {
        results.remove(executionId);
    }
}
