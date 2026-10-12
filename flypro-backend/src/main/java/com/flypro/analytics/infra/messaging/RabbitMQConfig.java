package com.flypro.analytics.infra.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manual RabbitMQ wiring (auto-config excluded by default to allow boot without a broker).
 * Declares the queues consumed by {@link AsyncJobListeners} and published to by {@link AsyncJobPublisher}.
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "flypro.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMQConfig {

    @Bean
    public ConnectionFactory rabbitConnectionFactory(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password
    ) {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue importJobsQueue() {
        return QueueBuilder.durable(MessagingQueues.IMPORT_JOBS).build();
    }

    @Bean
    public Queue aiPredictionsQueue() {
        return QueueBuilder.durable(MessagingQueues.AI_PREDICTIONS).build();
    }

    @Bean
    public Queue alertsQueue() {
        return QueueBuilder.durable(MessagingQueues.ALERTS).build();
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(MessagingQueues.NOTIFICATIONS).build();
    }

    @Bean
    public Queue dashboardRefreshQueue() {
        return QueueBuilder.durable(MessagingQueues.DASHBOARD_REFRESH).build();
    }

    @Bean
    public Queue accessEventsQueue() {
        return QueueBuilder.durable(MessagingQueues.ACCESS_EVENTS).build();
    }

    @Bean
    public Queue auditLogsQueue() {
        return QueueBuilder.durable(MessagingQueues.AUDIT_LOGS).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(MessagingQueues.DLQ).build();
    }
}
