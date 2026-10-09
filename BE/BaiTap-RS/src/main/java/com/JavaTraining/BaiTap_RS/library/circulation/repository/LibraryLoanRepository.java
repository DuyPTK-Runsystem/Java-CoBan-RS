package com.JavaTraining.BaiTap_RS.library.circulation.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryLoanRepository extends JpaRepository<LibraryLoan, Long> {

    String STATUS_PARAMETER = "status";

    interface LoanLockInfo {

        Long getCopyId();

        Long getPatronId();
    }

    long countByPatronIdAndStatus(Long patronId, LoanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LibraryLoan l where l.patronId = :patronId and l.status = :status order by l.id")
    List<LibraryLoan> findAllByPatronIdAndStatusForUpdate(@Param("patronId") Long patronId,
            @Param(STATUS_PARAMETER) LoanStatus status);

    Optional<LibraryLoan> findFirstByCopyIdAndStatus(Long copyId, LoanStatus status);

    List<LibraryLoan> findAllByCopyIdInAndStatus(List<Long> copyIds, LoanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LibraryLoan l where l.id = :id")
    Optional<LibraryLoan> findByIdForUpdate(@Param("id") Long id);

    @Query("select l.copyId as copyId, l.patronId as patronId from LibraryLoan l where l.id = :id")
    Optional<LoanLockInfo> findLockInfoById(@Param("id") Long id);

    @Query("select l from LibraryLoan l where l.patronId = :patronId and (:status is null or l.status = :status)")
    Page<LibraryLoan> pageForPatron(@Param("patronId") Long patronId,
            @Param(STATUS_PARAMETER) LoanStatus status, Pageable pageable);

    @Query("select l from LibraryLoan l where (:patronId is null or l.patronId = :patronId) "
            + "and (:status is null or l.status = :status)")
    Page<LibraryLoan> pageForStaff(@Param("patronId") Long patronId,
            @Param(STATUS_PARAMETER) LoanStatus status, Pageable pageable);

    @Query("select l from LibraryLoan l where l.status = :status and l.dueAt < :dueAt order by l.id")
    Page<LibraryLoan> findOverdue(@Param(STATUS_PARAMETER) LoanStatus status,
            @Param("dueAt") java.time.LocalDateTime dueAt, Pageable pageable);
}
