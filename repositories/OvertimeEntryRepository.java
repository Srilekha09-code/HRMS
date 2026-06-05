package com.hrms.project.repositories;

import com.hrms.project.models.OvertimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OvertimeEntryRepository extends JpaRepository<OvertimeEntry, Long> {

    @Query("SELECT SUM(o.overtimeHours) FROM OvertimeEntry o WHERE o.worker.id=:wid AND o.date BETWEEN :start AND :end")
    Double findMonthlyHours(Long wid, LocalDate start, LocalDate end);

    List<OvertimeEntry> findByWorkerIdAndDateBetween(Long wid, LocalDate start, LocalDate end);

    Double sumMonthlyOvertimeHours(Long workerId, LocalDate start, LocalDate end);
}
