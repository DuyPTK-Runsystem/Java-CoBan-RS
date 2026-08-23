package com.JavaTraining.BaiTap_RS.student.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "student",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_student_code", columnNames = "student_code"),
                @UniqueConstraint(name = "uk_student_user", columnNames = "user_id")
        })
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id", nullable = false)
    private Long id;

    @Column(name = "student_name", nullable = false, length = 35)
    private String studentName;

    @Column(name = "student_code", nullable = false, length = 10, unique = true)
    private String studentCode;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

    @OneToOne(
            mappedBy = "student",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY,
            optional = false)
    private StudentInfo studentInfo;

    public Student(String studentName, String studentCode) {
        this.studentName = studentName;
        this.studentCode = studentCode;
    }

    public void assignInfo(StudentInfo info) {
        this.studentInfo = info;
        info.setStudent(this);
    }
}
