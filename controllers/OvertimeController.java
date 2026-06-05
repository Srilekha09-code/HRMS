package com.hrms.project.controllers;

import com.hrms.project.models.OvertimeEntry;
import com.hrms.project.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService service;

    @GetMapping("/summary/{workerId}")
    public List<OvertimeEntry> summary(
            @PathVariable Long workerId,
            @RequestParam String month) {

        return service.getSummary(workerId, YearMonth.parse(month));
    }

    @PostMapping("/settle/{workerId}")
    public BigDecimal settle(
            @PathVariable Long workerId,
            @RequestParam String month) {

        return service.settle(workerId, YearMonth.parse(month));
    }
}
