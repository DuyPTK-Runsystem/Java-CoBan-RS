package com.JavaTraining.BaiTap_RS.student.repository;

import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentInfoRepository extends JpaRepository<StudentInfo, Long> {
}
