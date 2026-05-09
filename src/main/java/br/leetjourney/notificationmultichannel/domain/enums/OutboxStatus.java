package br.leetjourney.notificationmultichannel.domain.enums;

public enum OutboxStatus {
    PENDING,   // Aguardando o Relay enviar para o RabbitMQ
    SENT,      // Já entregue ao RabbitMQ com sucesso
    FAILED     // Falha definitiva após tentativas de publicação
}
