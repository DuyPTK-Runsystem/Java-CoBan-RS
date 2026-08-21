package com.JavaTraining.BaiTap_RS.common.audit.repository;

import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
