package com.flypro.analytics.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flypro.analytics.accessevent.mapper.AccessEventMapper;
import com.flypro.analytics.accessevent.repository.AccessEventRepository;
import com.flypro.analytics.accesspoint.mapper.AccessPointMapper;
import com.flypro.analytics.accesspoint.repository.AccessPointRepository;
import com.flypro.analytics.accessprofile.mapper.AccessProfileMapper;
import com.flypro.analytics.accessprofile.repository.AccessProfileRepository;
import com.flypro.analytics.alert.dto.AlertResponseDTO;
import com.flypro.analytics.alert.mapper.AlertMapper;
import com.flypro.analytics.alert.repository.AlertRepository;
import com.flypro.analytics.audit.repository.AuditLogRepository;
import com.flypro.analytics.badge.mapper.BadgeMapper;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.badgeassignment.repository.BadgeAssignmentRepository;
import com.flypro.analytics.dashboard.dto.DashboardFilterCriteria;
import com.flypro.analytics.dashboard.dto.StatisticsDTO;
import com.flypro.analytics.dashboard.service.DashboardService;
import com.flypro.analytics.department.mapper.DepartmentMapper;
import com.flypro.analytics.department.repository.DepartmentRepository;
import com.flypro.analytics.employee.mapper.EmployeeMapper;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import com.flypro.analytics.importbatch.mapper.ImportMapper;
import com.flypro.analytics.importbatch.repository.ImportBatchRepository;
import com.flypro.analytics.report.dto.ReportFilterCriteria;
import com.flypro.analytics.report.enums.ReportKind;
import com.flypro.analytics.report.enums.ReportType;
import com.flypro.analytics.report.model.ReportDocument;
import com.flypro.analytics.zone.mapper.ZoneMapper;
import com.flypro.analytics.zone.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds {@link ReportDocument}s from existing domain repositories/mappers.
 * Read-only aggregation: filters are applied in-memory on the already-mapped
 * response DTOs, so this never touches internal entity field names beyond
 * what the existing mappers already expose.
 */
@Service
@Transactional(readOnly = true)
public class ReportDocumentFactoryImpl implements ReportDocumentFactory {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final BadgeRepository badgeRepository;
    private final BadgeMapper badgeMapper;
    private final BadgeAssignmentRepository badgeAssignmentRepository;
    private final ZoneRepository zoneRepository;
    private final ZoneMapper zoneMapper;
    private final AccessProfileRepository accessProfileRepository;
    private final AccessProfileMapper accessProfileMapper;
    private final AccessPointRepository accessPointRepository;
    private final AccessPointMapper accessPointMapper;
    private final AccessEventRepository accessEventRepository;
    private final AccessEventMapper accessEventMapper;
    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final ImportBatchRepository importBatchRepository;
    private final ImportMapper importMapper;
    private final AuditLogRepository auditLogRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public ReportDocumentFactoryImpl(EmployeeRepository employeeRepository,
                                      EmployeeMapper employeeMapper,
                                      DepartmentRepository departmentRepository,
                                      DepartmentMapper departmentMapper,
                                      BadgeRepository badgeRepository,
                                      BadgeMapper badgeMapper,
                                      BadgeAssignmentRepository badgeAssignmentRepository,
                                      ZoneRepository zoneRepository,
                                      ZoneMapper zoneMapper,
                                      AccessProfileRepository accessProfileRepository,
                                      AccessProfileMapper accessProfileMapper,
                                      AccessPointRepository accessPointRepository,
                                      AccessPointMapper accessPointMapper,
                                      AccessEventRepository accessEventRepository,
                                      AccessEventMapper accessEventMapper,
                                      AlertRepository alertRepository,
                                      AlertMapper alertMapper,
                                      ImportBatchRepository importBatchRepository,
                                      ImportMapper importMapper,
                                      AuditLogRepository auditLogRepository,
                                      DashboardService dashboardService,
                                      ObjectMapper objectMapper) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
        this.badgeRepository = badgeRepository;
        this.badgeMapper = badgeMapper;
        this.badgeAssignmentRepository = badgeAssignmentRepository;
        this.zoneRepository = zoneRepository;
        this.zoneMapper = zoneMapper;
        this.accessProfileRepository = accessProfileRepository;
        this.accessProfileMapper = accessProfileMapper;
        this.accessPointRepository = accessPointRepository;
        this.accessPointMapper = accessPointMapper;
        this.accessEventRepository = accessEventRepository;
        this.accessEventMapper = accessEventMapper;
        this.alertRepository = alertRepository;
        this.alertMapper = alertMapper;
        this.importBatchRepository = importBatchRepository;
        this.importMapper = importMapper;
        this.auditLogRepository = auditLogRepository;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReportDocument build(ReportType type, ReportFilterCriteria filters) {
        return switch (type) {
            case EMPLOYEES -> employees(filters);
            case DEPARTMENTS -> departments(filters);
            case BADGES -> badges(filters);
            case BADGE_ASSIGNMENTS -> badgeAssignments(filters);
            case ZONES -> zones(filters);
            case ACCESS_PROFILES -> accessProfiles(filters);
            case ACCESS_POINTS -> accessPoints(filters);
            case ACCESS_EVENTS -> accessEvents(filters);
            case ALERTS -> alerts(filters);
            case AUDIT_LOGS -> auditLogs(filters);
            case IMPORTS -> imports(filters);
            case DASHBOARD_STATISTICS -> dashboardStatistics(filters);
            case BADGE_EXPIRATION -> badgeExpiration(filters);
            case ALERT_SUMMARY -> alertSummary(filters);
        };
    }

