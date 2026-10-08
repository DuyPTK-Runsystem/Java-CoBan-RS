package com.JavaTraining.BaiTap_RS.library.catalog.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyBatchRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCopyBatchRequestRepository extends JpaRepository<BookCopyBatchRequest, Long> {

    Optional<BookCopyBatchRequest> findByActorUserIdAndBookIdAndIdempotencyKey(
            Long actorUserId, Long bookId, String idempotencyKey);
}
