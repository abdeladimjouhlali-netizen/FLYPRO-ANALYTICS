package com.flypro.analytics.alert.mapper;

import com.flypro.analytics.alert.dto.AlertRequestDTO;
import com.flypro.analytics.alert.dto.AlertResponseDTO;
import com.flypro.analytics.alert.entity.Alert;
import com.flypro.analytics.employee.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for Alert entity and DTOs.
 */
@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeNumber", source = "employee.employeeNumber")
    @Mapping(target = "employeeFullName", source = "employee", qualifiedByName = "fullName")
    @Mapping(target = "badgeId", source = "badge.id")
    @Mapping(target = "badgeNumber", source = "badge.badgeNumber")
    @Mapping(target = "accessEventId", source = "accessEvent.id")
    @Mapping(target = "zoneId", source = "accessEvent.zone.id")
    @Mapping(target = "zoneName", source = "accessEvent.zone.name")
    @Mapping(target = "departmentId", source = "employee.department.id")
    AlertResponseDTO toResponse(Alert alert);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "resolvedDate", ignore = true)
    @Mapping(target = "resolvedBy", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "badge", ignore = true)
    @Mapping(target = "accessEvent", ignore = true)
    Alert toEntity(AlertRequestDTO request);

    @Named("fullName")
    default String fullName(Employee employee) {
        if (employee == null) {
            return null;
        }
        return employee.getFirstName() + " " + employee.getLastName();
    }
}
