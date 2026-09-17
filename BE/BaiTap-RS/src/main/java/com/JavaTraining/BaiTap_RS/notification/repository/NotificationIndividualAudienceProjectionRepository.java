package com.JavaTraining.BaiTap_RS.notification.repository;

import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only paged projections; credentials and assignment history stay out of the API. */
public interface NotificationIndividualAudienceProjectionRepository extends Repository<User, Long> {

    @Query(value = """
            select distinct u.id as userId,
                   coalesce(student.studentName, teacher.teacherName, u.username) as displayName,
                   student.studentCode as studentCode,
                   u.username as username
              from User u
              left join Student student on student.userId = u.id
              left join Teacher teacher on teacher.userId = u.id
              left join u.roles role
             where (student.id is null
                    or student.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE)
               and (:roleCode is null or role.code = :roleCode)
               and (:studentClassId is null or exists (
                    select enrollment.id from StudentYearEnrollment enrollment, AcademicYear studentYear
                     where enrollment.studentId = student.id and enrollment.currentClassId = :studentClassId
                       and enrollment.academicYearId = studentYear.id
                       and enrollment.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
                       and studentYear.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE))
               and (:teacherClassId is null or exists (
                    select homeroom.id from HomeroomAssignment homeroom, SchoolClass homeroomClass,
                           AcademicYear homeroomYear
                     where homeroom.teacherId = teacher.id and homeroom.classId = :teacherClassId
                       and homeroom.classId = homeroomClass.id
                       and homeroomClass.academicYearId = homeroomYear.id
                       and homeroom.status = com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus.ACTIVE
                       and homeroomClass.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
                       and homeroomYear.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE)
                    or exists (
                    select teaching.id from SubjectTeachingAssignment teaching, ClassSubject classSubject,
                           SchoolClass teachingClass, AcademicYear teachingYear
                     where teaching.teacherId = teacher.id and teaching.classSubjectId = classSubject.id
                       and classSubject.classId = :teacherClassId and classSubject.classId = teachingClass.id
                       and teachingClass.academicYearId = teachingYear.id
                       and teaching.status = com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus.ACTIVE
                       and teachingClass.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
                       and teachingYear.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE))
               and (:query is null
                    or lower(coalesce(student.studentCode, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(student.studentName, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(teacher.teacherCode, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(teacher.teacherName, '')) like lower(concat('%', :query, '%'))
                    or lower(u.username) like lower(concat('%', :query, '%')))
             order by coalesce(student.studentName, teacher.teacherName, u.username) asc, u.id asc
            """,
            countQuery = """
            select count(distinct u.id)
              from User u
              left join Student student on student.userId = u.id
              left join Teacher teacher on teacher.userId = u.id
              left join u.roles role
             where (student.id is null
                    or student.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE)
               and (:roleCode is null or role.code = :roleCode)
               and (:studentClassId is null or exists (
                    select enrollment.id from StudentYearEnrollment enrollment, AcademicYear studentYear
                     where enrollment.studentId = student.id and enrollment.currentClassId = :studentClassId
                       and enrollment.academicYearId = studentYear.id
                       and enrollment.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
                       and studentYear.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE))
               and (:teacherClassId is null or exists (
                    select homeroom.id from HomeroomAssignment homeroom, SchoolClass homeroomClass,
                           AcademicYear homeroomYear
                     where homeroom.teacherId = teacher.id and homeroom.classId = :teacherClassId
                       and homeroom.classId = homeroomClass.id and homeroomClass.academicYearId = homeroomYear.id
                       and homeroom.status = com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus.ACTIVE
                       and homeroomClass.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
                       and homeroomYear.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE)
                    or exists (
                    select teaching.id from SubjectTeachingAssignment teaching, ClassSubject classSubject,
                           SchoolClass teachingClass, AcademicYear teachingYear
                     where teaching.teacherId = teacher.id and teaching.classSubjectId = classSubject.id
                       and classSubject.classId = :teacherClassId and classSubject.classId = teachingClass.id
                       and teachingClass.academicYearId = teachingYear.id
                       and teaching.status = com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus.ACTIVE
                       and teachingClass.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
                       and teachingYear.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE))
               and (:query is null
                    or lower(coalesce(student.studentCode, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(student.studentName, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(teacher.teacherCode, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(teacher.teacherName, '')) like lower(concat('%', :query, '%'))
                    or lower(u.username) like lower(concat('%', :query, '%')))
            """)
    Page<IndividualAudienceProjection> findEligibleIndividuals(
            @Param("query") String query,
            @Param("roleCode") String roleCode,
            @Param("studentClassId") Long studentClassId,
            @Param("teacherClassId") Long teacherClassId,
            Pageable pageable);

