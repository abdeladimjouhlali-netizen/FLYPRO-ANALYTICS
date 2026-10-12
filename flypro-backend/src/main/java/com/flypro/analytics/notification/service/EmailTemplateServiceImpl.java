package com.flypro.analytics.notification.service;

import com.flypro.analytics.notification.exception.TemplateException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Service
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private final TemplateEngine templateEngine;

    public EmailTemplateServiceImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String render(String templateName, Map<String, Object> model) {
        try {
            Context context = new Context(Locale.ENGLISH);
            if (model != null) {
                context.setVariables(model);
            }
            return templateEngine.process(templateName, context);
        } catch (Exception ex) {
            throw new TemplateException("Failed to render template '" + templateName + "'", ex);
        }
    }
}
