package com.flypro.analytics.alert.service;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accessevent.repository.AccessEventRepository;
import com.flypro.analytics.alert.dto.AlertRequestDTO;
import com.flypro.analytics.alert.dto.AlertResponseDTO;
import com.flypro.analytics.alert.dto.AlertSearchCriteria;
import com.flypro.analytics.alert.entity.Alert;
import com.flypro.analytics.alert.exception.AlertNotFoundException;
import com.flypro.analytics.alert.mapper.AlertMapper;
import com.flypro.analytics.alert.repository.AlertRepository;
import com.flypro.analytics.alert.specification.AlertSpecification;
import com.flypro.analytics.alert.validator.AlertValidator;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badge.exception.BadgeNotFoundException;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.employee.exception.EmployeeNotFoundException;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Default implementation of {@link AlertService}.
 * Alerts are never deleted; lifecycle is managed via status transitions.
 */
@Service
@Transactional
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final EmployeeRepository employeeRepository;
    private final BadgeRepository badgeRepository;
    private final AccessEventRepository accessEventRepository;
    private final AlertMapper alertMapper;
    private final AlertValidator alertValidator;

    public AlertServiceImpl(AlertRepository alertRepository,
                            EmployeeRepository employeeRepository,
                            BadgeRepository badgeRepository,
                            AccessEventRepository accessEventRepository,
                            AlertMapper alertMapper,
                            AlertValidator alertValidator) {
        this.alertRepository = alertRepository;
        this.employeeRepository = employeeRepository;
        this.badgeRepository = badgeRepository;
        this.accessEventRepository = accessEventRepository;
        this.alertMapper = alertMapper;
        this.alertValidator = alertValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AlertResponseDTO> search(AlertSearchCriteria criteria, Pageable pageable) {
        Specification<Alert> specification = AlertSpecification.withCriteria(criteria);
        Page<AlertResponseDTO> page = alertRepository.findAll(specification, pageable)
                .map(alertMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertResponseDTO getById(Long id) {
        return alertMapper.toResponse(findAlert(id));
    }

    @Override
    public AlertResponseDTO create(AlertRequestDTO request) {
        alertValidator.validateCreate(request);

        Alert alert = alertMapper.toEntity(request);
        alert.setStatus(AlertStatus.OPEN);
        alert.setCreatedDate(Instant.now());

        if (request.employeeId() != null) {
            Employee employee = employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException(request.employeeId()));
            alert.setEmployee(employee);
        }
        if (request.badgeId() != null) {
            Badge badge = badgeRepository.findById(request.badgeId())
                    .orElseThrow(() -> new BadgeNotFoundException(request.badgeId()));
            alert.setBadge(badge);
        }
        if (request.accessEventId() != null) {
            AccessEvent event = accessEventRepository.findById(request.accessEventId())
                    .orElseThrow(() -> new com.flypro.analytics.accessevent.exception.AccessEventNotFoundException(
                            request.accessEventId()));
            alert.setAccessEvent(event);
        }

        return alertMapper.toResponse(alertRepository.save(alert));
    }

    @Override
    public AlertResponseDTO acknowledge(Long id) {
        Alert alert = findAlert(id);
        alertValidator.validateAcknowledge(alert);
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        return alertMapper.toResponse(alertRepository.save(alert));
    }

    @Override
    public AlertResponseDTO resolve(Long id) {
        Alert alert = findAlert(id);
        alertValidator.validateResolve(alert);
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedDate(Instant.now());
        alert.setResolvedBy(currentUsername());
        return alertMapper.toResponse(alertRepository.save(alert));
    }

    @Override
    public AlertResponseDTO close(Long id) {
        Alert alert = findAlert(id);
        alertValidator.validateClose(alert);
        if (alert.getResolvedDate() == null) {
            alert.setResolvedDate(Instant.now());
            alert.setResolvedBy(currentUsername());
        }
        alert.setStatus(AlertStatus.CLOSED);
        return alertMapper.toResponse(alertRepository.save(alert));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponseDTO> findByEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
        return alertRepository.findByEmployeeIdOrderByCreatedDateDesc(employeeId).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponseDTO> findByBadge(Long badgeId) {
        if (!badgeRepository.existsById(badgeId)) {
            throw new BadgeNotFoundException(badgeId);
        }
        return alertRepository.findByBadgeIdOrderByCreatedDateDesc(badgeId).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponseDTO> findBySeverity(AlertSeverity severity) {
        return alertRepository.findBySeverityOrderByCreatedDateDesc(severity).stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    private Alert findAlert(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        return authentication.getName();
    }
}
