package com.flypro.analytics.badge.service;

import com.flypro.analytics.badge.dto.BadgeAssignmentDTO;
import com.flypro.analytics.badge.dto.BadgeRenewRequestDTO;
import com.flypro.analytics.badge.dto.BadgeReplaceRequestDTO;
import com.flypro.analytics.badge.dto.BadgeRequestDTO;
import com.flypro.analytics.badge.dto.BadgeResponseDTO;
import com.flypro.analytics.badge.dto.BadgeSearchCriteria;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badge.exception.BadgeAlreadyAssignedException;
import com.flypro.analytics.badge.exception.BadgeAlreadyRevokedException;
import com.flypro.analytics.badge.exception.BadgeCannotBeDeletedException;
import com.flypro.analytics.badge.exception.BadgeExpiredException;
import com.flypro.analytics.badge.exception.BadgeNotAssignedException;
import com.flypro.analytics.badge.exception.BadgeNotFoundException;
import com.flypro.analytics.badge.exception.BadgeSuspendedException;
import com.flypro.analytics.badge.exception.EmployeeInactiveException;
import com.flypro.analytics.badge.exception.InvalidBadgeDatesException;
import com.flypro.analytics.badge.mapper.BadgeMapper;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.badge.specification.BadgeSpecification;
import com.flypro.analytics.badge.validator.BadgeValidator;
import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import com.flypro.analytics.badgeassignment.repository.BadgeAssignmentRepository;
import com.flypro.analytics.alert.engine.AlertRuleEngine;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.employee.exception.EmployeeNotFoundException;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Default implementation of {@link BadgeService}.
 */
