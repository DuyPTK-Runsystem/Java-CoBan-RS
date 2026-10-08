package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class BookBarcodeGeneratorTest {

    private final BookBarcodeGenerator generator = new BookBarcodeGenerator();

    @Test
    void padsSmallBarcodeIdsWithLeadingZeros() {
        assertEquals("LIB-000000123", generator.barcodeFor(123L), "small ids are zero padded");
    }

    @Test
    void preservesIdsLargerThanNineDigits() {
        assertEquals("LIB-1000000000", generator.barcodeFor(1_000_000_000L),
                "large ids are not truncated");
    }

    @Test
    void code128PngDecodesBackToTheOriginalBarcode() throws IOException, com.google.zxing.NotFoundException {
        String barcode = "LIB-000000123";
        java.awt.image.BufferedImage image = ImageIO.read(new ByteArrayInputStream(generator.png(barcode)));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));

        assertEquals(barcode, new MultiFormatReader().decode(bitmap).getText(),
                "generated Code 128 image decodes to its source barcode");
    }
}
