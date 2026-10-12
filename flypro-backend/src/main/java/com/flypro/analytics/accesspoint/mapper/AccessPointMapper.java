package com.flypro.analytics.accesspoint.mapper;

import com.flypro.analytics.accesspoint.dto.AccessPointRequestDTO;
import com.flypro.analytics.accesspoint.dto.AccessPointResponseDTO;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.zone.mapper.ZoneMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for AccessPoint entity and DTOs.
 */
@Mapper(componentModel = "spring", uses = ZoneMapper.class)
public interface AccessPointMapper {

    @Mapping(target = "zone", source = "zone")
    AccessPointResponseDTO toResponse(AccessPoint accessPoint);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "zone", ignore = true)
    AccessPoint toEntity(AccessPointRequestDTO request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "zone", ignore = true)
    void updateEntity(AccessPointRequestDTO request, @MappingTarget AccessPoint accessPoint);
}