    private ReportDocument employees(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = employeeRepository.findAll().stream()
                .map(employeeMapper::toResponse)
                .filter(dto -> f.departmentId() == null
                        || (dto.department() != null && f.departmentId().equals(dto.department().id())))
                .filter(dto -> f.employeeId() == null || f.employeeId().equals(dto.id()))
                .filter(dto -> withinRange(dto.createdAt(), f.from(), f.to()))
                .map(this::toRow)
                .toList();
        return document(ReportType.EMPLOYEES, f.reportKind(), "Employees Report", rows, Map.of("total", rows.size()));
    }

    private ReportDocument departments(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .filter(dto -> f.departmentId() == null || f.departmentId().equals(dto.id()))
                .filter(dto -> withinRange(dto.createdAt(), f.from(), f.to()))
                .map(this::toRow)
                .toList();
        return document(ReportType.DEPARTMENTS, f.reportKind(), "Departments Report", rows, Map.of("total", rows.size()));
    }

    private ReportDocument badges(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = badgeRepository.findAll().stream()
                .map(badgeMapper::toResponse)
                .filter(dto -> f.badgeId() == null || f.badgeId().equals(dto.id()))
                .filter(dto -> withinRange(dto.createdAt(), f.from(), f.to()))
                .map(this::toRow)
                .toList();
        return document(ReportType.BADGES, f.reportKind(), "Badges Report", rows, Map.of("total", rows.size()));
    }

    private ReportDocument badgeAssignments(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = badgeAssignmentRepository.findAll().stream()
                .filter(a -> f.badgeId() == null || f.badgeId().equals(a.getBadge().getId()))
                .filter(a -> f.employeeId() == null || f.employeeId().equals(a.getEmployee().getId()))
                .filter(a -> withinRange(a.getAssignedAt(), f.from(), f.to()))
                .map(badgeMapper::toAssignmentDto)
                .map(this::toRow)
                .toList();
        return document(ReportType.BADGE_ASSIGNMENTS, f.reportKind(), "Badge Assignments Report", rows,
                Map.of("total", rows.size()));
    }

