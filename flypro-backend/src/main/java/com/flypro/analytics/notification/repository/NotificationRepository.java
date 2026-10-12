package com.flypro.analytics.notification.repository;

import com.flypro.analytics.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIgnoreCaseOrderByCreatedAtDesc(String recipient, Pageable pageable);

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
