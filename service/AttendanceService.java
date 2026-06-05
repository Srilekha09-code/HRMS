package com.hrms.project.service;

import com.hrms.project.cacheService.ActiveWorkerCacheService;

import com.hrms.project.enums.SettlementStatus;
import com.hrms.project.exceptions.GlobalExceptionHandler.ApiException;
import com.hrms.project.models.AttendanceLog;
import com.hrms.project.models.OvertimeEntry;
import com.hrms.project.models.Site;
import com.hrms.project.models.Worker;
import com.hrms.project.repositories.AttendanceLogRepository;
import com.hrms.project.repositories.OvertimeEntryRepository;
import com.hrms.project.repositories.SiteRepository;
import com.hrms.project.repositories.WorkerRepository;
import com.hrms.project.utility.OvertimeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final double STANDARD_SHIFT_HOURS = 8.0;
    private static final double MAX_SHIFT_HOURS = 16.0;
    private static final double MONTHLY_OT_CAP = 60.0;

    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final OvertimeEntryRepository overtimeEntryRepository;
    private final ActiveWorkerCacheService activeWorkerCacheService;

    @Transactional
    public Map<String, Object> clockIn(Long workerId, Long siteId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ApiException(
                        "WORKER_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "Worker not found with id: " + workerId
                ));

        if (!worker.isActive()) {
            throw new ApiException(
                    "WORKER_INACTIVE",
                    HttpStatus.BAD_REQUEST,
                    "Worker is inactive with id: " + workerId
            );
        }

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ApiException(
                        "SITE_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "Site not found with id: " + siteId
                ));

        if (!site.isActive()) {
            throw new ApiException(
                    "SITE_INACTIVE",
                    HttpStatus.BAD_REQUEST,
                    "Site is inactive with id: " + siteId
            );
        }

        Optional<AttendanceLog> existingOpt =
                attendanceLogRepository.findOpenAttendanceByWorkerId(workerId);

        if (existingOpt.isPresent()) {

            AttendanceLog existing = existingOpt.get();

            String siteName = (existing.getSite() != null)
                    ? existing.getSite().getSiteName()
                    : "Unknown Site";

            throw new ApiException(
                    "DUPLICATE_CLOCK_IN",
                    HttpStatus.CONFLICT,
                    "Worker is already clocked in at Site: " + siteName
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        AttendanceLog attendance = new AttendanceLog();
        attendance.setWorker(worker);
        attendance.setSite(site);
        attendance.setClockInTime(now);
        attendance.setClockOutTime(null);
        attendance.setTotalHoursWorked(0.0);
        attendance.setOvertimeHours(0.0);
        attendance.setFlagged(false);

        AttendanceLog saved = attendanceLogRepository.save(attendance);

        // Redis entry: workerId, workerName, siteId, siteName, clockInTime
        activeWorkerCacheService.put(
                worker.getId(),
                worker.getName(),
                site.getId(),
                site.getSiteName(),
                saved.getClockInTime()
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attendanceId", saved.getId());
        response.put("workerId", worker.getId());
        response.put("workerName", worker.getName());
        response.put("siteId", site.getId());
        response.put("siteName", site.getSiteName());
        response.put("clockInTime", saved.getClockInTime());
        response.put("message", "Clock-in successful");
        return response;
    }

    @Transactional
    public Map<String, Object> clockOut(Long workerId) {
        AttendanceLog attendance = (AttendanceLog) attendanceLogRepository.findOpenAttendanceByWorkerId(workerId)
                .orElseThrow(() -> new ApiException(
                        "OPEN_ATTENDANCE_NOT_FOUND",
                        HttpStatus.BAD_REQUEST,
                        "Worker is not currently clocked in. Worker id: " + workerId
                ));

        OffsetDateTime clockOut = OffsetDateTime.now();

        double totalHours = ChronoUnit.MINUTES.between(attendance.getClockInTime(), clockOut) / 60.0;
        totalHours = round(totalHours);

        boolean flagged = totalHours > MAX_SHIFT_HOURS;
        double rawOvertime = Math.max(0.0, totalHours - STANDARD_SHIFT_HOURS);

        Worker worker = attendance.getWorker();
        YearMonth month = YearMonth.from(clockOut);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Double usedHours = overtimeEntryRepository.sumMonthlyOvertimeHours(workerId, start, end);
        double existingMonthlyHours = usedHours == null ? 0.0 : usedHours;
        double remainingAllowed = Math.max(0.0, MONTHLY_OT_CAP - existingMonthlyHours);
        double cappedOvertime = Math.min(rawOvertime, remainingAllowed);
        cappedOvertime = round(cappedOvertime);

        attendance.setClockOutTime(clockOut);
        attendance.setTotalHoursWorked(totalHours);
        attendance.setOvertimeHours(cappedOvertime);
        attendance.setFlagged(flagged);

        attendanceLogRepository.save(attendance);

        BigDecimal overtimeAmount = BigDecimal.ZERO;

        if (cappedOvertime > 0.0) {
            BigDecimal hourlyRate = worker.getDailyWageRate()
                    .divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);

            OvertimeEntry entry = new OvertimeEntry();
            entry.setWorker(worker);
            entry.setAttendanceLog(attendance);
            entry.setDate(clockOut.toLocalDate());
            entry.setOvertimeHours(cappedOvertime);
            entry.setOvertimeRateApplied(hourlyRate);
            entry.setAmount(OvertimeCalculator.calculateAmount(worker.getDailyWageRate(), cappedOvertime));
            entry.setStatus(SettlementStatus.PENDING);

            overtimeEntryRepository.save(entry);
            overtimeAmount = entry.getAmount();
        }

        activeWorkerCacheService.remove(workerId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attendanceId", attendance.getId());
        response.put("workerId", workerId);
        response.put("clockInTime", attendance.getClockInTime());
        response.put("clockOutTime", attendance.getClockOutTime());
        response.put("totalHoursWorked", attendance.getTotalHoursWorked());
        response.put("overtimeHoursRecorded", attendance.getOvertimeHours());
        response.put("overtimeAmount", overtimeAmount);
        response.put("flagged", attendance.isFlagged());
        response.put("message", "Clock-out successful");
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveWorkers() {
        // MUST come only from Redis
        return activeWorkerCacheService.getAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAttendanceHistory(Long workerId, LocalDate from, LocalDate to, int page, int size) {
        if (from.isAfter(to)) {
            throw new ApiException(
                    "INVALID_DATE_RANGE",
                    HttpStatus.BAD_REQUEST,
                    "Invalid date range. 'from' must be before or equal to 'to'"
            );
        }

        workerRepository.findById(workerId)
                .orElseThrow(() -> new ApiException(
                        "WORKER_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "Worker not found with id: " + workerId
                ));

        OffsetDateTime fromDateTime = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDateTime = to.plusDays(1).atStartOfDay().minusNanos(1).atOffset(ZoneOffset.UTC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "clockInTime"));
        Page<AttendanceLog> resultPage =
                attendanceLogRepository.findAttendanceHistory(workerId, fromDateTime, toDateTime, pageable);

        List<Map<String, Object>> content = resultPage.getContent().stream().map(a -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("attendanceId", a.getId());
            row.put("workerId", a.getWorker().getId());
            row.put("workerName", a.getWorker().getName());
            row.put("siteId", a.getSite().getId());
            row.put("siteName", a.getSite().getSiteName());
            row.put("clockInTime", a.getClockInTime());
            row.put("clockOutTime", a.getClockOutTime());
            row.put("totalHoursWorked", a.getTotalHoursWorked());
            row.put("overtimeHours", a.getOvertimeHours());
            row.put("flagged", a.isFlagged());
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("totalElements", resultPage.getTotalElements());
        response.put("totalPages", resultPage.getTotalPages());
        response.put("currentPage", resultPage.getNumber());
        response.put("pageSize", resultPage.getSize());
        return response;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}