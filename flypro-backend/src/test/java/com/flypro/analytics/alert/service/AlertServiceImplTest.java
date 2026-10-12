package com.flypro.analytics.alert.service;

import com.flypro.analytics.accessevent.repository.AccessEventRepository;
import com.flypro.analytics.alert.dto.AlertRequestDTO;
import com.flypro.analytics.alert.dto.AlertResponseDTO;
import com.flypro.analytics.alert.dto.AlertSearchCriteria;
import com.flypro.analytics.alert.entity.Alert;
import com.flypro.analytics.alert.exception.AlertNotFoundException;
import com.flypro.analytics.alert.exception.InvalidAlertStateException;
import com.flypro.analytics.alert.mapper.AlertMapper;
import com.flypro.analytics.alert.repository.AlertRepository;
import com.flypro.analytics.alert.validator.AlertValidator;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private BadgeRepository badgeRepository;
    @Mock
    private AccessEventRepository accessEventRepository;
    @Mock
    private AlertMapper alertMapper;
    @Mock
    private AlertValidator alertValidator;

    @InjectMocks
    private AlertServiceImpl alertService;

    private Alert alert;
    private AlertResponseDTO response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a", List.of()));

        alert = Alert.builder()
                .id(1L)
                .title("Access denied")
                .description("Denied")
                .severity(AlertSeverity.MEDIUM)
                .status(AlertStatus.OPEN)
                .type(AlertType.ACCESS_DENIED)
                .createdDate(Instant.parse("2026-07-26T10:00:00Z"))
                .build();

        response = new AlertResponseDTO(
                1L, "Access denied", "Denied", AlertSeverity.MEDIUM, AlertStatus.OPEN,
                AlertType.ACCESS_DENIED, alert.getCreatedDate(), null, null,
                null, null, null, null, null, null, null, null, null, null
        );

        when(alertMapper.toResponse(any(Alert.class))).thenReturn(response);
    }

    @Test
    @DisplayName("search returns page")
    void search() {
        when(alertRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(alert)));

        var page = alertService.search(new AlertSearchCriteria(
                AlertStatus.OPEN, null, null, null, null, null, null, null, null, null
        ), PageRequest.of(0, 10));

        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("resolve transitions OPEN to RESOLVED")
    void resolve() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));
        when(alertMapper.toResponse(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            return new AlertResponseDTO(
                    a.getId(), a.getTitle(), a.getDescription(), a.getSeverity(), a.getStatus(),
                    a.getType(), a.getCreatedDate(), a.getResolvedDate(), a.getResolvedBy(),
                    null, null, null, null, null, null, null, null, null, null
            );
        });

        AlertResponseDTO result = alertService.resolve(1L);

        assertThat(result.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(result.resolvedBy()).isEqualTo("admin");
        verify(alertValidator).validateResolve(alert);
    }

    @Test
    @DisplayName("acknowledge invalid state is delegated to validator")
    void acknowledgeInvalid() {
        alert.setStatus(AlertStatus.CLOSED);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        org.mockito.Mockito.doThrow(new InvalidAlertStateException("already closed"))
                .when(alertValidator).validateAcknowledge(alert);

        assertThatThrownBy(() -> alertService.acknowledge(1L))
                .isInstanceOf(InvalidAlertStateException.class);
    }

    @Test
    @DisplayName("getById not found")
    void notFound() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> alertService.getById(99L))
                .isInstanceOf(AlertNotFoundException.class);
    }

    @Test
    @DisplayName("create validates and persists")
    void create() {
        AlertRequestDTO request = new AlertRequestDTO(
                "Title", "Desc", AlertSeverity.HIGH, AlertType.BADGE_REVOKED, null, 3L, null
        );
        when(alertMapper.toEntity(request)).thenReturn(Alert.builder().title("Title").build());
        when(badgeRepository.findById(3L)).thenReturn(Optional.of(
                com.flypro.analytics.badge.entity.Badge.builder().id(3L).badgeNumber("B").build()));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(5L);
            return a;
        });

        alertService.create(request);

        verify(alertValidator).validateCreate(request);
        verify(alertRepository).save(any(Alert.class));
    }
}
