package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

public final class BookBarcodeGenerator {

    public static String barcodeFor(Long copyId) {
        String digits = copyId.toString();
        return "LIB-" + "0".repeat(Math.max(0, 9 - digits.length())) + digits;
    }

    public static byte[] png(String barcode) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            BitMatrix matrix = new MultiFormatWriter().encode(barcode, BarcodeFormat.CODE_128, 420, 110, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("Không thể tạo ảnh barcode", exception);
        }
    }
}
