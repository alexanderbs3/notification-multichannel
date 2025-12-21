package br.leetjourney.notificationmultichannel.domain.entity;

import br.leetjourney.notificationmultichannel.domain.enums.NotificationChannel;
import br.leetjourney.notificationmultichannel.domain.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data

public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String recipient; //email, telefone ou token

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();
}
