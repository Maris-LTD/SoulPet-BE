package com.mystictarot.backend.controller;

import com.mystictarot.backend.dto.response.ReadingDetailDTO;
import com.mystictarot.backend.dto.response.ReadingHistoryItemDTO;
import com.mystictarot.backend.entity.enums.SpreadType;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.service.HistoryService;
import com.mystictarot.backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("History Controller Tests")
class HistoryControllerTest {

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private HistoryController historyController;

    private UUID userId;
    private UUID readingId;
    private ReadingHistoryItemDTO historyItem;
    private ReadingDetailDTO detailDTO;
    private Page<ReadingHistoryItemDTO> historyPage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        readingId = UUID.randomUUID();
        historyItem = ReadingHistoryItemDTO.builder()
                .id(readingId)
                .question("What will happen?")
                .spreadType(SpreadType.THREE_CARDS)
                .createdAt(LocalDateTime.now())
                .build();
        detailDTO = ReadingDetailDTO.builder()
                .id(readingId)
                .question("What will happen?")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[]")
                .interpretationText("Clarity.")
                .createdAt(LocalDateTime.now())
                .chatMessages(List.of())
                .build();
        historyPage = new PageImpl<>(List.of(historyItem), PageRequest.of(0, 10), 1);
    }

    @Test
    @DisplayName("Should get history and return 200 OK with page")
    void shouldGetHistory_AndReturn200OkWithPage() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(historyService.getHistory(eq(userId), any())).thenReturn(historyPage);

            ResponseEntity<Page<ReadingHistoryItemDTO>> response = historyController.getHistory(PageRequest.of(0, 10));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0).getId()).isEqualTo(readingId);
            verify(historyService).getHistory(eq(userId), any());
        }
    }

    @Test
    @DisplayName("Should get reading detail and return 200 OK")
    void shouldGetReadingDetail_AndReturn200Ok() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(historyService.getReadingDetail(readingId, userId)).thenReturn(detailDTO);

            ResponseEntity<ReadingDetailDTO> response = historyController.getReadingDetail(readingId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(readingId);
            assertThat(response.getBody().getQuestion()).isEqualTo("What will happen?");
            verify(historyService).getReadingDetail(readingId, userId);
        }
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reading not found for detail")
    void shouldThrowResourceNotFoundException_WhenReadingNotFoundForDetail() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(historyService.getReadingDetail(readingId, userId))
                    .thenThrow(new ResourceNotFoundException("Reading", readingId));

            assertThatThrownBy(() -> historyController.getReadingDetail(readingId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(historyService).getReadingDetail(readingId, userId);
        }
    }

    @Test
    @DisplayName("Should delete reading and return 204 No Content")
    void shouldDeleteReading_AndReturn204NoContent() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            doNothing().when(historyService).deleteReading(readingId, userId);

            ResponseEntity<Void> response = historyController.deleteReading(readingId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();
            verify(historyService).deleteReading(readingId, userId);
        }
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reading not found for delete")
    void shouldThrowResourceNotFoundException_WhenReadingNotFoundForDelete() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            doThrow(new ResourceNotFoundException("Reading", readingId)).when(historyService).deleteReading(readingId, userId);

            assertThatThrownBy(() -> historyController.deleteReading(readingId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(historyService).deleteReading(readingId, userId);
        }
    }
}
