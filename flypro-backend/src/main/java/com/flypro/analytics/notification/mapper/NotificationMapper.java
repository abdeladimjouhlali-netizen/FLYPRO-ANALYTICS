package com.flypro.analytics.notification.mapper;

import com.flypro.analytics.notification.dto.NotificationResponse;
import com.flypro.analytics.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
