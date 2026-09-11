package com.JavaTraining.BaiTap_RS.common.contract.v3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class V3ContractDtoTest {

    @Test
    void pageResponseCopiesItemsAndAppliedFilters() {
        List<String> items = new ArrayList<>(List.of("HS001"));
        Map<String, String> filters = new HashMap<>(Map.of("status", "ACTIVE"));

        V3PageResponse<String> response = new V3PageResponse<>(items, 0, 20, 1, filters);

        items.add("HS002");
        filters.put("gradeId", "6");

        assertEquals(List.of("HS001"), response.items());
        assertEquals(Map.of("status", "ACTIVE"), response.appliedFilters());
        assertThrows(UnsupportedOperationException.class, () -> response.items().add("HS003"));
        assertThrows(UnsupportedOperationException.class, () -> response.appliedFilters().put("gradeId", "6"));
    }

    @Test
    void pageResponseRejectsInvalidPagingMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new V3PageResponse<>(List.of(), -1, 20, 0, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new V3PageResponse<>(List.of(), 0, 0, 0, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new V3PageResponse<>(List.of(), 0, 20, -1, Map.of()));
    }

}