    private ReportDocument zones(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = zoneRepository.findAll().stream()
                .map(zoneMapper::toResponse)
                .filter(dto -> f.zoneId() == null || f.zoneId().equals(dto.id()))
                .filter(dto -> withinRange(dto.createdAt(), f.from(), f.to()))
                .map(this::toRow)
                .toList();
        return document(ReportType.ZONES, f.reportKind(), "Zones Report", rows, Map.of("total", rows.size()));
    }

    private ReportDocument accessProfiles(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = accessProfileRepository.findAll().stream()
                .map(accessProfileMapper::toResponse)
                .filter(dto -> withinRange(dto.createdAt(), f.from(), f.to()))
                .map(this::toRow)
                .toList();
        return document(ReportType.ACCESS_PROFILES, f.reportKind(), "Access Profiles Report", rows,
                Map.of("total", rows.size()));
    }

    private ReportDocument accessPoints(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = accessPointRepository.findAll().stream()
                .map(accessPointMapper::toResponse)
                .filter(dto -> f.zoneId() == null || (dto.zone() != null && f.zoneId().equals(dto.zone().id())))
                .filter(dto -> f.accessPointId() == null || f.accessPointId().equals(dto.id()))
                .filter(dto -> withinRange(dto.createdAt(), f.from(), f.to()))
                .map(this::toRow)
                .toList();
        return document(ReportType.ACCESS_POINTS, f.reportKind(), "Access Points Report", rows,
                Map.of("total", rows.size()));
    }

    private ReportDocument accessEvents(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = accessEventRepository.findAll().stream()
                .map(accessEventMapper::toResponse)
                .filter(dto -> f.employeeId() == null || f.employeeId().equals(dto.employeeId()))
                .filter(dto -> f.badgeId() == null || f.badgeId().equals(dto.badgeId()))
                .filter(dto -> f.zoneId() == null || (dto.zone() != null && f.zoneId().equals(dto.zone().id())))
                .filter(dto -> f.accessPointId() == null || f.accessPointId().equals(dto.accessPointId()))
                .filter(dto -> f.accessResult() == null || f.accessResult() == dto.result())
                .filter(dto -> withinRange(dto.eventTimestamp(), f.from(), f.to()))
                .map(this::toRow)
                .toList();
        long authorized = rows.stream().filter(r -> "AUTHORIZED".equals(String.valueOf(r.get("result")))).count();
        return document(ReportType.ACCESS_EVENTS, f.reportKind(), "Access Events Report", rows,
                Map.of("total", rows.size(), "authorized", authorized, "denied", rows.size() - authorized));
    }

    private ReportDocument alerts(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = alertRepository.findAll().stream()
                .map(alertMapper::toResponse)
                .filter(dto -> f.employeeId() == null || f.employeeId().equals(dto.employeeId()))
                .filter(dto -> f.badgeId() == null || f.badgeId().equals(dto.badgeId()))
                .filter(dto -> f.alertSeverity() == null || f.alertSeverity() == dto.severity())
                .filter(dto -> f.alertType() == null || f.alertType() == dto.type())
                .filter(dto -> withinRange(dto.createdDate(), f.from(), f.to()))
                .map(this::toRow)
                .toList();
        return document(ReportType.ALERTS, f.reportKind(), "Alerts Report", rows, Map.of("total", rows.size()));
    }

