package com.JavaTraining.BaiTap_RS.functionalroom.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FunctionalRoomRepository extends JpaRepository<FunctionalRoom, Long> {

        boolean existsByCode(String code);

        boolean existsByCodeAndIdNot(String code, Long id);

        List<FunctionalRoom> findByStatus(RoomStatus status);

        @Query("SELECT r FROM FunctionalRoom r WHERE "
                        + "(:status IS NULL OR r.status = :status) AND "
                        + "(:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) "
                        + "OR LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<FunctionalRoom> searchRooms(
                        @Param("search") String search,
                        @Param("status") RoomStatus status,
                        Pageable pageable);
}
