package com.flypro.analytics.accessevent.service;

import com.flypro.analytics.accessevent.dto.AccessEventRequestDTO;
import com.flypro.analytics.accessevent.dto.AccessEventResponseDTO;
import com.flypro.analytics.accessevent.dto.AccessEventSearchCriteria;
import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accessevent.exception.AccessEventNotFoundException;
import com.flypro.analytics.accessevent.exception.InvalidAccessEventException;
import com.flypro.analytics.accessevent.mapper.AccessEventMapper;
import com.flypro.analytics.accessevent.repository.AccessEventRepository;
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
import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
import com.flypro.analytics.common.enums.EventSource;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.zone.entity.Zone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessEventServiceImplTest {

    @Mock
    private AccessEventRepository accessEventRepository;
    @Mock
    private BadgeRepository badgeRepository;
    @Mock
    private AccessPointRepository accessPointRepository;
    @Mock
    private BadgeAssignmentRepository badgeAssignmentRepository;
    @Mock
    private AccessEventMapper accessEventMapper;
    @Mock
    private AccessEventValidator accessEventValidator;
    @Mock
    private AlertRuleEngine alertRuleEngine;

    @InjectMocks
    private AccessEventServiceImpl accessEventService;

    private Zone zone;
    private AccessPoint accessPoint;
    private Badge badge;
    private Employee employee;
    private AccessEventRequestDTO request;
    private AccessEventResponseDTO response;
    private AccessEvent event;

    @BeforeEach
    void setUp() {
        zone = Zone.builder().name("Lobby").building("North").floor("0").sensitive(false).build();
        zone.setId(1L);

        accessPoint = AccessPoint.builder()
                .name("Main Door").code("AP-01").type(com.flypro.analytics.common.enums.AccessPointType.DOOR)
                .status(com.flypro.analytics.common.enums.AccessPointStatus.ACTIVE)
                .zone(zone).build();
        accessPoint.setId(2L);

        badge = Badge.builder()
                .badgeNumber("BDG-001").type(BadgeType.RFID).status(BadgeStatus.ACTIVE)
                .issueDate(LocalDate.now().minusMonths(1))
                .expirationDate(LocalDate.now().plusYears(1))
                .build();
        badge.setId(3L);

        employee = Employee.builder().employeeNumber("EMP-1").firstName("Sara").lastName("Benali").build();
        employee.setId(4L);

        request = new AccessEventRequestDTO(
                Instant.parse("2026-07-26T10:15:30Z"),
                AccessDirection.ENTRY,
                AccessResult.AUTHORIZED,
                null,
                EventSource.API,
                "{\"readerId\":\"R-01\"}",
                3L, 2L, 1L
        );

        event = AccessEvent.builder()
                .eventTimestamp(request.eventTimestamp())
                .direction(AccessDirection.ENTRY)
                .result(AccessResult.AUTHORIZED)
                .source(EventSource.API)
                .badge(badge)
                .accessPoint(accessPoint)
                .zone(zone)
                .employee(employee)
                .build();
        event.setId(100L);

        response = new AccessEventResponseDTO(
                100L, request.eventTimestamp(), AccessDirection.ENTRY, AccessResult.AUTHORIZED,
                null, EventSource.API, request.rawPayload(),
                3L, "BDG-001", BadgeStatus.ACTIVE,
                4L, "EMP-1", "Sara", "Benali",
                2L, "AP-01", "Main Door", null, null
        );

        when(accessEventValidator.normalize(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("create event with active badge and assignment")
    void createSuccess() {
        when(badgeRepository.findById(3L)).thenReturn(Optional.of(badge));
        when(accessPointRepository.findById(2L)).thenReturn(Optional.of(accessPoint));
        when(badgeAssignmentRepository.findByBadgeIdAndActiveTrue(3L))
                .thenReturn(Optional.of(BadgeAssignment.builder().badge(badge).employee(employee).active(true).build()));
        when(accessEventMapper.toEntity(request)).thenReturn(event);
        when(accessEventRepository.save(any())).thenReturn(event);
        when(accessEventMapper.toResponse(event)).thenReturn(response);

        AccessEventResponseDTO created = accessEventService.create(request);

        assertThat(created.result()).isEqualTo(AccessResult.AUTHORIZED);
        verify(accessEventRepository).save(any(AccessEvent.class));
    }

    @Test
    @DisplayName("create rejects missing badge")
    void createBadgeNotFound() {
        doThrow(new BadgeNotFoundException(99L)).when(accessEventValidator).validateCreate(any());

        assertThatThrownBy(() -> accessEventService.create(request))
                .isInstanceOf(BadgeNotFoundException.class);
        verify(accessEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects missing access point")
    void createAccessPointNotFound() {
        doThrow(new AccessPointNotFoundException(99L)).when(accessEventValidator).validateCreate(any());

        assertThatThrownBy(() -> accessEventService.create(request))
                .isInstanceOf(AccessPointNotFoundException.class);
    }

    @Test
    @DisplayName("create rejects zone mismatch")
    void createZoneMismatch() {
        when(badgeRepository.findById(3L)).thenReturn(Optional.of(badge));
        when(accessPointRepository.findById(2L)).thenReturn(Optional.of(accessPoint));

        AccessEventRequestDTO badZone = new AccessEventRequestDTO(
                request.eventTimestamp(), request.direction(), request.result(), null,
                request.source(), request.rawPayload(), 3L, 2L, 99L
        );

        assertThatThrownBy(() -> accessEventService.create(badZone))
                .isInstanceOf(InvalidAccessEventException.class);
    }

    @Test
    @DisplayName("expired badge forces EXPIRED result")
    void expiredBadge() {
        badge.setStatus(BadgeStatus.EXPIRED);
        when(badgeRepository.findById(3L)).thenReturn(Optional.of(badge));
        when(accessPointRepository.findById(2L)).thenReturn(Optional.of(accessPoint));
        when(badgeAssignmentRepository.findByBadgeIdAndActiveTrue(3L)).thenReturn(Optional.empty());
        when(accessEventMapper.toEntity(any())).thenReturn(event);
        when(accessEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accessEventMapper.toResponse(any())).thenReturn(response);

        accessEventService.create(request);

        ArgumentCaptor<AccessEvent> captor = ArgumentCaptor.forClass(AccessEvent.class);
        verify(accessEventRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(AccessResult.EXPIRED);
        assertThat(captor.getValue().getRefusalReason()).contains("expired");
    }

    @Test
    @DisplayName("revoked badge forces DENIED")
    void revokedBadge() {
        badge.setStatus(BadgeStatus.REVOKED);
        when(badgeRepository.findById(3L)).thenReturn(Optional.of(badge));
        when(accessPointRepository.findById(2L)).thenReturn(Optional.of(accessPoint));
        when(badgeAssignmentRepository.findByBadgeIdAndActiveTrue(3L)).thenReturn(Optional.empty());
        when(accessEventMapper.toEntity(any())).thenReturn(event);
        when(accessEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accessEventMapper.toResponse(any())).thenReturn(response);

        accessEventService.create(request);

        ArgumentCaptor<AccessEvent> captor = ArgumentCaptor.forClass(AccessEvent.class);
        verify(accessEventRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(AccessResult.DENIED);
        assertThat(captor.getValue().getRefusalReason()).contains("revoked");
    }

    @Test
    @DisplayName("suspended badge forces DENIED")
    void suspendedBadge() {
        badge.setStatus(BadgeStatus.SUSPENDED);
        when(badgeRepository.findById(3L)).thenReturn(Optional.of(badge));
        when(accessPointRepository.findById(2L)).thenReturn(Optional.of(accessPoint));
        when(badgeAssignmentRepository.findByBadgeIdAndActiveTrue(3L)).thenReturn(Optional.empty());
        when(accessEventMapper.toEntity(any())).thenReturn(event);
        when(accessEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accessEventMapper.toResponse(any())).thenReturn(response);

        accessEventService.create(request);

        ArgumentCaptor<AccessEvent> captor = ArgumentCaptor.forClass(AccessEvent.class);
        verify(accessEventRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(AccessResult.DENIED);
        assertThat(captor.getValue().getRefusalReason()).contains("suspended");
    }

    @Test
    @DisplayName("getById not found")
    void getNotFound() {
        when(accessEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessEventService.getById(99L))
                .isInstanceOf(AccessEventNotFoundException.class);
    }

    @Test
    @DisplayName("search with pagination")
    void search() {
        when(accessEventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1));
        when(accessEventMapper.toResponse(event)).thenReturn(response);

        PageResponse<AccessEventResponseDTO> page = accessEventService.search(
                new AccessEventSearchCriteria(null, null, null, null, null, null, null, null, null, null, null),
                PageRequest.of(0, 10)
        );

        assertThat(page.totalElements()).isEqualTo(1);
    }
}
