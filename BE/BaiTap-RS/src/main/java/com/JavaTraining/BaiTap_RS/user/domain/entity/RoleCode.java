package com.JavaTraining.BaiTap_RS.user.domain.entity;

/** Canonical role codes shared by authorization and business rules. */
public enum RoleCode {
    ADMIN,
    ACADEMIC_OFFICE,
    TEACHER,
    STUDENT,
    LIBRARIAN;

    public String code() {
        return name();
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