@Service
@Transactional
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final BadgeAssignmentRepository badgeAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final BadgeMapper badgeMapper;
    private final BadgeValidator badgeValidator;
    private final AlertRuleEngine alertRuleEngine;

    public BadgeServiceImpl(BadgeRepository badgeRepository,
                            BadgeAssignmentRepository badgeAssignmentRepository,
                            EmployeeRepository employeeRepository,
                            BadgeMapper badgeMapper,
                            BadgeValidator badgeValidator,
                            AlertRuleEngine alertRuleEngine) {
        this.badgeRepository = badgeRepository;
        this.badgeAssignmentRepository = badgeAssignmentRepository;
        this.employeeRepository = employeeRepository;
        this.badgeMapper = badgeMapper;
        this.badgeValidator = badgeValidator;
        this.alertRuleEngine = alertRuleEngine;
    }

    @Override
    public PageResponse<BadgeResponseDTO> search(BadgeSearchCriteria criteria, Pageable pageable) {
        expireOverdueBadges();
        Specification<Badge> specification = BadgeSpecification.withCriteria(criteria);
        Page<BadgeResponseDTO> page = badgeRepository.findAll(specification, pageable)
                .map(this::toResponseWithAssignment);
        return PageResponse.from(page);
    }

    @Override
    public BadgeResponseDTO getById(Long id) {
        Badge badge = findBadge(id);
        applyExpirationIfNeeded(badge);
        return toResponseWithAssignment(badge);
    }

    @Override
    public BadgeResponseDTO create(BadgeRequestDTO request) {
        BadgeRequestDTO normalized = badgeValidator.normalize(request);
        badgeValidator.validateCreate(normalized);

        Badge badge = badgeMapper.toEntity(normalized);
        Badge saved = badgeRepository.save(badge);
        return toResponseWithAssignment(saved);
    }

    @Override
    public BadgeResponseDTO update(Long id, BadgeRequestDTO request) {
        Badge badge = findBadge(id);
        BadgeRequestDTO normalized = badgeValidator.normalize(request);
        badgeValidator.validateUpdate(id, normalized);

        if (badge.getStatus() == BadgeStatus.REVOKED && normalized.status() == BadgeStatus.ACTIVE) {
            throw new BadgeAlreadyRevokedException(id);
        }

        badgeMapper.updateEntity(normalized, badge);
        applyExpirationIfNeeded(badge);
        enforceActiveConstraints(badge);

        return toResponseWithAssignment(badgeRepository.save(badge));
    }

    @Override
    public void delete(Long id) {
        Badge badge = findBadge(id);
        if (badgeRepository.hasAnyAssignment(id)) {
            throw new BadgeCannotBeDeletedException(
                    "Badge cannot be deleted because it has assignment history"
            );
        }
        badgeRepository.delete(badge);
    }

    @Override
    public BadgeResponseDTO assign(Long badgeId, Long employeeId) {
        Badge badge = findBadge(badgeId);
        applyExpirationIfNeeded(badge);
        assertAssignable(badge);

        Employee employee = findEmployee(employeeId);
        assertEmployeeActive(employee);

        if (badgeAssignmentRepository.existsByBadgeIdAndActiveTrue(badgeId)) {
            throw new BadgeAlreadyAssignedException(badgeId);
        }
        if (badgeAssignmentRepository.existsByEmployeeIdAndActiveTrue(employeeId)) {
            throw new BadgeAlreadyAssignedException(
                    "Employee already has an active badge assignment: " + employeeId
            );
        }

        BadgeAssignment assignment = BadgeAssignment.builder()
                .badge(badge)
                .employee(employee)
                .assignedAt(Instant.now())
                .returnedAt(null)
                .active(true)
                .build();
        badgeAssignmentRepository.save(assignment);

        if (badge.getStatus() == BadgeStatus.PENDING_ACTIVATION) {
            badge.setStatus(BadgeStatus.ACTIVE);
        }
        enforceActiveConstraints(badge);
        badgeRepository.save(badge);

        return toResponseWithAssignment(badge);
    }

    @Override
    public BadgeResponseDTO unassign(Long badgeId) {
        Badge badge = findBadge(badgeId);
        BadgeAssignment assignment = badgeAssignmentRepository.findByBadgeIdAndActiveTrue(badgeId)
                .orElseThrow(() -> new BadgeNotAssignedException(badgeId));

        closeAssignment(assignment);

        if (badge.getStatus() == BadgeStatus.ACTIVE) {
            badge.setStatus(BadgeStatus.PENDING_ACTIVATION);
            badgeRepository.save(badge);
        }

        return toResponseWithAssignment(badge);
    }

    @Override
    public BadgeResponseDTO renew(Long badgeId, BadgeRenewRequestDTO request) {
        Badge badge = findBadge(badgeId);
        if (badge.getStatus() == BadgeStatus.REVOKED) {
            throw new BadgeAlreadyRevokedException(badgeId);
        }

        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : LocalDate.now();
        badgeValidator.validateDates(issueDate, request.expirationDate());

        badge.setIssueDate(issueDate);
        badge.setExpirationDate(request.expirationDate());

        if (isPastExpiration(badge)) {
            badge.setStatus(BadgeStatus.EXPIRED);
        } else if (badge.getStatus() == BadgeStatus.EXPIRED) {
            badge.setStatus(BadgeStatus.PENDING_ACTIVATION);
        }

        return toResponseWithAssignment(badgeRepository.save(badge));
    }

    @Override
    public BadgeResponseDTO replace(Long badgeId, BadgeReplaceRequestDTO request) {
        if (request.newBadgeId().equals(badgeId)) {
            throw new InvalidBadgeDatesException("Replacement badge must be different from the current badge");
        }

        Badge current = findBadge(badgeId);
        BadgeAssignment active = badgeAssignmentRepository.findByBadgeIdAndActiveTrue(badgeId)
                .orElseThrow(() -> new BadgeNotAssignedException(badgeId));

        Employee employee = active.getEmployee();
        assertEmployeeActive(employee);

        Badge replacement = findBadge(request.newBadgeId());
        applyExpirationIfNeeded(replacement);
        assertAssignable(replacement);

        if (badgeAssignmentRepository.existsByBadgeIdAndActiveTrue(replacement.getId())) {
            throw new BadgeAlreadyAssignedException(replacement.getId());
        }

        closeAssignment(active);
        if (current.getStatus() == BadgeStatus.ACTIVE) {
            current.setStatus(BadgeStatus.PENDING_ACTIVATION);
            badgeRepository.save(current);
        }

        BadgeAssignment newAssignment = BadgeAssignment.builder()
                .badge(replacement)
                .employee(employee)
                .assignedAt(Instant.now())
                .returnedAt(null)
                .active(true)
                .build();
        badgeAssignmentRepository.save(newAssignment);

        if (replacement.getStatus() == BadgeStatus.PENDING_ACTIVATION
                || replacement.getStatus() == BadgeStatus.ACTIVE) {
            replacement.setStatus(BadgeStatus.ACTIVE);
        }
        enforceActiveConstraints(replacement);
        badgeRepository.save(replacement);

        return toResponseWithAssignment(replacement);
    }

    @Override
    public BadgeResponseDTO suspend(Long badgeId) {
        Badge badge = findBadge(badgeId);
        applyExpirationIfNeeded(badge);

        if (badge.getStatus() == BadgeStatus.REVOKED) {
            throw new BadgeAlreadyRevokedException(badgeId);
        }
        if (badge.getStatus() == BadgeStatus.EXPIRED) {
            throw new BadgeExpiredException(badgeId);
        }
        if (badge.getStatus() == BadgeStatus.LOST) {
            throw new BadgeSuspendedException(badgeId);
        }

        badge.setStatus(BadgeStatus.SUSPENDED);
        Badge saved = badgeRepository.save(badge);
        alertRuleEngine.evaluateBadgeStatusChange(saved, resolveAssignedEmployee(saved.getId()));
        return toResponseWithAssignment(saved);
    }

    @Override
    public BadgeResponseDTO activate(Long badgeId) {
        Badge badge = findBadge(badgeId);
        applyExpirationIfNeeded(badge);

        if (badge.getStatus() == BadgeStatus.REVOKED) {
            throw new BadgeAlreadyRevokedException(badgeId);
        }
        if (badge.getStatus() == BadgeStatus.EXPIRED) {
            throw new BadgeExpiredException(badgeId);
        }
        if (badge.getStatus() == BadgeStatus.LOST) {
            throw new BadgeSuspendedException(badgeId);
        }

        badgeAssignmentRepository.findByBadgeIdAndActiveTrue(badgeId).ifPresent(assignment -> {
            if (assignment.getEmployee().getStatus() != EmployeeStatus.ACTIVE) {
                throw new EmployeeInactiveException(assignment.getEmployee().getId());
            }
        });

        badge.setStatus(BadgeStatus.ACTIVE);
        enforceActiveConstraints(badge);
        return toResponseWithAssignment(badgeRepository.save(badge));
    }

    @Override
    public BadgeResponseDTO revoke(Long badgeId) {
        Badge badge = findBadge(badgeId);
        if (badge.getStatus() == BadgeStatus.REVOKED) {
            throw new BadgeAlreadyRevokedException(badgeId);
        }

        Employee previousEmployee = resolveAssignedEmployee(badgeId);
        badgeAssignmentRepository.findByBadgeIdAndActiveTrue(badgeId)
                .ifPresent(this::closeAssignment);

        badge.setStatus(BadgeStatus.REVOKED);
        Badge saved = badgeRepository.save(badge);
        alertRuleEngine.evaluateBadgeStatusChange(saved, previousEmployee);
        return toResponseWithAssignment(saved);
    }

    @Override
    public BadgeResponseDTO reportLost(Long badgeId) {
        Badge badge = findBadge(badgeId);
        if (badge.getStatus() == BadgeStatus.REVOKED) {
            throw new BadgeAlreadyRevokedException(badgeId);
        }

        badgeAssignmentRepository.findByBadgeIdAndActiveTrue(badgeId)
                .ifPresent(this::closeAssignment);

        badge.setStatus(BadgeStatus.LOST);
        return toResponseWithAssignment(badgeRepository.save(badge));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeAssignmentDTO> getHistory(Long badgeId) {
        findBadge(badgeId);
        return badgeAssignmentRepository.findHistoryByBadgeId(badgeId).stream()
                .map(badgeMapper::toAssignmentDto)
                .toList();
    }

    @Override
    public List<BadgeResponseDTO> listEmployeeBadges(Long employeeId) {
        findEmployee(employeeId);
        expireOverdueBadges();

        return badgeAssignmentRepository.findAllByEmployeeIdWithDetails(employeeId).stream()
                .map(BadgeAssignment::getBadge)
                .collect(java.util.stream.Collectors.toMap(
                        Badge::getId,
                        badge -> badge,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .map(badge -> {
                    applyExpirationIfNeeded(badge);
                    return toResponseWithAssignment(badge);
                })
                .toList();
    }

    private BadgeResponseDTO toResponseWithAssignment(Badge badge) {
        BadgeAssignment current = badgeAssignmentRepository
                .findByBadgeIdAndActiveTrue(badge.getId())
                .orElse(null);
        return badgeMapper.toResponse(badge, current);
    }

    private Employee resolveAssignedEmployee(Long badgeId) {
        return badgeAssignmentRepository.findByBadgeIdAndActiveTrue(badgeId)
                .map(BadgeAssignment::getEmployee)
                .orElse(null);
    }

    private void closeAssignment(BadgeAssignment assignment) {
        assignment.setActive(false);
        assignment.setReturnedAt(Instant.now());
        badgeAssignmentRepository.save(assignment);
    }

    private void assertAssignable(Badge badge) {
        if (badge.getStatus() == BadgeStatus.REVOKED) {
            throw new BadgeAlreadyRevokedException(badge.getId());
        }
        if (badge.getStatus() == BadgeStatus.EXPIRED) {
            throw new BadgeExpiredException(badge.getId());
        }
        if (badge.getStatus() == BadgeStatus.SUSPENDED) {
            throw new BadgeSuspendedException(badge.getId());
        }
        if (badge.getStatus() == BadgeStatus.LOST) {
            throw new BadgeSuspendedException(badge.getId());
        }
    }

    private void assertEmployeeActive(Employee employee) {
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EmployeeInactiveException(employee.getId());
        }
    }

    /**
     * A badge cannot remain ACTIVE when expired, revoked, suspended, or assigned to an inactive employee.
     */
    private void enforceActiveConstraints(Badge badge) {
        if (badge.getStatus() != BadgeStatus.ACTIVE) {
            return;
        }
        if (isPastExpiration(badge)) {
            badge.setStatus(BadgeStatus.EXPIRED);
            return;
        }
        if (badge.getStatus() == BadgeStatus.REVOKED || badge.getStatus() == BadgeStatus.SUSPENDED) {
            return;
        }
        badgeAssignmentRepository.findByBadgeIdAndActiveTrue(badge.getId()).ifPresent(assignment -> {
            if (assignment.getEmployee().getStatus() != EmployeeStatus.ACTIVE) {
                badge.setStatus(BadgeStatus.SUSPENDED);
            }
        });
    }

    private void applyExpirationIfNeeded(Badge badge) {
        if (badge.getStatus() != BadgeStatus.EXPIRED
                && badge.getStatus() != BadgeStatus.REVOKED
                && isPastExpiration(badge)) {
            badge.setStatus(BadgeStatus.EXPIRED);
            badgeRepository.save(badge);
        }
    }

    private void expireOverdueBadges() {
        List<Badge> candidates = badgeRepository.findExpiredCandidates(LocalDate.now());
        for (Badge badge : candidates) {
            badge.setStatus(BadgeStatus.EXPIRED);
        }
        if (!candidates.isEmpty()) {
            badgeRepository.saveAll(candidates);
        }
    }

    private boolean isPastExpiration(Badge badge) {
        return badge.getExpirationDate() != null
                && badge.getExpirationDate().isBefore(LocalDate.now());
    }

    private Badge findBadge(Long id) {
        return badgeRepository.findById(id)
                .orElseThrow(() -> new BadgeNotFoundException(id));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }
}
