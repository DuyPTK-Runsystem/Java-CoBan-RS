package com.JavaTraining.BaiTap_RS.library.card.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryCardRepository extends JpaRepository<LibraryCard, Long> {

    Optional<LibraryCard> findFirstByPatronIdAndStatusOrderByIssuedAtDesc(Long patronId, LibraryCardStatus status);

    List<LibraryCard> findAllByPatronIdInAndStatusOrderByIssuedAtDesc(Collection<Long> patronIds,
            LibraryCardStatus status);

    List<LibraryCard> findAllByPatronIdOrderByIssuedAtDesc(Long patronId);

    Optional<LibraryCard> findByCardNo(String cardNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LibraryCard c where c.cardNo = :cardNo")
    Optional<LibraryCard> findByCardNoForUpdate(@Param("cardNo") String cardNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LibraryCard c where c.patronId = :patronId and c.status = :status")
    List<LibraryCard> findByPatronIdAndStatusForUpdate(@Param("patronId") Long patronId,
            @Param("status") LibraryCardStatus status);
}
