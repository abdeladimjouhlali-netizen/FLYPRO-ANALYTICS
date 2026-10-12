package com.flypro.analytics.accessevent.service;

import com.flypro.analytics.accessevent.dto.AccessEventRequestDTO;
import com.flypro.analytics.accessevent.dto.AccessEventResponseDTO;
import com.flypro.analytics.accessevent.dto.AccessEventSearchCriteria;
import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accessevent.exception.AccessEventNotFoundException;
import com.flypro.analytics.accessevent.exception.InvalidAccessEventException;
import com.flypro.analytics.accessevent.mapper.AccessEventMapper;
import com.flypro.analytics.accessevent.repository.AccessEventRepository;
import com.flypro.analytics.accessevent.specification.AccessEventSpecification;
import com.flypro.analytics.accessevent.validator.AccessEventValidator;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.accesspoint.exception.AccessPointNotFoundException;
import com.flypro.analytics.accesspoint.repository.AccessPointRepository;
import com.flypro.analytics.alert.engine.AlertRuleEngine;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badge.exception.BadgeNotFoundException;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import com.flypro.analytics.badgeassignment.repository.BadgeAssignmentRepository;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.leave.service.LeaveRequestService;
import com.flypro.analytics.zone.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Default implementation of {@link AccessEventService}.
 * Access events are append-only audit records.
 */
@Service
@Transactional
public class AccessEventServiceImpl implements AccessEventService {

    private final AccessEventRepository accessEventRepository;
    private final BadgeRepository badgeRepository;
    private final AccessPointRepository accessPointRepository;
    private final BadgeAssignmentRepository badgeAssignmentRepository;
    private final AccessEventMapper accessEventMapper;
    private final AccessEventValidator accessEventValidator;
    private final AlertRuleEngine alertRuleEngine;
    private final LeaveRequestService leaveRequestService;

    public AccessEventServiceImpl(AccessEventRepository accessEventRepository,
                                  BadgeRepository badgeRepository,
                                  AccessPointRepository accessPointRepository,
                                  BadgeAssignmentRepository badgeAssignmentRepository,
                                  AccessEventMapper accessEventMapper,
                                  AccessEventValidator accessEventValidator,
                                  AlertRuleEngine alertRuleEngine,
                                  LeaveRequestService leaveRequestService) {
        this.accessEventRepository = accessEventRepository;
        this.badgeRepository = badgeRepository;
        this.accessPointRepository = accessPointRepository;
        this.badgeAssignmentRepository = badgeAssignmentRepository;
        this.accessEventMapper = accessEventMapper;
        this.accessEventValidator = accessEventValidator;
        this.alertRuleEngine = alertRuleEngine;
        this.leaveRequestService = leaveRequestService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccessEventResponseDTO> search(AccessEventSearchCriteria criteria, Pageable pageable) {
        Specification<AccessEvent> specification = AccessEventSpecification.withCriteria(criteria);
        Page<AccessEventResponseDTO> page = accessEventRepository.findAll(specification, pageable)
                .map(accessEventMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public AccessEventResponseDTO getById(Long id) {
        return accessEventMapper.toResponse(findEvent(id));
    }

    @Override
    public AccessEventResponseDTO create(AccessEventRequestDTO request) {
        AccessEventRequestDTO normalized = accessEventValidator.normalize(request);
        accessEventValidator.validateCreate(normalized);

        Badge badge = badgeRepository.findById(normalized.badgeId())
                .orElseThrow(() -> new BadgeNotFoundException(normalized.badgeId()));
        applyBadgeExpiration(badge);

        AccessPoint accessPoint = accessPointRepository.findById(normalized.accessPointId())
                .orElseThrow(() -> new AccessPointNotFoundException(normalized.accessPointId()));

        Zone zone = resolveZone(normalized.zoneId(), accessPoint);

        Employee employee = badgeAssignmentRepository.findByBadgeIdAndActiveTrue(badge.getId())
                .map(BadgeAssignment::getEmployee)
                .orElse(null);

        AccessResultDecision decision = resolveResult(badge, normalized);
        if (employee != null && isOnApprovedLeave(employee, normalized)) {
            decision = new AccessResultDecision(AccessResult.DENIED, "Employee is on approved leave");
        }

        AccessEvent event = accessEventMapper.toEntity(normalized);
        event.setBadge(badge);
        event.setAccessPoint(accessPoint);
        event.setZone(zone);
        event.setEmployee(employee);
        event.setResult(decision.result());
        event.setRefusalReason(decision.refusalReason());

        AccessEvent saved = accessEventRepository.save(event);
        alertRuleEngine.evaluateAccessEvent(saved);
        return accessEventMapper.toResponse(saved);
    }

    private Zone resolveZone(Long requestedZoneId, AccessPoint accessPoint) {
        Zone accessPointZone = accessPoint.getZone();
        if (accessPointZone == null) {
            throw new InvalidAccessEventException("Access point has no associated zone");
        }
        if (requestedZoneId != null && !requestedZoneId.equals(accessPointZone.getId())) {
            throw new InvalidAccessEventException(
                    "Zone " + requestedZoneId + " does not match access point zone "
                            + accessPointZone.getId()
            );
        }
        return accessPointZone;
    }

    private AccessResultDecision resolveResult(Badge badge, AccessEventRequestDTO request) {
        BadgeStatus status = badge.getStatus();

        return switch (status) {
            case EXPIRED -> new AccessResultDecision(
                    AccessResult.EXPIRED,
                    firstNonBlank(request.refusalReason(), "Badge is expired")
            );
            case REVOKED -> new AccessResultDecision(
                    AccessResult.DENIED,
                    firstNonBlank(request.refusalReason(), "Badge is revoked")
            );
            case SUSPENDED -> new AccessResultDecision(
                    AccessResult.DENIED,
                    firstNonBlank(request.refusalReason(), "Badge is suspended")
            );
            case LOST -> new AccessResultDecision(
                    AccessResult.DENIED,
                    firstNonBlank(request.refusalReason(), "Badge is reported lost")
            );
            case PENDING_ACTIVATION -> new AccessResultDecision(
                    AccessResult.DENIED,
                    firstNonBlank(request.refusalReason(), "Badge is pending activation")
            );
            case ACTIVE -> new AccessResultDecision(
                    request.result(),
                    request.result() == AccessResult.AUTHORIZED ? null : request.refusalReason()
            );
        };
    }

    private boolean isOnApprovedLeave(Employee employee, AccessEventRequestDTO request) {
        LocalDate eventDate = request.eventTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
        return leaveRequestService.isEmployeeOnApprovedLeave(employee.getId(), eventDate);
    }

    private void applyBadgeExpiration(Badge badge) {
        if (badge.getStatus() != BadgeStatus.EXPIRED
                && badge.getStatus() != BadgeStatus.REVOKED
                && badge.getExpirationDate() != null
                && badge.getExpirationDate().isBefore(LocalDate.now())) {
            badge.setStatus(BadgeStatus.EXPIRED);
            badgeRepository.save(badge);
        }
    }

    private AccessEvent findEvent(Long id) {
        return accessEventRepository.findById(id)
                .orElseThrow(() -> new AccessEventNotFoundException(id));
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private record AccessResultDecision(AccessResult result, String refusalReason) {
    }
}
