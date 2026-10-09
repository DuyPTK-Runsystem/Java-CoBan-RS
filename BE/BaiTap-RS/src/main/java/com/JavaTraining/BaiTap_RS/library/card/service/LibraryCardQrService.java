package com.JavaTraining.BaiTap_RS.library.card.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqVerifyLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LibraryCardQrService {

    private static final String PAYLOAD_VERSION = "v1";

    @Value("${app.library.card.hmac-secret:}")
    private String signingSecret;

    public void requireConfigured() {
        if (signingSecret == null || signingSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "CARD_SIGNING_UNAVAILABLE",
                    "Dịch vụ mã QR chưa được cấu hình");
        }
    }

    public String payload(LibraryCard card) {
        String content = PAYLOAD_VERSION + "|" + card.getCardNo() + "|" + card.getPatronId()
                + "|" + card.getExpiresAt().toEpochDay();
        return content + "|" + new String(sign(content), StandardCharsets.US_ASCII);
    }

    public byte[] png(String payload) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 320, 320);
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | java.io.IOException exception) {
            throw error(HttpStatus.INTERNAL_SERVER_ERROR, "CARD_QR_GENERATION_FAILED",
                    "Không thể tạo mã QR", exception);
        }
    }

    public ParsedPayload parseAndVerify(ReqVerifyLibraryCardDTO request) {
        String[] fields = request.payload().split("\\|", -1);
        validatePayloadShape(fields);
        try {
            long patronId = Long.parseLong(fields[2]);
            long expEpochDay = Long.parseLong(fields[3]);
            String content = String.join("|", fields[0], fields[1], fields[2], fields[3]);
            verifySignature(content, fields[4]);
            LocalDate expiresAt = LocalDate.ofEpochDay(expEpochDay);
            return new ParsedPayload(fields[1], patronId, expiresAt);
        } catch (NumberFormatException | DateTimeException exception) {
            throw error(HttpStatus.BAD_REQUEST, "CARD_PAYLOAD_MALFORMED", "Dữ liệu QR không đúng cấu trúc", exception);
        }
    }

    private void validatePayloadShape(String... fields) {
        if (fields.length != 5 || !PAYLOAD_VERSION.equals(fields[0]) || fields[1].isBlank()
                || fields[2].isBlank() || fields[3].isBlank() || fields[4].isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "CARD_PAYLOAD_MALFORMED", "Dữ liệu QR không đúng cấu trúc");
        }
    }

    private void verifySignature(String content, String signature) {
        if (!MessageDigest.isEqual(sign(content), signature.getBytes(StandardCharsets.US_ASCII))) {
            throw error(HttpStatus.BAD_REQUEST, "CARD_SIGNATURE_INVALID", "Chữ ký QR không hợp lệ");
        }
    }

    private byte[] sign(String content) {
        requireConfigured();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)))
                    .getBytes(StandardCharsets.US_ASCII);
        } catch (java.security.InvalidKeyException | java.security.NoSuchAlgorithmException exception) {
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "CARD_SIGNING_UNAVAILABLE",
                    "Không thể xác thực thẻ", exception);
        }
    }

    private LibraryPatronException error(HttpStatus status, String code, String message) {
        return new LibraryPatronException(status, code, message);
    }

    private LibraryPatronException error(HttpStatus status, String code, String message, Throwable cause) {
        return new LibraryPatronException(status, code, message, cause);
    }

    public record ParsedPayload(String cardNo, Long patronId, LocalDate expiresAt) { }
}
