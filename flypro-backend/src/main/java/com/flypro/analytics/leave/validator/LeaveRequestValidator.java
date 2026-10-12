package com.flypro.analytics.leave.validator;

import com.flypro.analytics.common.enums.LeaveStatus;
import com.flypro.analytics.leave.dto.LeaveRequestCreateDTO;
import com.flypro.analytics.leave.exception.InvalidLeaveRequestException;
import com.flypro.analytics.leave.repository.LeaveRequestRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.EnumSet;

/**
 * Business validation for leave request creation.
 */
@Component
public class LeaveRequestValidator {

    private static final EnumSet<LeaveStatus> BLOCKING_STATUSES = EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequestValidator(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public LeaveRequestCreateDTO normalize(LeaveRequestCreateDTO request) {
        String reason = StringUtils.hasText(request.reason()) ? request.reason().trim() : null;
        return new LeaveRequestCreateDTO(request.startDate(), request.endDate(), request.type(), reason);
    }

    public void validateCreate(Long employeeId, LeaveRequestCreateDTO request) {
        if (request.startDate() == null || request.endDate() == null) {
            throw new InvalidLeaveRequestException("Start date and end date are required");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidLeaveRequestException("End date cannot be before start date");
        }
        if (request.startDate().isBefore(LocalDate.now())) {
            throw new InvalidLeaveRequestException("Start date cannot be in the past");
        }
        if (leaveRequestRepository.existsOverlapping(
                employeeId, BLOCKING_STATUSES, request.startDate(), request.endDate())) {
            throw new InvalidLeaveRequestException(
                    "This employee already has a pending or approved leave request overlapping these dates");
        }
    }
}
