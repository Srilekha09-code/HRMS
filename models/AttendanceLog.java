package com.hrms.project.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Worker worker;

    @ManyToOne
    private Site site;

    private OffsetDateTime clockInTime;
    private OffsetDateTime clockOutTime;

    private double totalHoursWorked;
    private double overtimeHours;

    private boolean flagged;
}