package com.flypro.analytics.accesspoint.service;

import com.flypro.analytics.accesspoint.dto.AccessPointRequestDTO;
import com.flypro.analytics.accesspoint.dto.AccessPointResponseDTO;
import com.flypro.analytics.accesspoint.dto.AccessPointSearchCriteria;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.accesspoint.exception.AccessPointCannotBeDeletedException;
import com.flypro.analytics.accesspoint.exception.AccessPointNotFoundException;
import com.flypro.analytics.accesspoint.exception.DuplicateAccessPointCodeException;
import com.flypro.analytics.accesspoint.exception.DuplicateAccessPointNameException;
import com.flypro.analytics.accesspoint.mapper.AccessPointMapper;
import com.flypro.analytics.accesspoint.repository.AccessPointRepository;
import com.flypro.analytics.accesspoint.validator.AccessPointValidator;
import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.enums.AccessPointType;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.zone.entity.Zone;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import com.flypro.analytics.zone.repository.ZoneRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessPointServiceImplTest {

    @Mock
    private AccessPointRepository accessPointRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private AccessPointMapper accessPointMapper;
    @Mock
    private AccessPointValidator accessPointValidator;

    @InjectMocks
    private AccessPointServiceImpl accessPointService;

    private Zone zone;
    private AccessPoint accessPoint;
    private AccessPointRequestDTO request;
    private AccessPointResponseDTO response;

    @BeforeEach
    void setUp() {
        zone = Zone.builder().name("Lobby").building("North").floor("0").sensitive(false).build();
        zone.setId(1L);

        accessPoint = AccessPoint.builder()
                .name("Main Entrance Door")
                .code("AP-MAIN-01")
                .type(AccessPointType.DOOR)
                .description("Primary entrance")
                .status(AccessPointStatus.ACTIVE)
                .ipAddress("192.168.1.10")
                .location("Lobby")
                .zone(zone)
                .build();
        accessPoint.setId(10L);

        request = new AccessPointRequestDTO(
                "Main Entrance Door", "AP-MAIN-01", AccessPointType.DOOR,
                "Primary entrance", AccessPointStatus.ACTIVE,
                "192.168.1.10", "Lobby", 1L
        );
        response = new AccessPointResponseDTO(
                10L, "Main Entrance Door", "AP-MAIN-01", AccessPointType.DOOR,
                "Primary entrance", AccessPointStatus.ACTIVE,
                "192.168.1.10", "Lobby", null, null, null
        );

        when(accessPointValidator.normalize(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("create access point successfully")
    void createSuccess() {
        when(accessPointMapper.toEntity(request)).thenReturn(accessPoint);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(accessPointRepository.save(any())).thenReturn(accessPoint);
        when(accessPointMapper.toResponse(accessPoint)).thenReturn(response);

        assertThat(accessPointService.create(request).code()).isEqualTo("AP-MAIN-01");
        verify(accessPointValidator).validateCreate(request);
    }

    @Test
    @DisplayName("create rejects duplicate code")
    void createDuplicateCode() {
        doThrow(new DuplicateAccessPointCodeException("AP-MAIN-01"))
                .when(accessPointValidator).validateCreate(any());

        assertThatThrownBy(() -> accessPointService.create(request))
                .isInstanceOf(DuplicateAccessPointCodeException.class);
        verify(accessPointRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects duplicate name")
    void createDuplicateName() {
        doThrow(new DuplicateAccessPointNameException("Main Entrance Door"))
                .when(accessPointValidator).validateCreate(any());

        assertThatThrownBy(() -> accessPointService.create(request))
                .isInstanceOf(DuplicateAccessPointNameException.class);
    }

    @Test
    @DisplayName("create rejects missing zone")
    void createZoneNotFound() {
        doThrow(new ZoneNotFoundException(99L))
                .when(accessPointValidator).validateCreate(any());

        assertThatThrownBy(() -> accessPointService.create(request))
                .isInstanceOf(ZoneNotFoundException.class);
    }

    @Test
    @DisplayName("update access point")
    void updateSuccess() {
        when(accessPointRepository.findById(10L)).thenReturn(Optional.of(accessPoint));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(accessPointRepository.save(accessPoint)).thenReturn(accessPoint);
        when(accessPointMapper.toResponse(accessPoint)).thenReturn(response);

        assertThat(accessPointService.update(10L, request).id()).isEqualTo(10L);
        verify(accessPointMapper).updateEntity(eq(request), eq(accessPoint));
    }

    @Test
    @DisplayName("activate / deactivate / maintenance")
    void statusTransitions() {
        when(accessPointRepository.findById(10L)).thenReturn(Optional.of(accessPoint));
        when(accessPointRepository.save(accessPoint)).thenReturn(accessPoint);
        when(accessPointMapper.toResponse(accessPoint)).thenReturn(response);

        accessPointService.deactivate(10L);
        assertThat(accessPoint.getStatus()).isEqualTo(AccessPointStatus.INACTIVE);

        accessPointService.activate(10L);
        assertThat(accessPoint.getStatus()).isEqualTo(AccessPointStatus.ACTIVE);

        accessPointService.putInMaintenance(10L);
        assertThat(accessPoint.getStatus()).isEqualTo(AccessPointStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("delete blocked when access events exist")
    void deleteWithEvents() {
        when(accessPointRepository.findById(10L)).thenReturn(Optional.of(accessPoint));
        when(accessPointRepository.hasAccessEvents(10L)).thenReturn(true);

        assertThatThrownBy(() -> accessPointService.delete(10L))
                .isInstanceOf(AccessPointCannotBeDeletedException.class);
        verify(accessPointRepository, never()).delete(any(AccessPoint.class));
    }

    @Test
    @DisplayName("getById not found")
    void getNotFound() {
        when(accessPointRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessPointService.getById(99L))
                .isInstanceOf(AccessPointNotFoundException.class);
    }

    @Test
    @DisplayName("search with pagination")
    void search() {
        when(accessPointRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(accessPoint), PageRequest.of(0, 10), 1));
        when(accessPointMapper.toResponse(accessPoint)).thenReturn(response);

        PageResponse<AccessPointResponseDTO> page = accessPointService.search(
                new AccessPointSearchCriteria(null, null, null, null, 1L, null, null, null),
                PageRequest.of(0, 10)
        );

        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("list by zone")
    void listByZone() {
        when(zoneRepository.existsById(1L)).thenReturn(true);
        when(accessPointRepository.findByZoneIdOrderByNameAsc(1L)).thenReturn(List.of(accessPoint));
        when(accessPointMapper.toResponse(accessPoint)).thenReturn(response);

        assertThat(accessPointService.listByZone(1L)).hasSize(1);
    }
}
