package com.flypro.analytics.department.mapper;

import com.flypro.analytics.department.dto.DepartmentRequest;
import com.flypro.analytics.department.dto.DepartmentResponse;
import com.flypro.analytics.department.dto.DepartmentSummaryResponse;
import com.flypro.analytics.department.entity.Department;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Department entity and DTOs.
 */
@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    DepartmentResponse toResponse(Department department);

    DepartmentSummaryResponse toSummary(Department department);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "employees", ignore = true)
    Department toEntity(DepartmentRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "employees", ignore = true)
    void updateEntity(DepartmentRequest request, @MappingTarget Department department);
}
