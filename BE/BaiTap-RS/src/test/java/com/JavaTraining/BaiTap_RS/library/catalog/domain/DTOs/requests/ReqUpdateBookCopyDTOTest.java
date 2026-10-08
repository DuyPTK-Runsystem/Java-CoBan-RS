package com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ReqUpdateBookCopyDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void omittedShelfLocationIsNotProvided() throws Exception {
        ReqUpdateBookCopyDTO omitted = objectMapper.readValue("{\"expectedVersion\":0}",
                ReqUpdateBookCopyDTO.class);

        assertFalse(omitted.isShelfLocationProvided(), "omitted shelf location is not provided");
    }

    @Test
    void explicitNullShelfLocationIsProvided() throws Exception {
        ReqUpdateBookCopyDTO explicitNull = objectMapper.readValue(
                "{\"expectedVersion\":0,\"shelfLocation\":null}", ReqUpdateBookCopyDTO.class);

        assertTrue(explicitNull.isShelfLocationProvided(), "explicit null is provided");
    }

    @Test
    void explicitNullShelfLocationRemainsNull() throws Exception {
        ReqUpdateBookCopyDTO explicitNull = objectMapper.readValue(
                "{\"expectedVersion\":0,\"shelfLocation\":null}", ReqUpdateBookCopyDTO.class);

        assertNull(explicitNull.getShelfLocation(), "explicit null remains null");
    }

    @Test
    void reportsShelfLocationAsProvidedWhenBlank() throws Exception {
        ReqUpdateBookCopyDTO blank = objectMapper.readValue(
                "{\"expectedVersion\":0,\"shelfLocation\":\"   \"}", ReqUpdateBookCopyDTO.class);

        assertTrue(blank.isShelfLocationProvided(), "blank shelf location is still explicitly provided");
    }
}
