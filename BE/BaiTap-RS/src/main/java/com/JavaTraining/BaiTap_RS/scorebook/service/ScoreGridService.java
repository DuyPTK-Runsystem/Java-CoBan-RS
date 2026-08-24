package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreGridDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service chuyên biệt đọc dữ liệu bảng điểm (score grid) có phân trang.
 */
@Service
public class ScoreGridService {

    private final ScoreEntryContext entryContext;
    private final ScorebookGuard guard;
    private final ScoreGridLoader gridLoader;

    public ScoreGridService(
            ScoreEntryContext entryContext,
            ScorebookGuard guard,
            ScoreGridLoader gridLoader) {
        this.entryContext = entryContext;
        this.guard = guard;
        this.gridLoader = gridLoader;
    }

    @Transactional(readOnly = true)
    public ResStudentScoreGridDTO getScoreGrid(Long scorebookId, int page, int size) {
        Scorebook scorebook = entryContext.findWritableScorebook(scorebookId);
        guard.assertCanRead(scorebook);

        ClassSubject classSubject = entryContext.findClassSubject(scorebook.getClassSubjectId());
        Pageable pageable = PageRequest.of(page, size, Sort.by("studentId").ascending());

        return gridLoader.loadGrid(
                scorebook, classSubject.getClassId(), classSubject.getSemesterId(), pageable);
    }
}
