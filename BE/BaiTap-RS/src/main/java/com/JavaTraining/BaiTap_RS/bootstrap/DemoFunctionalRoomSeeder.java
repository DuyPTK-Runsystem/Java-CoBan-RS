package com.JavaTraining.BaiTap_RS.bootstrap;

import java.util.List;

import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.seed.demo.enabled", havingValue = "true")
public class DemoFunctionalRoomSeeder implements ApplicationRunner {

    private static final List<RoomSeed> ROOMS = List.of(
            new RoomSeed("LAB-PHY-01", "Phòng thí nghiệm Vật lý"),
            new RoomSeed("LAB-CHEM-01", "Phòng thí nghiệm Hóa học"),
            new RoomSeed("LAB-BIO-01", "Phòng thí nghiệm Sinh học"),
            new RoomSeed("TIN-1", "Phòng Tin học 1"),
            new RoomSeed("TIN-2", "Phòng Tin học 2"),
            new RoomSeed("NGHE-1", "Phòng Nghề"));

    private final FunctionalRoomRepository functionalRoomRepository;

    public DemoFunctionalRoomSeeder(FunctionalRoomRepository functionalRoomRepository) {
        this.functionalRoomRepository = functionalRoomRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRooms();
    }

    @Transactional
    public void seedRooms() {
        for (RoomSeed room : ROOMS) {
            if (!functionalRoomRepository.existsByCode(room.code())) {
                functionalRoomRepository.save(createRoom(room));
            }
        }
    }

    private FunctionalRoom createRoom(RoomSeed room) {
        return new FunctionalRoom(room.code(), room.name());
    }

    private record RoomSeed(String code, String name) {
    }
}
