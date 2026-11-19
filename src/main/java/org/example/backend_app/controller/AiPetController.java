
package org.example.backend_app.controller;

import jakarta.validation.Valid;

import org.example.backend_app.dto.AiChatRequest;
import org.example.backend_app.dto.AiChatResponse;
import org.example.backend_app.service.AiPetService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiPetController {

    private final AiPetService aiPetService;

    public AiPetController(AiPetService aiPetService) {
        this.aiPetService = aiPetService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request) {

        String reply = aiPetService.chat(
                request.getMessage().trim()
        );

        return ResponseEntity.ok(
                new AiChatResponse(reply)
        );
    }
}
