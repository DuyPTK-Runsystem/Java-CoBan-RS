package com.JavaTraining.BaiTap_RS.library.circulation.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineType;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryFine;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryFineRepository extends JpaRepository<LibraryFine, Long> {

    Optional<LibraryFine> findByLoanIdAndType(Long loanId, FineType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from LibraryFine f where f.id = :id")
    Optional<LibraryFine> findByIdForUpdate(@Param("id") Long id);

    @Query("select f from LibraryFine f join LibraryLoan l on l.id = f.loanId "
            + "where (:patronId is null or l.patronId = :patronId) "
            + "and (:status is null or f.status = :status)")
    Page<LibraryFine> pageForStaff(@Param("patronId") Long patronId,
            @Param("status") FineStatus status, Pageable pageable);

    @Query("select f from LibraryFine f join LibraryLoan l on l.id = f.loanId "
            + "where l.patronId = :patronId and (:status is null or f.status = :status)")
    Page<LibraryFine> pageForPatron(@Param("patronId") Long patronId,
            @Param("status") FineStatus status, Pageable pageable);

    @Query("select coalesce(sum(f.amount), 0) from LibraryFine f join LibraryLoan l on l.id = f.loanId "
            + "where l.patronId = :patronId and f.status = :status")
    java.math.BigDecimal sumByPatronAndStatus(@Param("patronId") Long patronId,
            @Param("status") FineStatus status);
}
