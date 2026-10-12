package com.flypro.analytics.zone.mapper;

import com.flypro.analytics.zone.dto.ZoneRequestDTO;
import com.flypro.analytics.zone.dto.ZoneResponseDTO;
import com.flypro.analytics.zone.dto.ZoneSummaryDTO;
import com.flypro.analytics.zone.entity.Zone;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Zone entity and DTOs.
 */
@Mapper(componentModel = "spring")
public interface ZoneMapper {

    ZoneResponseDTO toResponse(Zone zone);

    ZoneSummaryDTO toSummary(Zone zone);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "accessPoints", ignore = true)
    @Mapping(target = "accessProfiles", ignore = true)
    Zone toEntity(ZoneRequestDTO request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "accessPoints", ignore = true)
    @Mapping(target = "accessProfiles", ignore = true)
    void updateEntity(ZoneRequestDTO request, @MappingTarget Zone zone);
}