    private ReportDocument auditLogs(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = auditLogRepository.findAll().stream()
                .filter(a -> withinRange(a.getCreatedAt(), f.from(), f.to()))
                .filter(a -> f.username() == null || f.username().equalsIgnoreCase(a.getUsername()))
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", a.getId());
                    row.put("action", a.getAction());
                    row.put("entityName", a.getEntityName());
                    row.put("entityId", a.getEntityId());
                    row.put("username", a.getUsername());
                    row.put("ipAddress", a.getIpAddress());
                    row.put("createdAt", a.getCreatedAt());
                    return row;
                })
                .toList();
        return document(ReportType.AUDIT_LOGS, f.reportKind(), "Audit Logs Report", rows, Map.of("total", rows.size()));
    }

    private ReportDocument imports(ReportFilterCriteria f) {
        List<Map<String, Object>> rows = importBatchRepository.findAll().stream()
                .map(importMapper::toResponse)
                .filter(dto -> withinRange(dto.createdAt(), f.from(), f.to()))
                .map(this::toRow)
                .toList();
        return document(ReportType.IMPORTS, f.reportKind(), "Imports Report", rows, Map.of("total", rows.size()));
    }

    private ReportDocument dashboardStatistics(ReportFilterCriteria f) {
        DashboardFilterCriteria criteria = new DashboardFilterCriteria(
                f.from(), f.to(), f.departmentId(), f.zoneId(), f.accessPointId(), f.employeeId(), f.badgeId(),
                f.alertSeverity(), f.alertType());
        StatisticsDTO stats = dashboardService.getStatistics(criteria);
        return ReportDocument.builder()
                .reportType(ReportType.DASHBOARD_STATISTICS)
                .reportKind(f.reportKind())
                .title("Dashboard Statistics Report")
                .generatedAt(Instant.now())
                .summary(toRow(stats))
                .columns(List.of())
                .rows(List.of())
                .build();
    }

    private ReportDocument badgeExpiration(ReportFilterCriteria f) {
        Instant to = f.to() != null ? f.to() : Instant.now().plus(Duration.ofDays(30));
        List<Map<String, Object>> rows = badgeRepository.findAll().stream()
                .map(badgeMapper::toResponse)
                .filter(dto -> dto.expirationDate() != null)
                .filter(dto -> withinRange(dto.expirationDate().atStartOfDay(ZoneOffset.UTC).toInstant(), f.from(), to))
                .map(this::toRow)
                .toList();
        return document(ReportType.BADGE_EXPIRATION, f.reportKind(), "Badge Expiration Report", rows,
                Map.of("total", rows.size()));
    }

    private ReportDocument alertSummary(ReportFilterCriteria f) {
        List<AlertResponseDTO> alerts = alertRepository.findAll().stream()
                .map(alertMapper::toResponse)
                .filter(dto -> withinRange(dto.createdDate(), f.from(), f.to()))
                .toList();

        Map<String, Long> byType = alerts.stream()
                .collect(Collectors.groupingBy(a -> a.type().name(), Collectors.counting()));
        List<Map<String, Object>> rows = byType.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("type", e.getKey());
                    row.put("count", e.getValue());
                    return row;
                })
                .toList();

        Map<String, Long> bySeverity = alerts.stream()
                .collect(Collectors.groupingBy(a -> a.severity().name(), Collectors.counting()));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", alerts.size());
        summary.put("bySeverity", bySeverity);

        return ReportDocument.builder()
                .reportType(ReportType.ALERT_SUMMARY)
                .reportKind(f.reportKind())
                .title("Alert Summary Report")
                .generatedAt(Instant.now())
                .summary(summary)
                .columns(List.of("type", "count"))
                .rows(rows)
                .build();
    }

    private static boolean withinRange(Instant value, Instant from, Instant to) {
        if (value == null) {
            return from == null && to == null;
        }
        if (from != null && value.isBefore(from)) {
            return false;
        }
        return to == null || !value.isAfter(to);
    }

    private ReportDocument document(ReportType type, ReportKind kind, String title,
                                     List<Map<String, Object>> rows, Map<String, Object> summary) {
        return ReportDocument.builder()
                .reportType(type)
                .reportKind(kind)
                .title(title)
                .generatedAt(Instant.now())
                .summary(summary)
                .columns(columnsOf(rows))
                .rows(rows)
                .build();
    }

    private static List<String> columnsOf(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
    }

    private Map<String, Object> toRow(Object dto) {
        return objectMapper.convertValue(dto, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }
}
