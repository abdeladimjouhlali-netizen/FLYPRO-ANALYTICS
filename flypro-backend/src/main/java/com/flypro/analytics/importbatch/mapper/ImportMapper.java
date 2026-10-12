package com.flypro.analytics.importbatch.mapper;

import com.flypro.analytics.common.enums.ImportFileType;
import com.flypro.analytics.dataquality.entity.DataQualityIssue;
import com.flypro.analytics.importbatch.dto.DataQualityIssueDTO;
import com.flypro.analytics.importbatch.dto.ImportResponseDTO;
import com.flypro.analytics.importbatch.entity.ImportBatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper for import batch and data-quality DTOs.
 */
@Mapper(componentModel = "spring")
public interface ImportMapper {

    @Mapping(target = "fileType", source = "source", qualifiedByName = "toFileType")
    @Mapping(target = "issues", source = "dataQualityIssues")
    ImportResponseDTO toResponse(ImportBatch batch);

    DataQualityIssueDTO toIssueDto(DataQualityIssue issue);

    List<DataQualityIssueDTO> toIssueDtos(List<DataQualityIssue> issues);

    @Named("toFileType")
    default ImportFileType toFileType(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return ImportFileType.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
