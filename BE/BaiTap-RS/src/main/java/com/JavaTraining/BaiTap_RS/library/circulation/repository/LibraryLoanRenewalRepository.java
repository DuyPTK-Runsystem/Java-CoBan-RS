package com.JavaTraining.BaiTap_RS.library.circulation.repository;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoanRenewal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryLoanRenewalRepository extends JpaRepository<LibraryLoanRenewal, Long> {
}
