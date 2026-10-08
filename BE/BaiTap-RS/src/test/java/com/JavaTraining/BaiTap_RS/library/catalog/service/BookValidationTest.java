package com.JavaTraining.BaiTap_RS.library.catalog.service;

import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class BookValidationTest {

    @ParameterizedTest
    @CsvSource({ "' 0-306-40615-2 ', 0306406152", "978-0-306-40615-7, 9780306406157", "0-8044-2957-x, 080442957X" })
    void normalizesFormattedIsbn(String input, String expected) {
        assertEquals(expected, BookValidation.normalizeIsbn(input), "Valid ISBN should be normalized");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "   " })
    void treatsMissingOrBlankIsbnAsAbsent(String input) {
        assertNull(BookValidation.normalizeIsbn(input), "Missing ISBN should remain absent");
    }

    @ParameterizedTest
    @ValueSource(strings = { "9780306406158", "٩٧٨٠٣٠٦٤٠٦١٥٧", "٠٣٠٦٤٠٦١٥٢", " - " })
    void rejectsInvalidChecksumsAndNonAsciiDigits(String input) {
        assertEquals("INVALID_ISBN", isbnFailureCode(input), "Invalid ISBN should use the stable validation code");
    }

    @Test
    void acceptsOnlyAbsoluteHttpCoverUrls() {
        assertEquals("https://example.org/cover.jpg",
                BookValidation.validateCoverUrl("  https://example.org/cover.jpg  "),
                "Absolute HTTP cover URL should be trimmed");
    }

    @Test
    void treatsBlankCoverUrlAsAbsent() {
        assertNull(BookValidation.validateCoverUrl("  "), "Blank cover URL should remain absent");
    }

    @Test
    void rejectsNonHttpCoverUrl() {
        assertEquals("INVALID_COVER_URL", coverUrlFailureCode("javascript:alert(1)"),
                "Invalid cover URL should use the stable validation code");
    }

    private String isbnFailureCode(String input) {
        try {
            BookValidation.normalizeIsbn(input);
        } catch (LibraryCatalogException error) {
            return error.getCode();
        }
        throw new AssertionError("Invalid ISBN input should be rejected");
    }

    private String coverUrlFailureCode(String input) {
        try {
            BookValidation.validateCoverUrl(input);
        } catch (LibraryCatalogException error) {
            return error.getCode();
        }
        throw new AssertionError("Non-HTTP cover URL should be rejected");
    }
}
