package com.hrms.project.service;

import com.hrms.project.enums.SettlementStatus;
import com.hrms.project.models.OvertimeEntry;
import com.hrms.project.repositories.OvertimeEntryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeEntryRepository repo;

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
}

