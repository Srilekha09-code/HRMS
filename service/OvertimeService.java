package com.hrms.project.service;

import com.hrms.project.enums.SettlementStatus;
import com.hrms.project.events.OvertimeSettlementEvent;
import com.hrms.project.models.OvertimeEntry;
import com.hrms.project.repositories.OvertimeEntryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeEntryRepository repo;
    private final ApplicationEventPublisher eventPublisher;
    @Autowired
    private OvertimeEntryRepository overtimeEntryRepository;

    public List<OvertimeEntry> getSummary(Long workerId, YearMonth month) {
        return repo.findByWorkerIdAndDateBetween(workerId,
                month.atDay(1), month.atEndOfMonth());
    }

    @Transactional
    public BigDecimal settle(Long workerId, YearMonth month) {

        if (month.equals(YearMonth.now()))
            throw new RuntimeException("Cannot settle current month");

        List<OvertimeEntry> entries = getSummary(workerId, month);

        BigDecimal total = BigDecimal.ZERO;

        for (OvertimeEntry e : entries) {
            if (e.getStatus() == SettlementStatus.SETTLED)
                continue;

            e.setStatus(SettlementStatus.SETTLED);
            total = total.add(e.getAmount());
        }

        repo.saveAll(entries);

        return total;
    }

    @Transactional
    public Map<String, Object> settleMonth(Long workerId, YearMonth month) {

        List<OvertimeEntry> entries =
                overtimeEntryRepository.findPendingEntriesForSettlement(
                        workerId,
                        month.atDay(1),
                        month.atEndOfMonth(),
                        SettlementStatus.PENDING
                );

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OvertimeEntry entry : entries) {
            entry.setStatus(SettlementStatus.SETTLED);
            totalAmount = totalAmount.add(entry.getAmount());
        }

        // ✅ FIXED LINE
        overtimeEntryRepository.saveAll(entries);

        return Map.of(
                "workerId", workerId,
                "month", month.toString(),
                "amount", totalAmount
        );
    }
}

