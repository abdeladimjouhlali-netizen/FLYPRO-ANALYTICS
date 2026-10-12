package com.flypro.analytics.leave.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.leave.dto.LeaveRequestCreateDTO;
import com.flypro.analytics.leave.dto.LeaveRequestResponseDTO;
import com.flypro.analytics.leave.dto.LeaveRequestReviewDTO;
import com.flypro.analytics.leave.dto.LeaveRequestSearchCriteria;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Business operations for employee leave requests.
 */
public interface LeaveRequestService {

    LeaveRequestResponseDTO create(Long employeeId, LeaveRequestCreateDTO request);

    PageResponse<LeaveRequestResponseDTO> myRequests(Long employeeId, Pageable pageable);

    void cancel(Long id, Long employeeId);

    PageResponse<LeaveRequestResponseDTO> search(LeaveRequestSearchCriteria criteria, Pageable pageable);

    LeaveRequestResponseDTO getById(Long id);

    LeaveRequestResponseDTO approve(Long id, LeaveRequestReviewDTO review);

    LeaveRequestResponseDTO reject(Long id, LeaveRequestReviewDTO review);

    boolean isEmployeeOnApprovedLeave(Long employeeId, LocalDate date);
}
