package com.JavaTraining.BaiTap_RS.library.patron.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryPatronRepository extends JpaRepository<LibraryPatron, Long> {

    Optional<LibraryPatron> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LibraryPatron p where p.id in :ids order by p.id")
    java.util.List<LibraryPatron> findAllByIdsForUpdate(@Param("ids") java.util.List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LibraryPatron p where p.id = :id")
    Optional<LibraryPatron> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select p from LibraryPatron p join User u on u.id = p.userId
            where (:status is null or p.status = :status)
              and (:keyword is null or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or exists (select s.id from Student s where s.userId = u.id
                       and lower(s.studentName) like lower(concat('%', :keyword, '%')))
                   or exists (select t.id from Teacher t where t.userId = u.id
                       and lower(t.teacherName) like lower(concat('%', :keyword, '%'))))
            """)
    Page<LibraryPatron> search(@Param("keyword") String keyword,
            @Param("status") com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus status,
            Pageable pageable);
}
