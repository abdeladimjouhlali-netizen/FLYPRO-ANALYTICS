package com.flypro.analytics.notification.service;

import java.util.Map;

/**
 * Renders HTML email bodies from Thymeleaf templates.
 */
public interface EmailTemplateService {

    String render(String templateName, Map<String, Object> model);
}
