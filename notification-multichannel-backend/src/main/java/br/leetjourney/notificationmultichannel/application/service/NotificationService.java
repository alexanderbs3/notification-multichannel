package br.leetjourney.notificationmultichannel.application.service;

import br.leetjourney.notificationmultichannel.api.dto.NotificationEventDTO;
import br.leetjourney.notificationmultichannel.api.dto.NotificationRequestDTO;
import br.leetjourney.notificationmultichannel.api.dto.NotificationResponseDTO;
import br.leetjourney.notificationmultichannel.domain.entity.Notification;
import br.leetjourney.notificationmultichannel.domain.entity.NotificationOutbox;
import br.leetjourney.notificationmultichannel.domain.enums.NotificationStatus;
import br.leetjourney.notificationmultichannel.domain.enums.OutboxStatus;
import br.leetjourney.notificationmultichannel.domain.repository.NotificationRepository;
import br.leetjourney.notificationmultichannel.domain.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public NotificationResponseDTO createNotification(NotificationRequestDTO request) {
        // 1. Criamos a entidade de negócio
        Notification notification = new Notification();
        notification.setRecipient(request.recipient());
        notification.setContent(request.content());
        notification.setChannel(request.channel());
        notification.setStatus(NotificationStatus.CREATED);

        // O save() retorna a entidade com o ID (UUID) preenchido
        Notification savedNotification = notificationRepository.save(notification);

        // 2. Criamos o Payload para o RabbitMQ com o ID gerado
        var queuePayload = new NotificationEventDTO(
                savedNotification.getId(),
                savedNotification.getRecipient(),
                savedNotification.getContent(),
                savedNotification.getChannel()
        );

        // 3. Criamos o registro de Outbox na mesma transação
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setDestination(savedNotification.getChannel().name());

        try {
            outbox.setPayload(objectMapper.writeValueAsString(queuePayload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar para o Outbox", e);
        }

        outboxRepository.save(outbox);

        // RETORNO: Agora devolvemos o DTO com o ID para a API
        return new NotificationResponseDTO(
                savedNotification.getId(),
                savedNotification.getStatus(),
                savedNotification.getChannel()
        );
    }

    // MÉTODO ADICIONADO: Para suportar o endpoint GET /v1/notifications/{id}
    @Transactional(readOnly = true)
    public NotificationResponseDTO getNotificationStatus(UUID id) {
        return notificationRepository.findById(id)
                .map(n -> new NotificationResponseDTO(n.getId(), n.getStatus(), n.getChannel()))
                .orElseThrow(() -> new EntityNotFoundException("Notificação não encontrada com o ID: " + id));
    }
}