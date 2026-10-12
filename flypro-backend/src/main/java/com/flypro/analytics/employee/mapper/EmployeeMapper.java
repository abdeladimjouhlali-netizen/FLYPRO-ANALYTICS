package com.flypro.analytics.employee.mapper;

import com.flypro.analytics.department.mapper.DepartmentMapper;
import com.flypro.analytics.employee.dto.EmployeeRequestDTO;
import com.flypro.analytics.employee.dto.EmployeeResponseDTO;
import com.flypro.analytics.employee.entity.Employee;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Employee entity and DTOs.
 */
@Mapper(componentModel = "spring", uses = DepartmentMapper.class)
public interface EmployeeMapper {

    @Mapping(target = "accessProfileId", source = "accessProfile.id")
    @Mapping(target = "department", source = "department")
    @Mapping(target = "portalUsername", ignore = true)
    @Mapping(target = "temporaryPassword", ignore = true)
    EmployeeResponseDTO toResponse(Employee employee);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "accessProfile", ignore = true)
    @Mapping(target = "badgeAssignments", ignore = true)
    Employee toEntity(EmployeeRequestDTO request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "accessProfile", ignore = true)
    @Mapping(target = "badgeAssignments", ignore = true)
    void updateEntity(EmployeeRequestDTO request, @MappingTarget Employee employee);
}
