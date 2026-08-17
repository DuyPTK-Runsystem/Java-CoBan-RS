package com.JavaTraining.BaiTap_RS.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_TYPE = "JWT";
    private static final String ALGORITHM = "HS256";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long accessTokenValidityInSeconds;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity-in-seconds}") long accessTokenValidityInSeconds) {
        this.objectMapper = objectMapper;
        this.secret = Base64.getDecoder().decode(secret);
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
    }

    public String createAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", ALGORITHM, "typ", TOKEN_TYPE);
        Map<String, Object> payload = Map.of(
                "sub", principal.getUsername(),
                "user_id", principal.getId(),
                "iat", now.getEpochSecond(),
                "exp", now.plusSeconds(accessTokenValidityInSeconds).getEpochSecond());
        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String unsignedToken = headerPart + "." + payloadPart;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String extractUsername(String token) {
        JsonNode payload = readPayload(token);
        long expiresAt = payload.path("exp").asLong();
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new IllegalArgumentException("Token is expired");
        }
        verifySignature(token);
        return payload.path("sub").asText();
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot create token", exception);
        }
    }

    private JsonNode readPayload(String token) {
        String[] parts = splitToken(token);
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        try {
            return objectMapper.readTree(payload);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Token payload is invalid", exception);
        }
    }

    private void verifySignature(String token) {
        String[] parts = splitToken(token);
        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!expectedSignature.equals(parts[2])) {
            throw new IllegalArgumentException("Token signature is invalid");
        }
    }

    private String[] splitToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Token format is invalid");
        }
        return parts;
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (InvalidKeyException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot sign token", exception);
        }
    }
}
