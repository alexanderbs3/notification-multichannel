package br.leetjourney.notificationmultichannel.domain.repository;

import br.leetjourney.notificationmultichannel.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
