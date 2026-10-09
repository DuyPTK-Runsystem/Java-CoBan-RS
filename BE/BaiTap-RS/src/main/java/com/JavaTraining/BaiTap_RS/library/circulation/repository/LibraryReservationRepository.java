package com.JavaTraining.BaiTap_RS.library.circulation.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryReservationRepository extends JpaRepository<LibraryReservation, Long> {

    String PATRON_ID_PARAMETER = "patronId";
    String STATUS_PARAMETER = "status";

    interface ReservationLockInfo {

        Long getBookId();

        Long getPatronId();

        Long getAllocatedCopyId();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LibraryReservation r where r.patronId = :patronId and r.bookId = :bookId "
            + "and r.status in :statuses order by r.id")
    List<LibraryReservation> findActiveByPatronAndBookForUpdate(@Param(PATRON_ID_PARAMETER) Long patronId,
            @Param("bookId") Long bookId, @Param("statuses") List<ReservationStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LibraryReservation r where r.bookId = :bookId and r.status = :status "
            + "and r.pickupDueAt <= :pickupDueAt order by r.id")
    List<LibraryReservation> findExpiredByBookForUpdate(@Param("bookId") Long bookId,
            @Param(STATUS_PARAMETER) ReservationStatus status,
            @Param("pickupDueAt") java.time.LocalDateTime pickupDueAt);

    @Query("select r from LibraryReservation r where r.bookId = :bookId and r.status = :status "
            + "order by r.policySnapshot.reservedAt, r.id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<LibraryReservation> findQueue(@Param("bookId") Long bookId, @Param(STATUS_PARAMETER) ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LibraryReservation r where r.patronId = :patronId "
            + "and r.allocatedCopyId = :copyId and r.status = :status")
    Optional<LibraryReservation> findReadyByPatronAndCopyForUpdate(@Param(PATRON_ID_PARAMETER) Long patronId,
            @Param("copyId") Long copyId, @Param(STATUS_PARAMETER) ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LibraryReservation r where r.id = :id")
    Optional<LibraryReservation> findByIdForUpdate(@Param("id") Long id);

    @Query("select r.bookId as bookId, r.patronId as patronId, r.allocatedCopyId as allocatedCopyId "
            + "from LibraryReservation r where r.id = :id")
    Optional<ReservationLockInfo> findLockInfoById(@Param("id") Long id);

    @Query("select distinct r.bookId from LibraryReservation r where r.status = :status "
            + "and r.pickupDueAt <= :pickupDueAt")
    List<Long> findExpiredBookIds(@Param(STATUS_PARAMETER) ReservationStatus status,
            @Param("pickupDueAt") java.time.LocalDateTime pickupDueAt);

    @Query("select r from LibraryReservation r where r.patronId = :patronId "
            + "and (:status is null or r.status = :status)")
    Page<LibraryReservation> pageForPatron(@Param(PATRON_ID_PARAMETER) Long patronId,
            @Param(STATUS_PARAMETER) ReservationStatus status, Pageable pageable);

    @Query("select r from LibraryReservation r where (:patronId is null or r.patronId = :patronId) "
            + "and (:status is null or r.status = :status)")
    Page<LibraryReservation> pageForStaff(@Param(PATRON_ID_PARAMETER) Long patronId,
            @Param(STATUS_PARAMETER) ReservationStatus status, Pageable pageable);
}
