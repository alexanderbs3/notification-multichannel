package br.leetjourney.notificationmultichannel.api.dto;

import br.leetjourney.notificationmultichannel.domain.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NotificationEventDTO(
        UUID notificationId,
        @NotBlank String recipient,
        @NotBlank String content,
        @NotNull NotificationChannel channel // Corrigido: Enum usa @NotNull
) {
}