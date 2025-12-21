package br.leetjourney.notificationmultichannel.infrastructure.messaging.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String routingkey, Object payload) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingkey, payload);
    }
}
