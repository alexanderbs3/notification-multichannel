package br.leetjourney.notificationmultichannel.domain.entity;

import br.leetjourney.notificationmultichannel.domain.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class NotificationOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String payload; // Json com dados da mensagem

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private String destination;

    private LocalDateTime createdAt = LocalDateTime.now();




}
