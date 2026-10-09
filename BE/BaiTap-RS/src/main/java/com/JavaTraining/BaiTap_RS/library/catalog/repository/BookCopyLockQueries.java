package com.JavaTraining.BaiTap_RS.library.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookCopyLockQueries {

    @Query("select c.book.id from BookCopy c where c.barcode = :barcode")
    Optional<Long> findBookIdByBarcode(@Param("barcode") String barcode);

    @Query("select c.book.id from BookCopy c where c.id = :copyId")
    Optional<Long> findBookIdById(@Param("copyId") Long copyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.barcode in :barcodes order by c.id")
    List<BookCopy> findAllByBarcodesForUpdate(@Param("barcodes") List<String> barcodes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.id = :id")
    Optional<BookCopy> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.barcode = :barcode")
    Optional<BookCopy> findByBarcodeForUpdate(@Param("barcode") String barcode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.book.id = :bookId and c.status = :status "
            + "and c.referenceOnly = false order by c.id")
    List<BookCopy> findAvailableCopiesForUpdate(@Param("bookId") Long bookId,
            @Param("status") BookCopyStatus status);
}
