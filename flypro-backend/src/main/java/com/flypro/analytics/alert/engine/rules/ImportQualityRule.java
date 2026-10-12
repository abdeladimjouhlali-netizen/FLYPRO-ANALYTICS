package com.flypro.analytics.alert.engine.rules;

import com.flypro.analytics.alert.engine.AlertCandidate;
import com.flypro.analytics.alert.engine.AlertEvaluationContext;
import com.flypro.analytics.alert.engine.AlertRule;
import com.flypro.analytics.alert.engine.AlertTriggerSource;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.importbatch.entity.ImportBatch;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Raises SYSTEM_WARNING when an import batch contains rejected / inconsistent records.
 */
@Component
public class ImportQualityRule implements AlertRule {

    @Override
    public String name() {
        return "ImportQualityRule";
    }

    @Override
    public boolean supports(AlertTriggerSource source) {
        return source == AlertTriggerSource.IMPORT;
    }

    @Override
    public List<AlertCandidate> evaluate(AlertEvaluationContext context) {
        ImportBatch batch = context.importBatch();
        if (batch == null || batch.getInvalidRecords() <= 0) {
            return List.of();
        }

        return List.of(new AlertCandidate(
                AlertType.SYSTEM_WARNING,
                AlertSeverity.MEDIUM,
                "Inconsistent imported data",
                "Import batch " + batch.getId() + " (" + batch.getFileName() + ") rejected "
                        + batch.getInvalidRecords() + " record(s)",
                null, null, null, batch
        ));
    }
}
