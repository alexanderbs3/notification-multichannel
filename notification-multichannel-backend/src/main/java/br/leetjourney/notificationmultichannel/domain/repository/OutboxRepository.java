package br.leetjourney.notificationmultichannel.domain.repository;

import br.leetjourney.notificationmultichannel.domain.entity.NotificationOutbox;
import br.leetjourney.notificationmultichannel.domain.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

    List<NotificationOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
