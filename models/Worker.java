package com.hrms.project.models;

import com.hrms.project.enums.Designation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "workers",
        uniqueConstraints = @UniqueConstraint(columnNames = "phone"))
@Getter
@Setter
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Designation designation;

    private BigDecimal dailyWageRate;

    private boolean active;
}