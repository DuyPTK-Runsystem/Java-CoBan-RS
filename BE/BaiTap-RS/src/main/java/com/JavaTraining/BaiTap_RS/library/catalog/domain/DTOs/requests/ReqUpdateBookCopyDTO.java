package com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReqUpdateBookCopyDTO {

    @NotNull
    @Min(0)
    private Long expectedVersion;

    @Size(max = 100)
    private String shelfLocation;

    private boolean shelfLocationProvided;
    private Boolean referenceOnly;
    private BookCopyStatus status;

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
        this.shelfLocationProvided = true;
    }

    @JsonIgnore
    public boolean isShelfLocationProvided() {
        return shelfLocationProvided;
    }

    public Boolean isReferenceOnly() {
        return referenceOnly;
    }

    public void setReferenceOnly(Boolean referenceOnly) {
        this.referenceOnly = referenceOnly;
    }

    public BookCopyStatus getStatus() {
        return status;
    }

    public void setStatus(BookCopyStatus status) {
        this.status = status;
    }
}
