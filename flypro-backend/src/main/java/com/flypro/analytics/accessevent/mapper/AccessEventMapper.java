package com.flypro.analytics.accessevent.mapper;

import com.flypro.analytics.accessevent.dto.AccessEventRequestDTO;
import com.flypro.analytics.accessevent.dto.AccessEventResponseDTO;
import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.zone.mapper.ZoneMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for AccessEvent entity and DTOs.
 */
@Mapper(componentModel = "spring", uses = ZoneMapper.class)
public interface AccessEventMapper {

    @Mapping(target = "badgeId", source = "badge.id")
    @Mapping(target = "badgeNumber", source = "badge.badgeNumber")
    @Mapping(target = "badgeStatus", source = "badge.status")
    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeNumber", source = "employee.employeeNumber")
    @Mapping(target = "employeeFirstName", source = "employee.firstName")
    @Mapping(target = "employeeLastName", source = "employee.lastName")
    @Mapping(target = "accessPointId", source = "accessPoint.id")
    @Mapping(target = "accessPointCode", source = "accessPoint.code")
    @Mapping(target = "accessPointName", source = "accessPoint.name")
    @Mapping(target = "zone", source = "zone")
    AccessEventResponseDTO toResponse(AccessEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "badge", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "accessPoint", ignore = true)
    @Mapping(target = "zone", ignore = true)
    AccessEvent toEntity(AccessEventRequestDTO request);
}
