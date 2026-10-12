package com.flypro.analytics.portal.controller;

import com.flypro.analytics.accessevent.dto.AccessEventResponseDTO;
import com.flypro.analytics.alert.dto.AlertResponseDTO;
import com.flypro.analytics.badge.dto.BadgeResponseDTO;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.common.response.ApiResponse;
import com.flypro.analytics.employee.dto.EmployeeResponseDTO;
import com.flypro.analytics.leave.dto.LeaveRequestCreateDTO;
import com.flypro.analytics.leave.dto.LeaveRequestResponseDTO;
import com.flypro.analytics.portal.service.EmployeePortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Employee Portal", description = "Espace personnel employé")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeePortalController {

    private final EmployeePortalService portalService;

    public EmployeePortalController(EmployeePortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/employee")
    @Operation(summary = "Mon profil employé")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> myEmployee() {
        return ResponseEntity.ok(ApiResponse.ok("Profil employé", portalService.myProfile()));
    }

    @GetMapping("/badges")
    @Operation(summary = "Mes badges")
    public ResponseEntity<ApiResponse<List<BadgeResponseDTO>>> myBadges() {
        return ResponseEntity.ok(ApiResponse.ok("Badges employés", portalService.myBadges()));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Mes alertes")
    public ResponseEntity<ApiResponse<List<AlertResponseDTO>>> myAlerts() {
        return ResponseEntity.ok(ApiResponse.ok("Alertes employés", portalService.myAlerts()));
    }

    @GetMapping("/access-events")
    @Operation(summary = "Mes événements d'accès")
    public ResponseEntity<ApiResponse<PageResponse<AccessEventResponseDTO>>> myAccessEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Événements d'accès", portalService.myAccessEvents(page, size)));
    }

    @GetMapping("/leave-requests")
    @Operation(summary = "Mes demandes de congé")
    public ResponseEntity<ApiResponse<PageResponse<LeaveRequestResponseDTO>>> myLeaveRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Demandes de congé", portalService.myLeaveRequests(page, size)));
    }

    @PostMapping("/leave-requests")
    @Operation(summary = "Demander un congé", description = "Tant que la demande est approuvée et couvre la date du jour, le badge de l'employé est refusé")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDTO>> requestLeave(
            @Valid @RequestBody LeaveRequestCreateDTO request
    ) {
        LeaveRequestResponseDTO created = portalService.requestLeave(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Demande de congé envoyée", created));
    }

    @DeleteMapping("/leave-requests/{id}")
    @Operation(summary = "Annuler une demande de congé en attente")
    public ResponseEntity<ApiResponse<Void>> cancelLeaveRequest(@PathVariable Long id) {
        portalService.cancelLeaveRequest(id);
        return ResponseEntity.ok(ApiResponse.message("Demande de congé annulée"));
    }
}
