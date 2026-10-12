package com.flypro.analytics.importbatch.storage;

import com.flypro.analytics.common.enums.ImportFileType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory staging area for uploaded import files pending preview / execute.
 * Content is discarded after successful execution or explicit remove.
 */
@Component
public class ImportFileStorage {

    private final Map<Long, StagedImportFile> files = new ConcurrentHashMap<>();

    public void store(Long batchId, StagedImportFile file) {
        files.put(batchId, file);
    }

    public Optional<StagedImportFile> get(Long batchId) {
        return Optional.ofNullable(files.get(batchId));
    }

    public void remove(Long batchId) {
        files.remove(batchId);
    }

    public record StagedImportFile(
            String fileName,
            ImportFileType fileType,
            byte[] content
    ) {
    }
}
