package com.flypro.analytics.portal.service;

import com.flypro.analytics.accessevent.dto.AccessEventResponseDTO;
import com.flypro.analytics.accessevent.dto.AccessEventSearchCriteria;
import com.flypro.analytics.accessevent.service.AccessEventService;
import com.flypro.analytics.alert.dto.AlertResponseDTO;
import com.flypro.analytics.alert.service.AlertService;
import com.flypro.analytics.badge.dto.BadgeResponseDTO;
import com.flypro.analytics.badge.service.BadgeService;
import com.flypro.analytics.common.exceptions.BadRequestException;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.employee.dto.EmployeeResponseDTO;
import com.flypro.analytics.employee.service.EmployeeService;
import com.flypro.analytics.leave.dto.LeaveRequestCreateDTO;
import com.flypro.analytics.leave.dto.LeaveRequestResponseDTO;
import com.flypro.analytics.leave.service.LeaveRequestService;
import com.flypro.analytics.security.userdetails.UserPrincipal;
import com.flypro.analytics.user.entity.User;
import com.flypro.analytics.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only façade backing the employee self-service portal ({@code /api/v1/me/*}).
 * Resolves the linked employee from the authenticated {@link User} and delegates
 * to the existing domain services, scoped to that employee.
 */
@Service
public class EmployeePortalService {

    private final UserRepository userRepository;
    private final EmployeeService employeeService;
    private final BadgeService badgeService;
    private final AlertService alertService;
    private final AccessEventService accessEventService;
    private final LeaveRequestService leaveRequestService;

    public EmployeePortalService(UserRepository userRepository,
                                  EmployeeService employeeService,
                                  BadgeService badgeService,
                                  AlertService alertService,
                                  AccessEventService accessEventService,
                                  LeaveRequestService leaveRequestService) {
        this.userRepository = userRepository;
        this.employeeService = employeeService;
        this.badgeService = badgeService;
        this.alertService = alertService;
        this.accessEventService = accessEventService;
        this.leaveRequestService = leaveRequestService;
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDTO myProfile() {
        return employeeService.getById(currentEmployeeId());
    }

    @Transactional(readOnly = true)
    public List<BadgeResponseDTO> myBadges() {
        return badgeService.listEmployeeBadges(currentEmployeeId());
    }

    @Transactional(readOnly = true)
    public List<AlertResponseDTO> myAlerts() {
        return alertService.findByEmployee(currentEmployeeId());
    }

    @Transactional(readOnly = true)
    public PageResponse<AccessEventResponseDTO> myAccessEvents(int page, int size) {
        AccessEventSearchCriteria criteria = new AccessEventSearchCriteria(
                null, currentEmployeeId(), null, null, null, null, null, null, null, null, null);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventTimestamp"));
        return accessEventService.search(criteria, pageable);
    }

    @Transactional
    public LeaveRequestResponseDTO requestLeave(LeaveRequestCreateDTO request) {
        return leaveRequestService.create(currentEmployeeId(), request);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponseDTO> myLeaveRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return leaveRequestService.myRequests(currentEmployeeId(), pageable);
    }

    @Transactional
    public void cancelLeaveRequest(Long leaveRequestId) {
        leaveRequestService.cancel(leaveRequestId, currentEmployeeId());
    }

    private Long currentEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("No authenticated user");
        }
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BadRequestException("Authenticated user not found"));
        Long employeeId = user.getEmployeeId();
        if (employeeId == null) {
            throw new BadRequestException("This account is not linked to an employee profile");
        }
        return employeeId;
    }
}