    @Query("""
            select u.id as userId, role.code as roleCode
              from User u join u.roles role
             where u.id in :roleUserIds
               and role.code in ('STUDENT', 'TEACHER', 'ACADEMIC_OFFICE', 'ADMIN')
             order by u.id asc, role.code asc
            """)
    List<UserRoleProjection> findRoleCodesByUserIds(@Param("roleUserIds") Collection<Long> userIds);

    @Query("""
            select distinct u.id as userId,
                   coalesce(student.studentName, teacher.teacherName, u.username) as displayName
              from User u
              left join Student student on student.userId = u.id
              left join Teacher teacher on teacher.userId = u.id
             where u.id in :userIds
             order by u.id asc
            """)
    List<IndividualDisplayNameProjection> findDisplayNamesByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("""
            select u.id as userId, c.id as classId, c.classCode as classCode, c.className as className
              from User u, Student student, StudentYearEnrollment enrollment, SchoolClass c, AcademicYear year
             where u.id in :studentUserIds and student.userId = u.id and enrollment.studentId = student.id
               and enrollment.currentClassId = c.id and enrollment.academicYearId = year.id
               and student.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
               and enrollment.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
               and year.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
             order by u.id asc, c.id asc
            """)
    List<ClassContextProjection> findStudentClassContextsByUserIds(
            @Param("studentUserIds") Collection<Long> userIds);

    @Query("""
            select distinct u.id as userId, c.id as classId, c.classCode as classCode, c.className as className
              from User u, Teacher teacher, HomeroomAssignment assignment, SchoolClass c, AcademicYear year
             where u.id in :homeroomUserIds and teacher.userId = u.id and assignment.teacherId = teacher.id
               and assignment.classId = c.id and c.academicYearId = year.id
               and assignment.status = com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus.ACTIVE
               and c.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
               and year.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
             order by u.id asc, c.id asc
            """)
    List<ClassContextProjection> findTeacherHomeroomContextsByUserIds(
            @Param("homeroomUserIds") Collection<Long> userIds);

    @Query("""
            select distinct u.id as userId, c.id as classId, c.classCode as classCode, c.className as className
              from User u, Teacher teacher, SubjectTeachingAssignment assignment, ClassSubject classSubject,
                   SchoolClass c, AcademicYear year
             where u.id in :subjectUserIds and teacher.userId = u.id and assignment.teacherId = teacher.id
               and assignment.classSubjectId = classSubject.id and classSubject.classId = c.id
               and c.academicYearId = year.id
               and assignment.status = com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus.ACTIVE
               and c.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE
               and year.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
             order by u.id asc, c.id asc
            """)
    List<ClassContextProjection> findTeacherSubjectContextsByUserIds(
            @Param("subjectUserIds") Collection<Long> userIds);

    interface IndividualAudienceProjection {
        Long getUserId();
        String getDisplayName();
        String getStudentCode();
        String getUsername();
    }

    interface UserRoleProjection {
        Long getUserId();
        String getRoleCode();
    }

    interface IndividualDisplayNameProjection {
        Long getUserId();
        String getDisplayName();
    }

    interface ClassContextProjection {
        Long getUserId();
        Long getClassId();
        String getClassCode();
        String getClassName();
    }
}
