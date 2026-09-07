package com.JavaTraining.BaiTap_RS.student.domain.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(
        name = "student_info",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_info_student", columnNames = "student_id"))
public class StudentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "average_score")
    private Double averageScore;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    public StudentInfo(LocalDate dateOfBirth, String address, Double averageScore) {
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.averageScore = averageScore;
    }
}
