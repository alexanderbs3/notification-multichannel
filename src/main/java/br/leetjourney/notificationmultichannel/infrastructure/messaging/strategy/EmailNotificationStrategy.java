package br.leetjourney.notificationmultichannel.infrastructure.messaging.strategy;

import br.leetjourney.notificationmultichannel.api.dto.NotificationEventDTO;
import br.leetjourney.notificationmultichannel.domain.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationStrategy implements NotificationStrategy{
    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationEventDTO message) {
        log.info("Enviando E-MAIL para {}: {}", message.recipient(), message.content());

    }
}
