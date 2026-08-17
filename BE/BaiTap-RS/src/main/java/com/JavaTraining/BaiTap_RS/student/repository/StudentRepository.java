package com.JavaTraining.BaiTap_RS.student.repository;

import java.util.Collection;
import java.util.List;

import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    boolean existsByStudentCode(String studentCode);

    @Query("select student.studentCode from Student student where student.studentCode in :studentCodes")
    List<String> findExistingStudentCodes(@Param("studentCodes") Collection<String> studentCodes);
}
