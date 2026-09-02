package com.JavaTraining.BaiTap_RS.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTest {

    private static final String SECRET = "c2VjdXJlLXNlY3JldC1mb3ItdGVzdC10b2tlbi1zaWduaW5n";

    @Test
    void accessTokenContainsSortedRoleCodesWithoutAuthorityPrefix() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JwtTokenService service = new JwtTokenService(objectMapper, SECRET, 3600L);
        User user = new User("admin01", "bcrypt-hash");
        user.addRole(new Role("TEACHER", "Teacher", "Teacher"));
        user.addRole(new Role("ADMIN", "Administrator", "Admin"));
        ReflectionTestUtils.setField(user, "id", 1L);

        String token = service.createAccessToken(new UserPrincipal(user));
        JsonNode payload = decodePayload(objectMapper, token);

        Assertions.assertEquals(
                List.of("ADMIN", "TEACHER"),
                objectMapper.convertValue(payload.path("role"), List.class),
                "JWT role claim should contain sorted canonical role codes");
    }

    private JsonNode decodePayload(ObjectMapper objectMapper, String token) throws Exception {
        String payloadPart = token.split("\\.")[1];
        byte[] payload = Base64.getUrlDecoder().decode(payloadPart);
        return objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
    }
}
