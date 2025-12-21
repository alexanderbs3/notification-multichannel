package br.leetjourney.notificationmultichannel.domain.enums;

public enum NotificationStatus {

    CREATED,   // Registrada no sistema
    PROCESSED, // Enviada para o canal de destino
    FAILED     // Erro no provedor (Ex: SMTP fora do ar)
}
