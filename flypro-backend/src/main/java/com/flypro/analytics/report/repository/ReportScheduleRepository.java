package com.flypro.analytics.report.repository;

import com.flypro.analytics.report.entity.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    List<ReportSchedule> findByEnabledTrueAndNextRunAtLessThanEqual(Instant now);
}
