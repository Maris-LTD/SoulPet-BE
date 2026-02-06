package com.mystictarot.backend.controller;

import com.mystictarot.backend.dto.response.ReadingDetailDTO;
import com.mystictarot.backend.dto.response.ReadingHistoryItemDTO;
import com.mystictarot.backend.service.HistoryService;
import com.mystictarot.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
@Tag(name = "History", description = "Reading history APIs")
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    @Operation(summary = "Get reading history", description = "Retrieve paginated list of active readings for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    public ResponseEntity<Page<ReadingHistoryItemDTO>> getHistory(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Page<ReadingHistoryItemDTO> page = historyService.getHistory(userId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reading detail", description = "Retrieve full reading with chat messages by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading detail retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ReadingDetailDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "404", description = "Reading not found or not owned by user")
    })
    public ResponseEntity<ReadingDetailDTO> getReadingDetail(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ReadingDetailDTO detail = historyService.getReadingDetail(id, userId);
        return ResponseEntity.ok(detail);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete reading", description = "Soft delete a reading from history")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reading deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "404", description = "Reading not found or not owned by user")
    })
    public ResponseEntity<Void> deleteReading(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        historyService.deleteReading(id, userId);
        return ResponseEntity.noContent().build();
    }
}
