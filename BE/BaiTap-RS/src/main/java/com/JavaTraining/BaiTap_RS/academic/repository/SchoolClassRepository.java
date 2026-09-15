package com.JavaTraining.BaiTap_RS.academic.repository;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long>,
                SchoolClassLookupRepository, SchoolClassConstraintRepository {
}
