package com.hrms.project.controllers;

import com.hrms.project.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public Map<String, Object> clockIn(@RequestBody Map<String, Object> request) {
        Long workerId = Long.valueOf(request.get("workerId").toString());
        Long siteId = Long.valueOf(request.get("siteId").toString());
        return attendanceService.clockIn(workerId, siteId);
    }

    @PostMapping("/clock-out")
    public Map<String, Object> clockOut(@RequestBody Map<String, Object> request) {
        Long workerId = Long.valueOf(request.get("workerId").toString());
        return attendanceService.clockOut(workerId);
    }

    @GetMapping("/active")
    public List<Map<String, Object>> getActiveWorkers() {
        return attendanceService.getActiveWorkers();
    }

    @GetMapping("/log")
    public Map<String, Object> getAttendanceLog(
            @RequestParam Long workerId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return attendanceService.getAttendanceHistory(workerId, from, to, page, size);
    }
}
