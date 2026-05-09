package br.leetjourney.notificationmultichannel.infrastructure.messaging.strategy;

import br.leetjourney.notificationmultichannel.api.dto.NotificationEventDTO;
import br.leetjourney.notificationmultichannel.domain.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component

public class SmsNotificationStrategy implements NotificationStrategy{
    @Override
    public NotificationChannel getChannel() { return NotificationChannel.SMS; }

    @Override
    public void send(NotificationEventDTO message) {
        log.info("Enviando SMS para {}: {}", message.recipient(), message.content());

    }
}
