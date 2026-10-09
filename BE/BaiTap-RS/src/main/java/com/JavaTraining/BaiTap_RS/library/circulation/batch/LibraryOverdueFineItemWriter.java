package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/** The processor persists each item in its own transaction; this writer completes the Spring Batch chunk. */
@Component
public class LibraryOverdueFineItemWriter implements ItemWriter<Long> {

    @Override
    public void write(Chunk<? extends Long> items) {
        // Fine, audit, and notification writes are committed by the idempotent per-item service.
    }
}
