package com.JavaTraining.BaiTap_RS.library.card.service;

import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryCardNumberService {

    private final EntityManager entityManager;

    @Transactional
    public String nextCardNo(int year) {
        entityManager.createNativeQuery("""
                INSERT INTO library_card_sequence (sequence_year, sequence_value)
                VALUES (?1, 0)
                ON DUPLICATE KEY UPDATE sequence_value = sequence_value
                """).setParameter(1, year).executeUpdate();
        Number current = (Number) entityManager.createNativeQuery(
                "SELECT sequence_value FROM library_card_sequence WHERE sequence_year = ?1 FOR UPDATE")
                .setParameter(1, year).getSingleResult();
        int next = current.intValue() + 1;
        if (next > 999_999) {
            throw new LibraryPatronException(HttpStatus.CONFLICT, "CARD_SEQUENCE_EXHAUSTED",
                    "Đã hết số thẻ trong năm");
        }
        entityManager.createNativeQuery("UPDATE library_card_sequence SET sequence_value = ?1 WHERE sequence_year = ?2")
                .setParameter(1, next).setParameter(2, year).executeUpdate();
        return "LC-" + year + "-" + String.format(java.util.Locale.ROOT, "%06d", next);
    }
}
