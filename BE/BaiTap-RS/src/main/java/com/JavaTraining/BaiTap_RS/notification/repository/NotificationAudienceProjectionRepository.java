package com.JavaTraining.BaiTap_RS.notification.repository;

import java.util.Collection;
import java.util.List;

import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only projections used by the manager audience selector and audience guards. */
public interface NotificationAudienceProjectionRepository extends Repository<User, Long> {

    @Query(value = """
            select c.id as classId,
                   c.classCode as classCode,
                   c.className as className,
                   c.academicYearId as academicYearId,
                   (select count(distinct eligibleUser.id)
                      from StudentYearEnrollment enrollment,
                           Student student,
                           User eligibleUser
                     where enrollment.currentClassId = c.id
                       and enrollment.academicYearId = c.academicYearId
                       and enrollment.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
                       and student.id = enrollment.studentId
                       and student.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
                       and student.userId = eligibleUser.id) as eligibleRecipientCount
              from SchoolClass c, AcademicYear year
             where year.id = c.academicYearId
               and c.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
               and year.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
               and exists (select enrollment.id
                             from StudentYearEnrollment enrollment,
                                  Student student,
                                  User eligibleUser
                            where enrollment.currentClassId = c.id
                              and enrollment.academicYearId = c.academicYearId
                              and enrollment.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
                              and student.id = enrollment.studentId
                              and student.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
                              and student.userId = eligibleUser.id)
               and (:query is null
                    or lower(c.classCode) like lower(concat('%', :query, '%'))
                    or lower(coalesce(c.className, '')) like lower(concat('%', :query, '%')))
             order by c.classCode asc, c.id asc
            """,
            countQuery = """
            select count(c.id)
              from SchoolClass c, AcademicYear year
             where year.id = c.academicYearId
               and c.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
               and year.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
               and exists (select enrollment.id
                             from StudentYearEnrollment enrollment,
                                  Student student,
                                  User eligibleUser
                            where enrollment.currentClassId = c.id
                              and enrollment.academicYearId = c.academicYearId
                              and enrollment.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
                              and student.id = enrollment.studentId
                              and student.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
                              and student.userId = eligibleUser.id)
               and (:query is null
                    or lower(c.classCode) like lower(concat('%', :query, '%'))
                    or lower(coalesce(c.className, '')) like lower(concat('%', :query, '%')))
            """)
    Page<ClassAudienceProjection> findEligibleClasses(@Param("query") String query, Pageable pageable);

    @Query("""
            select distinct u.id
              from User u
              left join Student student on student.userId = u.id
             where u.id in :userIds
               and (student.id is null
                    or student.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE)
            """)
    List<Long> findEligibleUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("""
            select distinct eligibleUser.id
              from StudentYearEnrollment enrollment,
                   Student student,
                   User eligibleUser,
                   SchoolClass schoolClass,
                   AcademicYear year
             where schoolClass.id = :classId
               and year.id = schoolClass.academicYearId
               and schoolClass.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
               and year.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
               and enrollment.currentClassId = schoolClass.id
               and enrollment.academicYearId = schoolClass.academicYearId
               and enrollment.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
               and student.id = enrollment.studentId
               and student.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
               and student.userId = eligibleUser.id
             order by eligibleUser.id asc
            """)
    List<Long> findEligibleUserIdsForClass(@Param("classId") Long classId);

    @Query("""
            select case when count(schoolClass.id) > 0 then true else false end
              from SchoolClass schoolClass, AcademicYear year
             where schoolClass.id = :classId
               and year.id = schoolClass.academicYearId
               and schoolClass.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
               and year.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
               and exists (select enrollment.id
                             from StudentYearEnrollment enrollment,
                                  Student student,
                                  User eligibleUser
                            where enrollment.currentClassId = schoolClass.id
                              and enrollment.academicYearId = schoolClass.academicYearId
                              and enrollment.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
                              and student.id = enrollment.studentId
                              and student.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
                              and student.userId = eligibleUser.id)
            """)
    boolean existsEligibleClass(@Param("classId") Long classId);

    interface ClassAudienceProjection {
        Long getClassId();

        String getClassCode();

        String getClassName();

        Long getAcademicYearId();

        Long getEligibleRecipientCount();
    }
}
