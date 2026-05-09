package br.leetjourney.notificationmultichannel.consumers;

import br.leetjourney.notificationmultichannel.api.dto.NotificationEventDTO;
import br.leetjourney.notificationmultichannel.domain.entity.Notification;
import br.leetjourney.notificationmultichannel.domain.enums.NotificationStatus;
import br.leetjourney.notificationmultichannel.domain.repository.NotificationRepository;
import br.leetjourney.notificationmultichannel.infrastructure.messaging.rabbitmq.RabbitMQConfig;
import br.leetjourney.notificationmultichannel.infrastructure.messaging.strategy.NotificationStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final List<NotificationStrategy> strategies;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    @Transactional
    public void consume(String payload) { // Alterado para String para evitar erro de conversão automática
        try {
            log.info("Mensagem recebida do broker: {}", payload);

            // Conversão manual: Transforma a String JSON no nosso Objeto de Dados
            NotificationEventDTO message = objectMapper.readValue(payload, NotificationEventDTO.class);

            Notification notification = notificationRepository.findById(message.notificationId())
                    .orElseThrow(() -> new RuntimeException("Notificação não encontrada: " + message.notificationId()));

            // Idempotência: Se já foi processado, apenas ignora e dá ACK (sucesso) para a fila
            if (notification.getStatus() == NotificationStatus.PROCESSED) {
                log.warn("Idempotência: Notificação {} já processada anteriormente.", message.notificationId());
                return;
            }

            // Encontra a estratégia correta (Email, SMS, etc)
            NotificationStrategy strategy = strategies.stream()
                    .filter(s -> s.getChannel().equals(message.channel()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Estratégia não encontrada para: " + message.channel()));

            strategy.send(message);

            // Atualiza o banco para consolidar o envio
            notification.setStatus(NotificationStatus.PROCESSED);
            notificationRepository.save(notification);

            log.info("Notificação {} processada com sucesso!", message.notificationId());

        } catch (JsonProcessingException e) {
            log.error("Erro ao converter JSON: {}", payload, e);
            // Em caso de JSON inválido, não adianta tentar de novo (DLQ direta)
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException(e);
        } catch (Exception e) {
            log.error("Erro ao processar notificação. Iniciando fluxo de retry...");
            throw e; // Lança para o RabbitMQ fazer o Retry (3 vezes) configurado no yml
        }
    }
}