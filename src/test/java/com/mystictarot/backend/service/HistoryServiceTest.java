package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.response.ReadingDetailDTO;
import com.mystictarot.backend.dto.response.ReadingHistoryItemDTO;
import com.mystictarot.backend.entity.ChatMessage;
import com.mystictarot.backend.entity.Reading;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.ChatRole;
import com.mystictarot.backend.entity.enums.ReadingStatus;
import com.mystictarot.backend.entity.enums.SpreadType;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.repository.ChatMessageRepository;
import com.mystictarot.backend.repository.ReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("History Service Tests")
class HistoryServiceTest {

    @Mock
    private ReadingRepository readingRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private HistoryService historyService;

    private UUID userId;
    private UUID readingId;
    private User testUser;
    private Reading testReading;
    private ChatMessage testMessage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        readingId = UUID.randomUUID();
        testUser = User.builder().id(userId).email("u@e.com").name("User").build();
        testReading = Reading.builder()
                .id(readingId)
                .user(testUser)
                .question("What will happen?")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .interpretationText("You will find clarity.")
                .status(ReadingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        testMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .reading(testReading)
                .role(ChatRole.USER)
                .content("Tell me more")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should return paginated history with ACTIVE readings only")
    void shouldReturnPaginatedHistory_WithActiveReadingsOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reading> readingPage = new PageImpl<>(List.of(testReading), pageable, 1);
        when(readingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(eq(userId), eq(ReadingStatus.ACTIVE), eq(pageable)))
                .thenReturn(readingPage);

        Page<ReadingHistoryItemDTO> result = historyService.getHistory(userId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        ReadingHistoryItemDTO item = result.getContent().get(0);
        assertThat(item.getId()).isEqualTo(readingId);
        assertThat(item.getQuestion()).isEqualTo("What will happen?");
        assertThat(item.getSpreadType()).isEqualTo(SpreadType.THREE_CARDS);
        assertThat(item.getCreatedAt()).isNotNull();
        verify(readingRepository).findByUserIdAndStatusOrderByCreatedAtDesc(eq(userId), eq(ReadingStatus.ACTIVE), eq(pageable));
        verify(chatMessageRepository, never()).findByReadingIdOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("Should truncate long question in history item")
    void shouldTruncateLongQuestion_InHistoryItem() {
        String longQuestion = "a".repeat(150);
        testReading.setQuestion(longQuestion);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reading> readingPage = new PageImpl<>(List.of(testReading), pageable, 1);
        when(readingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(eq(userId), eq(ReadingStatus.ACTIVE), eq(pageable)))
                .thenReturn(readingPage);

        Page<ReadingHistoryItemDTO> result = historyService.getHistory(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getQuestion()).endsWith("...");
        assertThat(result.getContent().get(0).getQuestion().length()).isLessThanOrEqualTo(104);
    }

    @Test
    @DisplayName("Should return reading detail with chat messages")
    void shouldReturnReadingDetail_WithChatMessages() {
        when(readingRepository.findByIdAndUserId(readingId, userId)).thenReturn(Optional.of(testReading));
        when(chatMessageRepository.findByReadingIdOrderByCreatedAtAsc(readingId)).thenReturn(List.of(testMessage));

        ReadingDetailDTO result = historyService.getReadingDetail(readingId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(readingId);
        assertThat(result.getQuestion()).isEqualTo("What will happen?");
        assertThat(result.getSpreadType()).isEqualTo(SpreadType.THREE_CARDS);
        assertThat(result.getCardsJson()).isEqualTo("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]");
        assertThat(result.getInterpretationText()).isEqualTo("You will find clarity.");
        assertThat(result.getChatMessages()).hasSize(1);
        assertThat(result.getChatMessages().get(0).getRole()).isEqualTo(ChatRole.USER);
        assertThat(result.getChatMessages().get(0).getContent()).isEqualTo("Tell me more");
        verify(readingRepository).findByIdAndUserId(readingId, userId);
        verify(chatMessageRepository).findByReadingIdOrderByCreatedAtAsc(readingId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reading not found for detail")
    void shouldThrowResourceNotFoundException_WhenReadingNotFoundForDetail() {
        when(readingRepository.findByIdAndUserId(readingId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> historyService.getReadingDetail(readingId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reading");

        verify(chatMessageRepository, never()).findByReadingIdOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("Should soft delete reading successfully")
    void shouldSoftDeleteReading_Successfully() {
        when(readingRepository.findByIdAndUserId(readingId, userId)).thenReturn(Optional.of(testReading));
        when(readingRepository.save(any(Reading.class))).thenAnswer(inv -> inv.getArgument(0));

        historyService.deleteReading(readingId, userId);

        verify(readingRepository).findByIdAndUserId(readingId, userId);
        verify(readingRepository).save(argThat(r ->
                r.getStatus() == ReadingStatus.DELETED && r.getDeletedAt() != null));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reading not found for delete")
    void shouldThrowResourceNotFoundException_WhenReadingNotFoundForDelete() {
        when(readingRepository.findByIdAndUserId(readingId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> historyService.deleteReading(readingId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reading");

        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return empty page when user has no readings")
    void shouldReturnEmptyPage_WhenUserHasNoReadings() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reading> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(readingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(eq(userId), eq(ReadingStatus.ACTIVE), eq(pageable)))
                .thenReturn(emptyPage);

        Page<ReadingHistoryItemDTO> result = historyService.getHistory(userId, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}
