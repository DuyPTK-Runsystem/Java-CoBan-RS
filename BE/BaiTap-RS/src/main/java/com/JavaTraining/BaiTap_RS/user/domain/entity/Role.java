package com.JavaTraining.BaiTap_RS.user.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "role", uniqueConstraints = @UniqueConstraint(name = "uk_role_code", columnNames = "code"))
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    @Setter(lombok.AccessLevel.PACKAGE)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    @Setter(lombok.AccessLevel.PACKAGE)
    private String name;

    @Column(name = "description", length = 255)
    @Setter(lombok.AccessLevel.PACKAGE)
    private String description;

    public Role(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
