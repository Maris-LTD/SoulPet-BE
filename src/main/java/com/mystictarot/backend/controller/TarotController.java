package com.mystictarot.backend.controller;

import com.mystictarot.backend.dto.request.FollowUpRequestDTO;
import com.mystictarot.backend.dto.request.InterpretRequestDTO;
import com.mystictarot.backend.dto.response.FollowUpResponseDTO;
import com.mystictarot.backend.dto.response.InterpretResponseDTO;
import com.mystictarot.backend.service.TarotService;
import com.mystictarot.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/tarot")
@RequiredArgsConstructor
@Tag(name = "Tarot", description = "Tarot reading interpretation and follow-up APIs")
public class TarotController {

    private final TarotService tarotService;

    @PostMapping("/interpret")
    @Operation(summary = "Interpret tarot reading", description = "Submit selected cards and question to get AI-powered interpretation. Consumes weekly reading quota.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interpretation generated successfully",
                    content = @Content(schema = @Schema(implementation = InterpretResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (wrong card count, invalid card IDs)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Weekly reading limit exceeded"),
            @ApiResponse(responseCode = "502", description = "AI service temporarily unavailable")
    })
    public ResponseEntity<InterpretResponseDTO> interpret(@Valid @RequestBody InterpretRequestDTO request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        InterpretResponseDTO response = tarotService.interpretReading(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/follow-up")
    @Operation(summary = "Ask follow-up question", description = "Ask a follow-up question about an existing reading. Consumes 1 extra credit.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Follow-up answer generated successfully",
                    content = @Content(schema = @Schema(implementation = FollowUpResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient credits for follow-up"),
            @ApiResponse(responseCode = "404", description = "Reading not found"),
            @ApiResponse(responseCode = "502", description = "AI service temporarily unavailable")
    })
    public ResponseEntity<FollowUpResponseDTO> followUp(@Valid @RequestBody FollowUpRequestDTO request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        FollowUpResponseDTO response = tarotService.followUp(userId, request);
        return ResponseEntity.ok(response);
    }
}
