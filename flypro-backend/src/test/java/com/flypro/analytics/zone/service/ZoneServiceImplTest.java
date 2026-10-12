package com.flypro.analytics.zone.service;

import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.zone.dto.ZoneRequestDTO;
import com.flypro.analytics.zone.dto.ZoneResponseDTO;
import com.flypro.analytics.zone.dto.ZoneSearchCriteria;
import com.flypro.analytics.zone.entity.Zone;
import com.flypro.analytics.zone.exception.DuplicateZoneException;
import com.flypro.analytics.zone.exception.ZoneCannotBeDeletedException;
import com.flypro.analytics.zone.exception.ZoneNotFoundException;
import com.flypro.analytics.zone.mapper.ZoneMapper;
import com.flypro.analytics.zone.repository.ZoneRepository;
import com.flypro.analytics.zone.validator.ZoneValidator;
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
class ZoneServiceImplTest {

    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private ZoneMapper zoneMapper;
    @Mock
    private ZoneValidator zoneValidator;

    @InjectMocks
    private ZoneServiceImpl zoneService;

    private Zone zone;
    private ZoneRequestDTO request;
    private ZoneResponseDTO response;

    @BeforeEach
    void setUp() {
        zone = Zone.builder()
                .name("Server Room A")
                .building("Building North")
                .floor("3")
                .sensitive(true)
                .description("Restricted")
                .build();
        zone.setId(1L);

        request = new ZoneRequestDTO("Server Room A", "Building North", "3", true, "Restricted");
        response = new ZoneResponseDTO(1L, "Server Room A", "Building North", "3", true, "Restricted", null, null);

        when(zoneValidator.normalize(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("create zone successfully")
    void createSuccess() {
        when(zoneMapper.toEntity(request)).thenReturn(zone);
        when(zoneRepository.save(any())).thenReturn(zone);
        when(zoneMapper.toResponse(zone)).thenReturn(response);

        assertThat(zoneService.create(request).name()).isEqualTo("Server Room A");
        verify(zoneValidator).validateCreate(request);
    }

    @Test
    @DisplayName("create rejects duplicate name")
    void createDuplicate() {
        doThrow(new DuplicateZoneException("Server Room A"))
                .when(zoneValidator).validateCreate(any());

        assertThatThrownBy(() -> zoneService.create(request))
                .isInstanceOf(DuplicateZoneException.class);
        verify(zoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("update zone")
    void updateSuccess() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(zoneRepository.save(zone)).thenReturn(zone);
        when(zoneMapper.toResponse(zone)).thenReturn(response);

        assertThat(zoneService.update(1L, request).id()).isEqualTo(1L);
        verify(zoneMapper).updateEntity(eq(request), eq(zone));
    }

    @Test
    @DisplayName("delete blocked when access points exist")
    void deleteWithAccessPoints() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(zoneRepository.hasAccessPoints(1L)).thenReturn(true);

        assertThatThrownBy(() -> zoneService.delete(1L))
                .isInstanceOf(ZoneCannotBeDeletedException.class);
        verify(zoneRepository, never()).delete(any(Zone.class));
    }

    @Test
    @DisplayName("delete blocked when assigned to profile")
    void deleteAssigned() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(zoneRepository.hasAccessPoints(1L)).thenReturn(false);
        when(zoneRepository.isAssignedToAnyProfile(1L)).thenReturn(true);

        assertThatThrownBy(() -> zoneService.delete(1L))
                .isInstanceOf(ZoneCannotBeDeletedException.class);
    }

    @Test
    @DisplayName("getById not found")
    void getNotFound() {
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zoneService.getById(99L))
                .isInstanceOf(ZoneNotFoundException.class);
    }

    @Test
    @DisplayName("search with pagination")
    void search() {
        when(zoneRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(zone), PageRequest.of(0, 10), 1));
        when(zoneMapper.toResponse(zone)).thenReturn(response);

        PageResponse<ZoneResponseDTO> page = zoneService.search(
                new ZoneSearchCriteria(null, null, null, true, null),
                PageRequest.of(0, 10)
        );

        assertThat(page.totalElements()).isEqualTo(1);
    }
}
