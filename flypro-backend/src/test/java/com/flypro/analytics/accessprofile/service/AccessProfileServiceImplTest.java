package com.flypro.analytics.accessprofile.service;

import com.flypro.analytics.accessprofile.dto.AccessProfileRequestDTO;
import com.flypro.analytics.accessprofile.dto.AccessProfileResponseDTO;
import com.flypro.analytics.accessprofile.entity.AccessProfile;
import com.flypro.analytics.accessprofile.exception.AccessProfileMustHaveZoneException;
import com.flypro.analytics.accessprofile.exception.DuplicateAccessProfileException;
import com.flypro.analytics.accessprofile.exception.InvalidWorkingHoursException;
import com.flypro.analytics.accessprofile.exception.ZoneAlreadyAssignedException;
import com.flypro.analytics.accessprofile.mapper.AccessProfileMapper;
import com.flypro.analytics.accessprofile.repository.AccessProfileRepository;
import com.flypro.analytics.accessprofile.validator.AccessProfileValidator;
import com.flypro.analytics.common.enums.SecurityLevel;
import com.flypro.analytics.zone.entity.Zone;
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

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessProfileServiceImplTest {

    @Mock
    private AccessProfileRepository accessProfileRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private AccessProfileMapper accessProfileMapper;
    @Mock
    private AccessProfileValidator accessProfileValidator;

    @InjectMocks
    private AccessProfileServiceImpl accessProfileService;

    private Zone zone1;
    private Zone zone2;
    private AccessProfile profile;
    private AccessProfileRequestDTO request;
    private AccessProfileResponseDTO response;

    @BeforeEach
    void setUp() {
        zone1 = Zone.builder().name("Z1").building("B1").floor("1").sensitive(false).build();
        zone1.setId(1L);
        zone2 = Zone.builder().name("Z2").building("B1").floor("2").sensitive(true).build();
        zone2.setId(2L);

        profile = AccessProfile.builder()
                .name("Standard")
                .description("Office")
                .securityLevel(SecurityLevel.MEDIUM)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .zones(new HashSet<>(Set.of(zone1)))
                .build();
        profile.setId(10L);

        request = new AccessProfileRequestDTO(
                "Standard", "Office", SecurityLevel.MEDIUM,
                LocalTime.of(8, 0), LocalTime.of(18, 0), Set.of(1L)
        );
        response = new AccessProfileResponseDTO(
                10L, "Standard", "Office", SecurityLevel.MEDIUM,
                LocalTime.of(8, 0), LocalTime.of(18, 0), Set.of(), null, null
        );

        when(accessProfileValidator.normalize(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("create access profile with zones")
    void createSuccess() {
        when(accessProfileMapper.toEntity(request)).thenReturn(profile);
        when(zoneRepository.findByIdIn(Set.of(1L))).thenReturn(List.of(zone1));
        when(accessProfileRepository.save(any())).thenReturn(profile);
        when(accessProfileMapper.toResponse(profile)).thenReturn(response);

        assertThat(accessProfileService.create(request).name()).isEqualTo("Standard");
        verify(accessProfileValidator).validateCreate(request);
    }

    @Test
    @DisplayName("create rejects duplicate name")
    void createDuplicate() {
        doThrow(new DuplicateAccessProfileException("Standard"))
                .when(accessProfileValidator).validateCreate(any());

        assertThatThrownBy(() -> accessProfileService.create(request))
                .isInstanceOf(DuplicateAccessProfileException.class);
        verify(accessProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects invalid working hours")
    void createInvalidHours() {
        doThrow(new InvalidWorkingHoursException("Start time must be before end time"))
                .when(accessProfileValidator).validateCreate(any());

        assertThatThrownBy(() -> accessProfileService.create(request))
                .isInstanceOf(InvalidWorkingHoursException.class);
    }

    @Test
    @DisplayName("assign zone")
    void assignZone() {
        when(accessProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(zoneRepository.findById(2L)).thenReturn(Optional.of(zone2));
        when(accessProfileRepository.save(profile)).thenReturn(profile);
        when(accessProfileMapper.toResponse(profile)).thenReturn(response);

        accessProfileService.assignZone(10L, 2L);

        assertThat(profile.getZones()).contains(zone2);
    }

    @Test
    @DisplayName("assign rejects already assigned zone")
    void assignAlready() {
        when(accessProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone1));

        assertThatThrownBy(() -> accessProfileService.assignZone(10L, 1L))
                .isInstanceOf(ZoneAlreadyAssignedException.class);
    }

    @Test
    @DisplayName("remove last zone is rejected")
    void removeLastZone() {
        when(accessProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone1));

        assertThatThrownBy(() -> accessProfileService.removeZone(10L, 1L))
                .isInstanceOf(AccessProfileMustHaveZoneException.class);
    }

    @Test
    @DisplayName("remove zone when multiple remain")
    void removeZone() {
        profile.getZones().add(zone2);
        when(accessProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone1));
        when(accessProfileRepository.save(profile)).thenReturn(profile);
        when(accessProfileMapper.toResponse(profile)).thenReturn(response);

        accessProfileService.removeZone(10L, 1L);

        assertThat(profile.getZones()).containsExactly(zone2);
    }
}
