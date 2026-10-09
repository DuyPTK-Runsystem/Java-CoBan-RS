package com.JavaTraining.BaiTap_RS.user.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("""
            select distinct u from User u
            where not exists (select p.id from LibraryPatron p where p.userId = u.id)
              and (:keyword is null or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or exists (select s.id from Student s where s.userId = u.id
                       and lower(s.studentName) like lower(concat('%', :keyword, '%')))
                   or exists (select t.id from Teacher t where t.userId = u.id
                       and lower(t.teacherName) like lower(concat('%', :keyword, '%'))))
            """)
    Page<User> findActivationCandidates(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.roles r
            WHERE r.code IN ('ADMIN', 'ACADEMIC_OFFICE')
            """)
    java.util.List<User> findAcademicOfficeAndAdminUsers();
}
