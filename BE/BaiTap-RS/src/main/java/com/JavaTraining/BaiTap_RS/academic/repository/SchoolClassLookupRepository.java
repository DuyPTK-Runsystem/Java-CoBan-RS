package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.Collection;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolClassLookupRepository {

    @Query("""
            select distinct schoolClass from SchoolClass schoolClass
            join ClassSubject classSubject on classSubject.classId = schoolClass.id
            where classSubject.semesterId = :semesterId
            order by schoolClass.classCode asc
            """)
    List<SchoolClass> findAllBySemesterId(@Param("semesterId") Long semesterId);

    List<SchoolClass> findAllByAcademicYearIdOrderByClassCodeAsc(Long academicYearId);

    List<SchoolClass> findAllByIdInOrderByClassCodeAsc(Collection<Long> ids);

    List<SchoolClass> findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(
            Collection<Long> ids, Long academicYearId);
}
