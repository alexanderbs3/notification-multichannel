package br.leetjourney.notificationmultichannel.infrastructure.messaging.strategy;

import br.leetjourney.notificationmultichannel.api.dto.NotificationEventDTO;
import br.leetjourney.notificationmultichannel.domain.enums.NotificationChannel;

public interface NotificationStrategy {
    NotificationChannel getChannel();
    void send(NotificationEventDTO message);
}
