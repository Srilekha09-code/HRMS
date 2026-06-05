package com.hrms.project.repositories;

import com.hrms.project.models.AttendanceLog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    Optional<AttendanceLog> findByWorkerIdAndClockOutTimeIsNull(Long workerId);

    @Query("""
       select a
       from AttendanceLog a
       where a.worker.id = :workerId
         and a.clockOutTime is null
       """)
    Optional<AttendanceLog> findOpenAttendanceByWorkerId(@Param("workerId") Long workerId);

    @EntityGraph(attributePaths = {"worker", "site"})
    Page<AttendanceLog> findAttendanceHistory(Long workerId, OffsetDateTime fromDateTime, OffsetDateTime toDateTime, Pageable pageable);
}