package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationResultSource;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:transcript-result-repository;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class StudentSubjectResultRepositoryTest {

    @Autowired
    private StudentSubjectTermResultRepository termResultRepository;

    @Autowired
    private StudentSubjectAnnualResultRepository annualResultRepository;

    @Autowired
    private StudentAnnualTranscriptRepository annualTranscriptRepository;

    @Autowired
    private StudentTermTranscriptRepository termTranscriptRepository;

    @Test
    void persistsTermResultAndSupportsTranscriptAndSubjectQueries() {
        StudentSubjectTermResult result = new StudentSubjectTermResult(10L, 20L, 30L, SubjectType.ACADEMIC);
        result.setDtbmh(new BigDecimal("8.5"));
        result.setCalculatedVersion(4L);
        result.setCalculatedAt(LocalDateTime.now());

        StudentSubjectTermResult saved = termResultRepository.saveAndFlush(result);

        assertNotNull(saved.getId(), "term result id should be generated");
        assertNotNull(saved.getCreatedAt(), "term result created timestamp should be populated");
        assertNotNull(saved.getUpdatedAt(), "term result updated timestamp should be populated");
        assertEquals(new BigDecimal("8.5"), saved.getDtbmh(), "term result score should persist");
        StudentSubjectTermResult found = termResultRepository
                .findByTermTranscriptIdAndSubjectId(10L, 30L)
                .orElseThrow();
        assertEquals(saved.getId(), found.getId(), "term query should return the persisted result");
        assertEquals(saved.getDtbmh(), found.getDtbmh(), "term query should preserve the score");
        assertEquals(1, termResultRepository.findAllByTermTranscriptIdOrderBySubjectIdAsc(10L).size(),
                "transcript query should return one result");
        assertEquals(1, termResultRepository.findAllBySubjectIdOrderByTermTranscriptIdAsc(30L).size(),
                "subject query should return one result");
    }

    @Test
    void persistsAnnualResultWithRetakeSourceAndTermResultReferences() {
        StudentSubjectAnnualResult result = new StudentSubjectAnnualResult(11L, 31L, SubjectType.ACADEMIC);
        result.setHk1TermResultId(101L);
        result.setHk2TermResultId(102L);
        result.setRetakeId(103L);
        result.setRegularDtbmhCn(new BigDecimal("6.5"));
        result.setOfficialDtbmhCn(new BigDecimal("7.5"));
        result.setCalculationSource(CalculationResultSource.RETAKE);
        result.setCalculatedVersion(5L);
        result.setNote("Retake applied");

        StudentSubjectAnnualResult saved = annualResultRepository.saveAndFlush(result);

        assertNotNull(saved.getId(), "annual result id should be generated");
        assertEquals(CalculationResultSource.RETAKE, saved.getCalculationSource(),
                "annual result source should persist");
        assertEquals(101L, saved.getHk1TermResultId(), "HK1 result reference should persist");
        assertEquals(102L, saved.getHk2TermResultId(), "HK2 result reference should persist");
        StudentSubjectAnnualResult found = annualResultRepository
                .findByAnnualTranscriptIdAndSubjectId(11L, 31L)
                .orElseThrow();
        assertEquals(saved.getId(), found.getId(), "annual query should return the persisted result");
        assertEquals(saved.getCalculationSource(), found.getCalculationSource(),
                "annual query should preserve the calculation source");
        assertEquals(1, annualResultRepository.findAllByAnnualTranscriptIdOrderBySubjectIdAsc(11L).size(),
                "transcript query should return one annual result");
        assertEquals(1, annualResultRepository.findAllBySubjectIdOrderByAnnualTranscriptIdAsc(31L).size(),
                "subject query should return one annual result");
    }

    @Test
    void rejectsDuplicateTermResultForTranscriptAndSubject() {
        termResultRepository.saveAndFlush(new StudentSubjectTermResult(12L, 22L, 32L, SubjectType.SKILL));
        StudentSubjectTermResult duplicate = new StudentSubjectTermResult(12L, 23L, 32L, SubjectType.SKILL);

        assertThrows(DataIntegrityViolationException.class, () -> termResultRepository.saveAndFlush(duplicate),
                "duplicate term transcript/subject must be rejected");
    }

    @Test
    void rejectsDuplicateAnnualResultForTranscriptAndSubject() {
        annualResultRepository.saveAndFlush(new StudentSubjectAnnualResult(13L, 33L, SubjectType.ACADEMIC));
        StudentSubjectAnnualResult duplicate = new StudentSubjectAnnualResult(13L, 33L, SubjectType.ACADEMIC);

        assertThrows(DataIntegrityViolationException.class, () -> annualResultRepository.saveAndFlush(duplicate),
                "duplicate annual transcript/subject must be rejected");
    }

    @Test
    void persistsNewTranscriptFieldsAndUpdatesLifecycleTimestamp() {
        StudentAnnualTranscript annual = new StudentAnnualTranscript(1L, 2L);
        annual.setRegularDtbcn(new BigDecimal("7.0"));
        annual.setFinalDtbcn(new BigDecimal("7.5"));
        annual.setResultSource(CalculationResultSource.RETAKE);
        annual.setLastCalculationTaskId(3L);
        StudentAnnualTranscript savedAnnual = annualTranscriptRepository.saveAndFlush(annual);

        assertEquals(new BigDecimal("7.0"), savedAnnual.getRegularDtbcn(), "regular annual score should persist");
        assertEquals(new BigDecimal("7.5"), savedAnnual.getFinalDtbcn(), "final annual score should persist");
        assertEquals(CalculationResultSource.RETAKE, savedAnnual.getResultSource(),
                "annual result source should persist");
        assertEquals(3L, savedAnnual.getLastCalculationTaskId(),
                "last calculation task reference should persist");

        LocalDateTime initialUpdatedAt = savedAnnual.getUpdatedAt();
        savedAnnual.setFinalDtbcn(new BigDecimal("7.6"));
        savedAnnual.setUpdatedAt(LocalDateTime.MIN);
        StudentAnnualTranscript updatedAnnual = annualTranscriptRepository.saveAndFlush(savedAnnual);
        assertTrue(updatedAnnual.getUpdatedAt().isAfter(initialUpdatedAt),
                "pre-update lifecycle should refresh updated timestamp");

        StudentTermTranscript term = new StudentTermTranscript(4L, 5L, 1L);
        term.setDtbhk(new BigDecimal("8.0"));
        term.setCalculationStatus(CalculationStatus.FINISH);
        StudentTermTranscript savedTerm = termTranscriptRepository.saveAndFlush(term);

        assertEquals(new BigDecimal("8.0"), savedTerm.getDtbhk(), "term average score should persist");
        assertNotNull(savedTerm.getCreatedAt(), "term transcript created timestamp should be populated");
        assertNotNull(savedTerm.getUpdatedAt(), "term transcript updated timestamp should be populated");
    }
}
