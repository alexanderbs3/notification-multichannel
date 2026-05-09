package br.leetjourney.notificationmultichannel.api.dto;

import br.leetjourney.notificationmultichannel.domain.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record NotificationRequestDTO(

        UUID notificationId,
        String recipient,
        String content,
        NotificationChannel channel
) {
}