package com.JavaTraining.BaiTap_RS;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:baitaprs;MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BaiTapRsApplicationTests {

    @Test
    void contextLoads() {
    }
}
