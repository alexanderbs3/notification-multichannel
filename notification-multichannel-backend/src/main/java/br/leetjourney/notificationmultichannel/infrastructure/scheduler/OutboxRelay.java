package br.leetjourney.notificationmultichannel.infrastructure.scheduler;

import br.leetjourney.notificationmultichannel.domain.entity.NotificationOutbox;
import br.leetjourney.notificationmultichannel.domain.enums.OutboxStatus;
import br.leetjourney.notificationmultichannel.domain.repository.OutboxRepository;
import br.leetjourney.notificationmultichannel.infrastructure.messaging.rabbitmq.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final NotificationPublisher notificationPublisher;

    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {
        List<NotificationOutbox> pendingItems =
                outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingItems.isEmpty()) return;

        log.info("Processando {} itens do outbox", pendingItems.size());

        pendingItems.forEach(item -> {
            try {
                // Agora enviamos de verdade!
                notificationPublisher.publish(item.getDestination(), item.getPayload());

                item.setStatus(OutboxStatus.SENT);
                outboxRepository.save(item);
                log.info("Item {} publicado com sucesso no RabbitMQ", item.getId());
            } catch (Exception e) {
                log.error("Falha ao publicar no RabbitMQ. O item permanecerá como PENDING para retry.", e);
                // Não alteramos para SENT, o @Scheduled tentará novamente na próxima execução
            }
        });
    }
}
