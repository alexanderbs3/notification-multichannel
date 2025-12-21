package br.leetjourney.notificationmultichannel.api.controller;


import br.leetjourney.notificationmultichannel.api.dto.NotificationRequestDTO;
import br.leetjourney.notificationmultichannel.api.dto.NotificationResponseDTO;
import br.leetjourney.notificationmultichannel.application.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponseDTO> send(@RequestBody @Valid NotificationRequestDTO request) {
        // Chamamos o service e ele nos retorna o objeto salvo com ID
        NotificationResponseDTO response = notificationService.createNotification(request);
        return ResponseEntity.accepted().body(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDTO> getStatus(@PathVariable UUID id){
        return ResponseEntity.ok(notificationService.getNotificationStatus(id));
    }
}
