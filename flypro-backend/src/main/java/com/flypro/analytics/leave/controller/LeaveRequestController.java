package com.flypro.analytics.leave.controller;

import com.flypro.analytics.common.enums.LeaveStatus;
import com.flypro.analytics.common.enums.LeaveType;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.leave.dto.LeaveRequestResponseDTO;
import com.flypro.analytics.leave.dto.LeaveRequestReviewDTO;
import com.flypro.analytics.leave.dto.LeaveRequestSearchCriteria;
import com.flypro.analytics.leave.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST API for administering employee leave requests.
 */
@RestController
@RequestMapping("/api/v1/leave-requests")
@Tag(name = "Leave Requests", description = "Employee leave request review and approval")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping
    @Operation(summary = "Search leave requests")
    public ResponseEntity<ApiResponse<PageResponse<LeaveRequestResponseDTO>>> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) LeaveType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        LeaveRequestSearchCriteria criteria = new LeaveRequestSearchCriteria(
                employeeId, departmentId, status, type, from, to, keyword
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Leave requests retrieved successfully",
                leaveRequestService.search(criteria, pageable)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave request by id")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(leaveRequestService.getById(id)));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve leave request", description = "While an approved leave covers the current date, the employee's badge access is denied")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDTO>> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) LeaveRequestReviewDTO review
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Leave request approved successfully",
                leaveRequestService.approve(id, review)
        ));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject leave request")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDTO>> reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) LeaveRequestReviewDTO review
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Leave request rejected successfully",
                leaveRequestService.reject(id, review)
        ));
    }
}
