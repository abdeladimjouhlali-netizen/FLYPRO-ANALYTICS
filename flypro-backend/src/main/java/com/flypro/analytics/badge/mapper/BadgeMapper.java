package com.flypro.analytics.badge.mapper;

import com.flypro.analytics.badge.dto.BadgeAssignmentDTO;
import com.flypro.analytics.badge.dto.BadgeRequestDTO;
import com.flypro.analytics.badge.dto.BadgeResponseDTO;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Badge / BadgeAssignment entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface BadgeMapper {

    @Mapping(target = "currentAssignment", ignore = true)
    BadgeResponseDTO toResponse(Badge badge);

    default BadgeResponseDTO toResponse(Badge badge, BadgeAssignment currentAssignment) {
        BadgeResponseDTO base = toResponse(badge);
        return new BadgeResponseDTO(
                base.id(),
                base.badgeNumber(),
                base.type(),
                base.status(),
                base.issueDate(),
                base.expirationDate(),
                currentAssignment == null ? null : toAssignmentDto(currentAssignment),
                base.createdAt(),
                base.updatedAt()
        );
    }

    @Mapping(target = "badgeId", source = "badge.id")
    @Mapping(target = "badgeNumber", source = "badge.badgeNumber")
    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeNumber", source = "employee.employeeNumber")
    @Mapping(target = "employeeFirstName", source = "employee.firstName")
    @Mapping(target = "employeeLastName", source = "employee.lastName")
    BadgeAssignmentDTO toAssignmentDto(BadgeAssignment assignment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    Badge toEntity(BadgeRequestDTO request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    void updateEntity(BadgeRequestDTO request, @MappingTarget Badge badge);
}
