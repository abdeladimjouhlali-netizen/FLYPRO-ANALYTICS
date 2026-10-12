package com.flypro.analytics.alert.repository;

import com.flypro.analytics.alert.entity.Alert;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.test.FlyproDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@FlyproDataJpaTest
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @Test
    @DisplayName("persist and query by severity / open status")
    void persistAndQuery() {
        Alert alert = Alert.builder()
                .title("Denied")
                .description("test")
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.OPEN)
                .type(AlertType.ACCESS_DENIED)
                .createdDate(Instant.parse("2026-07-26T10:00:00Z"))
                .build();
        Alert saved = alertRepository.save(alert);

        assertThat(saved.getId()).isNotNull();
        assertThat(alertRepository.findBySeverityOrderByCreatedDateDesc(AlertSeverity.HIGH)).hasSize(1);
        assertThat(alertRepository.existsByTypeAndBadgeIdAndStatusIn(
                AlertType.ACCESS_DENIED, 1L, EnumSet.of(AlertStatus.OPEN))).isFalse();
        assertThat(alertRepository.findAll()).extracting(Alert::getType)
                .containsExactly(AlertType.ACCESS_DENIED);
    }
}
