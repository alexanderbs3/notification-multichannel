package br.leetjourney.notificationmultichannel.api.dto;

import br.leetjourney.notificationmultichannel.domain.enums.NotificationChannel;
import br.leetjourney.notificationmultichannel.domain.enums.NotificationStatus;

import java.util.UUID;

public record NotificationResponseDTO (UUID id,
                                       NotificationStatus status,
                                       NotificationChannel channel
)


{}
