package com.flypro.analytics.accessprofile.mapper;

import com.flypro.analytics.accessprofile.dto.AccessProfileRequestDTO;
import com.flypro.analytics.accessprofile.dto.AccessProfileResponseDTO;
import com.flypro.analytics.accessprofile.entity.AccessProfile;
import com.flypro.analytics.zone.mapper.ZoneMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for AccessProfile entity and DTOs.
 */
@Mapper(componentModel = "spring", uses = ZoneMapper.class)
public interface AccessProfileMapper {

    @Mapping(target = "zones", source = "zones")
    AccessProfileResponseDTO toResponse(AccessProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "zones", ignore = true)
    @Mapping(target = "employees", ignore = true)
    AccessProfile toEntity(AccessProfileRequestDTO request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "zones", ignore = true)
    @Mapping(target = "employees", ignore = true)
    void updateEntity(AccessProfileRequestDTO request, @MappingTarget AccessProfile profile);
}
