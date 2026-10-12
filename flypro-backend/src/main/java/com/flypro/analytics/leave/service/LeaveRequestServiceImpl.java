package com.flypro.analytics.leave.service;

import com.flypro.analytics.common.enums.LeaveStatus;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.employee.exception.EmployeeNotFoundException;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import com.flypro.analytics.leave.dto.LeaveRequestCreateDTO;
import com.flypro.analytics.leave.dto.LeaveRequestResponseDTO;
import com.flypro.analytics.leave.dto.LeaveRequestReviewDTO;
import com.flypro.analytics.leave.dto.LeaveRequestSearchCriteria;
import com.flypro.analytics.leave.entity.LeaveRequest;
import com.flypro.analytics.leave.exception.InvalidLeaveRequestException;
import com.flypro.analytics.leave.exception.LeaveRequestAlreadyReviewedException;
import com.flypro.analytics.leave.exception.LeaveRequestNotFoundException;
import com.flypro.analytics.leave.mapper.LeaveRequestMapper;
import com.flypro.analytics.leave.repository.LeaveRequestRepository;
import com.flypro.analytics.leave.specification.LeaveRequestSpecification;
import com.flypro.analytics.leave.validator.LeaveRequestValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Default implementation of {@link LeaveRequestService}.
 */
@Service
@Transactional
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeaveRequestValidator leaveRequestValidator;

    public LeaveRequestServiceImpl(LeaveRequestRepository leaveRequestRepository,
                                    EmployeeRepository employeeRepository,
                                    LeaveRequestMapper leaveRequestMapper,
                                    LeaveRequestValidator leaveRequestValidator) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRequestMapper = leaveRequestMapper;
        this.leaveRequestValidator = leaveRequestValidator;
    }

    @Override
    public LeaveRequestResponseDTO create(Long employeeId, LeaveRequestCreateDTO request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        LeaveRequestCreateDTO normalized = leaveRequestValidator.normalize(request);
        leaveRequestValidator.validateCreate(employeeId, normalized);

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .startDate(normalized.startDate())
                .endDate(normalized.endDate())
                .type(normalized.type())
                .reason(normalized.reason())
                .status(LeaveStatus.PENDING)
                .build();

        return leaveRequestMapper.toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponseDTO> myRequests(Long employeeId, Pageable pageable) {
        Page<LeaveRequestResponseDTO> page = leaveRequestRepository.findByEmployeeId(employeeId, pageable)
                .map(leaveRequestMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public void cancel(Long id, Long employeeId) {
        LeaveRequest leaveRequest = findEntity(id);
        if (!leaveRequest.getEmployee().getId().equals(employeeId)) {
            throw new LeaveRequestNotFoundException(id);
        }
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException("Only pending leave requests can be cancelled");
        }
        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepository.save(leaveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponseDTO> search(LeaveRequestSearchCriteria criteria, Pageable pageable) {
        Specification<LeaveRequest> specification = LeaveRequestSpecification.withCriteria(criteria);
        Page<LeaveRequestResponseDTO> page = leaveRequestRepository.findAll(specification, pageable)
                .map(leaveRequestMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestResponseDTO getById(Long id) {
        return leaveRequestMapper.toResponse(findEntity(id));
    }

    @Override
    public LeaveRequestResponseDTO approve(Long id, LeaveRequestReviewDTO review) {
        LeaveRequest leaveRequest = findEntity(id);
        requirePending(leaveRequest);
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setReviewComment(review == null ? null : review.comment());
        return leaveRequestMapper.toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Override
    public LeaveRequestResponseDTO reject(Long id, LeaveRequestReviewDTO review) {
        LeaveRequest leaveRequest = findEntity(id);
        requirePending(leaveRequest);
        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setReviewComment(review == null ? null : review.comment());
        return leaveRequestMapper.toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmployeeOnApprovedLeave(Long employeeId, LocalDate date) {
        if (employeeId == null || date == null) {
            return false;
        }
        return leaveRequestRepository.existsByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employeeId, LeaveStatus.APPROVED, date, date);
    }

    private void requirePending(LeaveRequest leaveRequest) {
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveRequestAlreadyReviewedException(leaveRequest.getId());
        }
    }

    private LeaveRequest findEntity(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException(id));
    }
}
