package com.JavaTraining.BaiTap_RS.library.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    Optional<BookCopy> findByBarcode(String barcode);

    @Query("select c.book.id from BookCopy c where c.barcode = :barcode")
    Optional<Long> findBookIdByBarcode(@Param("barcode") String barcode);

    @org.springframework.data.jpa.repository.Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.barcode = :barcode")
    Optional<BookCopy> findByBarcodeForUpdate(@Param("barcode") String barcode);

    long countByBookId(Long bookId);

    long countByBookIdAndStatusAndReferenceOnlyFalse(Long bookId, BookCopyStatus status);

    boolean existsByBookIdAndStatusIn(Long bookId, List<BookCopyStatus> statuses);

    @Query("select c.book.id, count(c) from BookCopy c where c.book.id in :bookIds group by c.book.id")
    List<Object[]> countCopiesByBookIds(@Param("bookIds") List<Long> bookIds);

    @Query("select c.book.id, count(c) from BookCopy c where c.book.id in :bookIds "
            + "and c.status = :status and c.referenceOnly = false group by c.book.id")
    List<Object[]> countAvailableByBookIds(@Param("bookIds") List<Long> bookIds,
            @Param("status") BookCopyStatus status);

    @Query("select c from BookCopy c where c.book.id = :bookId "
            + "and (:status is null or c.status = :status) "
            + "and (:referenceOnly is null or c.referenceOnly = :referenceOnly)")
    Page<BookCopy> searchByBookId(@Param("bookId") Long bookId,
            @Param("status") BookCopyStatus status,
            @Param("referenceOnly") Boolean referenceOnly, Pageable pageable);
}
