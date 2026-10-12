package com.flypro.analytics.dashboard.repository;

import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.dashboard.dto.DashboardFilterCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JPA EntityManager-backed analytics repository.
 * Uses aggregated JPQL to avoid N+1 and entity materialization.
 */
@Repository
public class DashboardRepositoryImpl implements DashboardRepository {

    private final EntityManager entityManager;

    public DashboardRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public long countEmployees(EmployeeStatus status, Long departmentId) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(e) FROM Employee e WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (status != null) {
            jpql.append(" AND e.status = :status");
            params.put("status", status);
        }
        if (departmentId != null) {
            jpql.append(" AND e.department.id = :departmentId");
            params.put("departmentId", departmentId);
        }
        return singleLong(jpql.toString(), params);
    }

    @Override
    public long countEmployees(Long departmentId) {
        return countEmployees(null, departmentId);
    }

    @Override
    public long countDepartments() {
        return singleLong("SELECT COUNT(d) FROM Department d", Map.of());
    }

    @Override
    public long countBadges(BadgeStatus status) {
        return singleLong("SELECT COUNT(b) FROM Badge b WHERE b.status = :status",
                Map.of("status", status));
    }

    @Override
    public long countBadges() {
        return singleLong("SELECT COUNT(b) FROM Badge b", Map.of());
    }

    @Override
    public long countAccessPoints(Long zoneId) {
        if (zoneId == null) {
            return singleLong("SELECT COUNT(ap) FROM AccessPoint ap", Map.of());
        }
        return singleLong("SELECT COUNT(ap) FROM AccessPoint ap WHERE ap.zone.id = :zoneId",
                Map.of("zoneId", zoneId));
    }

    @Override
    public long countZones(Boolean sensitiveOnly) {
        if (sensitiveOnly == null) {
            return singleLong("SELECT COUNT(z) FROM Zone z", Map.of());
        }
        return singleLong("SELECT COUNT(z) FROM Zone z WHERE z.sensitive = :sensitive",
                Map.of("sensitive", sensitiveOnly));
    }

    @Override
    public long countAccessEvents(DashboardFilterCriteria filter, AccessResult result) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(ae) FROM AccessEvent ae WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendAccessEventFilters(jpql, params, filter);
        if (result != null) {
            jpql.append(" AND ae.result = :result");
            params.put("result", result);
        }
        return singleLong(jpql.toString(), params);
    }

    @Override
    public long countAccessEvents(DashboardFilterCriteria filter) {
        return countAccessEvents(filter, null);
    }

    @Override
    public long countAlerts(AlertStatus status, DashboardFilterCriteria filter) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(a) FROM Alert a WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendAlertFilters(jpql, params, filter);
        if (status != null) {
            jpql.append(" AND a.status = :status");
            params.put("status", status);
        }
        return singleLong(jpql.toString(), params);
    }

    @Override
    public long countAlerts(DashboardFilterCriteria filter) {
        return countAlerts(null, filter);
    }

    @Override
    public Map<LocalDate, Long> countAccessEventsByDay(DashboardFilterCriteria filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT CAST(ae.eventTimestamp AS LocalDate), COUNT(ae)
                FROM AccessEvent ae
                WHERE 1=1
                """);
        Map<String, Object> params = new HashMap<>();
        appendAccessEventFilters(jpql, params, filter);
        jpql.append(" GROUP BY CAST(ae.eventTimestamp AS LocalDate) ORDER BY 1");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);

        Map<LocalDate, Long> result = new LinkedHashMap<>();
        for (Object[] row : query.getResultList()) {
            result.put((LocalDate) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public Map<AccessResult, Long> countAccessEventsByResult(DashboardFilterCriteria filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT ae.result, COUNT(ae)
                FROM AccessEvent ae
                WHERE 1=1
                """);
        Map<String, Object> params = new HashMap<>();
        appendAccessEventFilters(jpql, params, filter);
        jpql.append(" GROUP BY ae.result");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);

        Map<AccessResult, Long> result = new EnumMap<>(AccessResult.class);
        for (Object[] row : query.getResultList()) {
            result.put((AccessResult) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public List<NameCountProjection> countAccessesByDepartment(DashboardFilterCriteria filter, int limit) {
        return topNamed("""
                SELECT e.department.id, e.department.name, COUNT(ae)
                FROM AccessEvent ae
                JOIN ae.employee e
                WHERE 1=1
                """, filter, " GROUP BY e.department.id, e.department.name ORDER BY COUNT(ae) DESC", limit);
    }

    @Override
    public List<NameCountProjection> countAccessesByZone(DashboardFilterCriteria filter, int limit) {
        return topNamed("""
                SELECT ae.zone.id, ae.zone.name, COUNT(ae)
                FROM AccessEvent ae
                WHERE 1=1
                """, filter, " GROUP BY ae.zone.id, ae.zone.name ORDER BY COUNT(ae) DESC", limit);
    }

    @Override
    public List<NameCountProjection> countAccessesByAccessPoint(DashboardFilterCriteria filter, int limit) {
        return topNamed("""
                SELECT ae.accessPoint.id, ae.accessPoint.name, COUNT(ae)
                FROM AccessEvent ae
                WHERE 1=1
                """, filter, " GROUP BY ae.accessPoint.id, ae.accessPoint.name ORDER BY COUNT(ae) DESC", limit);
    }

    @Override
    public List<LabelCountProjection> countBadgesByStatus() {
        List<Object[]> rows = entityManager.createQuery(
                "SELECT CAST(b.status AS string), COUNT(b) FROM Badge b GROUP BY b.status",
                Object[].class
        ).getResultList();
        return toLabelCounts(rows);
    }

    @Override
    public List<LabelCountProjection> countEmployeesByStatus(Long departmentId) {
        StringBuilder jpql = new StringBuilder(
                "SELECT CAST(e.status AS string), COUNT(e) FROM Employee e WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (departmentId != null) {
            jpql.append(" AND e.department.id = :departmentId");
            params.put("departmentId", departmentId);
        }
        jpql.append(" GROUP BY e.status");
        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);
        return toLabelCounts(query.getResultList());
    }

    @Override
    public List<LabelCountProjection> countAlertsBySeverity(DashboardFilterCriteria filter) {
        StringBuilder jpql = new StringBuilder(
                "SELECT CAST(a.severity AS string), COUNT(a) FROM Alert a WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendAlertFilters(jpql, params, filter);
        jpql.append(" GROUP BY a.severity");
        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);
        return toLabelCounts(query.getResultList());
    }

    @Override
    public List<LabelCountProjection> countAlertsByType(DashboardFilterCriteria filter) {
        StringBuilder jpql = new StringBuilder(
                "SELECT CAST(a.type AS string), COUNT(a) FROM Alert a WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendAlertFilters(jpql, params, filter);
        jpql.append(" GROUP BY a.type");
        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);
        return toLabelCounts(query.getResultList());
    }

    @Override
    public List<NameCountProjection> topAccessPoints(DashboardFilterCriteria filter, int limit) {
        return countAccessesByAccessPoint(filter, limit);
    }

    @Override
    public List<NameCountProjection> topZones(DashboardFilterCriteria filter, int limit) {
        return countAccessesByZone(filter, limit);
    }

    @Override
    public List<NameCountProjection> topEmployees(DashboardFilterCriteria filter, int limit) {
        return topNamed("""
                SELECT e.id, CONCAT(e.firstName, ' ', e.lastName), COUNT(ae)
                FROM AccessEvent ae
                JOIN ae.employee e
                WHERE 1=1
                """, filter, " GROUP BY e.id, e.firstName, e.lastName ORDER BY COUNT(ae) DESC", limit);
    }

    @Override
    public List<NameCountProjection> employeesWithDeniedAccesses(DashboardFilterCriteria filter, int limit) {
        StringBuilder jpql = new StringBuilder("""
                SELECT e.id, CONCAT(e.firstName, ' ', e.lastName), COUNT(ae)
                FROM AccessEvent ae
                JOIN ae.employee e
                WHERE ae.result = :denied
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("denied", AccessResult.DENIED);
        appendAccessEventFilters(jpql, params, filter);
        jpql.append(" GROUP BY e.id, e.firstName, e.lastName ORDER BY COUNT(ae) DESC");
        return executeNamed(jpql.toString(), params, limit);
    }

    @Override
    public List<NameCountProjection> badgesExpiringSoon(int days, int limit) {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(days);
        List<Object[]> rows = entityManager.createQuery("""
                        SELECT b.id, b.badgeNumber, COUNT(b)
                        FROM Badge b
                        WHERE b.expirationDate IS NOT NULL
                          AND b.expirationDate >= :today
                          AND b.expirationDate <= :until
                          AND b.status <> :revoked
                        GROUP BY b.id, b.badgeNumber
                        ORDER BY MIN(b.expirationDate) ASC
                        """, Object[].class)
                .setParameter("today", today)
                .setParameter("until", until)
                .setParameter("revoked", BadgeStatus.REVOKED)
                .setMaxResults(limit)
                .getResultList();
        return toNamedCounts(rows);
    }

    @Override
    public List<NameCountProjection> departmentsWithMostAccesses(DashboardFilterCriteria filter, int limit) {
        return countAccessesByDepartment(filter, limit);
    }

    @Override
    public List<NameCountProjection> mostFrequentAlertTypes(DashboardFilterCriteria filter, int limit) {
        StringBuilder jpql = new StringBuilder("""
                SELECT 0L, CAST(a.type AS string), COUNT(a)
                FROM Alert a
                WHERE 1=1
                """);
        Map<String, Object> params = new HashMap<>();
        appendAlertFilters(jpql, params, filter);
        jpql.append(" GROUP BY a.type ORDER BY COUNT(a) DESC");
        return executeNamed(jpql.toString(), params, limit);
    }

    @Override
    public List<Instant> findAccessEventTimestamps(DashboardFilterCriteria filter) {
        StringBuilder jpql = new StringBuilder("SELECT ae.eventTimestamp FROM AccessEvent ae WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendAccessEventFilters(jpql, params, filter);
        TypedQuery<Instant> query = entityManager.createQuery(jpql.toString(), Instant.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    private void appendAccessEventFilters(StringBuilder jpql, Map<String, Object> params, DashboardFilterCriteria filter) {
        if (filter == null) {
            return;
        }
        if (filter.from() != null) {
            jpql.append(" AND ae.eventTimestamp >= :from");
            params.put("from", filter.from());
        }
        if (filter.to() != null) {
            jpql.append(" AND ae.eventTimestamp <= :to");
            params.put("to", filter.to());
        }
        if (filter.departmentId() != null) {
            jpql.append(" AND ae.employee.department.id = :departmentId");
            params.put("departmentId", filter.departmentId());
        }
        if (filter.zoneId() != null) {
            jpql.append(" AND ae.zone.id = :zoneId");
            params.put("zoneId", filter.zoneId());
        }
        if (filter.accessPointId() != null) {
            jpql.append(" AND ae.accessPoint.id = :accessPointId");
            params.put("accessPointId", filter.accessPointId());
        }
        if (filter.employeeId() != null) {
            jpql.append(" AND ae.employee.id = :employeeId");
            params.put("employeeId", filter.employeeId());
        }
        if (filter.badgeId() != null) {
            jpql.append(" AND ae.badge.id = :badgeId");
            params.put("badgeId", filter.badgeId());
        }
    }

    private void appendAlertFilters(StringBuilder jpql, Map<String, Object> params, DashboardFilterCriteria filter) {
        if (filter == null) {
            return;
        }
        if (filter.from() != null) {
            jpql.append(" AND a.createdDate >= :from");
            params.put("from", filter.from());
        }
        if (filter.to() != null) {
            jpql.append(" AND a.createdDate <= :to");
            params.put("to", filter.to());
        }
        if (filter.alertSeverity() != null) {
            jpql.append(" AND a.severity = :alertSeverity");
            params.put("alertSeverity", filter.alertSeverity());
        }
        if (filter.alertType() != null) {
            jpql.append(" AND a.type = :alertType");
            params.put("alertType", filter.alertType());
        }
        if (filter.employeeId() != null) {
            jpql.append(" AND a.employee.id = :employeeId");
            params.put("employeeId", filter.employeeId());
        }
        if (filter.badgeId() != null) {
            jpql.append(" AND a.badge.id = :badgeId");
            params.put("badgeId", filter.badgeId());
        }
        if (filter.departmentId() != null) {
            jpql.append(" AND a.employee.department.id = :departmentId");
            params.put("departmentId", filter.departmentId());
        }
        if (filter.zoneId() != null) {
            jpql.append(" AND a.accessEvent.zone.id = :zoneId");
            params.put("zoneId", filter.zoneId());
        }
    }

    private List<NameCountProjection> topNamed(String selectFrom, DashboardFilterCriteria filter, String groupOrder, int limit) {
        StringBuilder jpql = new StringBuilder(selectFrom);
        Map<String, Object> params = new HashMap<>();
        appendAccessEventFilters(jpql, params, filter);
        jpql.append(groupOrder);
        return executeNamed(jpql.toString(), params, limit);
    }

    private List<NameCountProjection> executeNamed(String jpql, Map<String, Object> params, int limit) {
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
        params.forEach(query::setParameter);
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        return toNamedCounts(query.getResultList());
    }

    private long singleLong(String jpql, Map<String, Object> params) {
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        params.forEach(query::setParameter);
        Long value = query.getSingleResult();
        return value == null ? 0L : value;
    }

    private List<NameCountProjection> toNamedCounts(List<Object[]> rows) {
        List<NameCountProjection> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Long id = row[0] instanceof Number n ? n.longValue() : (Long) row[0];
            String name = row[1] == null ? null : String.valueOf(row[1]);
            long count = row[2] instanceof Number n ? n.longValue() : 0L;
            result.add(new SimpleNameCount(id, name, count));
        }
        return result;
    }

    private List<LabelCountProjection> toLabelCounts(List<Object[]> rows) {
        List<LabelCountProjection> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            String label = row[0] == null ? null : String.valueOf(row[0]);
            long count = row[1] instanceof Number n ? n.longValue() : 0L;
            result.add(new SimpleLabelCount(label, count));
        }
        return result;
    }

    private record SimpleNameCount(Long id, String name, Long count) implements NameCountProjection {
        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Long getCount() {
            return count;
        }
    }

    private record SimpleLabelCount(String label, Long count) implements LabelCountProjection {
        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public Long getCount() {
            return count;
        }
    }
}
