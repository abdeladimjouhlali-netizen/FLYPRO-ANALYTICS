package com.flypro.analytics.leave.mapper;

import com.flypro.analytics.leave.dto.LeaveRequestResponseDTO;
import com.flypro.analytics.leave.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for LeaveRequest entity and DTOs.
 */
@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeNumber", source = "employee.employeeNumber")
    @Mapping(target = "employeeFirstName", source = "employee.firstName")
    @Mapping(target = "employeeLastName", source = "employee.lastName")
    @Mapping(target = "departmentId", source = "employee.department.id")
    @Mapping(target = "departmentName", source = "employee.department.name")
    @Mapping(target = "reviewedBy", source = "updatedBy")
    @Mapping(target = "reviewedAt", source = "updatedAt")
    LeaveRequestResponseDTO toResponse(LeaveRequest entity);
}
