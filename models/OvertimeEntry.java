package com.hrms.project.models;

import com.hrms.project.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class OvertimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Worker worker;

    @OneToOne
    private AttendanceLog attendanceLog;

    private LocalDate date;
    private double overtimeHours;

    private BigDecimal overtimeRateApplied;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;


}