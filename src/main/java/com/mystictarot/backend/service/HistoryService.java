package com.mystictarot.backend.service;

import com.mystictarot.backend.dto.response.ChatMessageItemDTO;
import com.mystictarot.backend.dto.response.ReadingDetailDTO;
import com.mystictarot.backend.dto.response.ReadingHistoryItemDTO;
import com.mystictarot.backend.entity.ChatMessage;
import com.mystictarot.backend.entity.Reading;
import com.mystictarot.backend.entity.enums.ReadingStatus;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.repository.ChatMessageRepository;
import com.mystictarot.backend.repository.ReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private static final int QUESTION_TRUNCATE_LENGTH = 100;

    private final ReadingRepository readingRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public Page<ReadingHistoryItemDTO> getHistory(UUID userId, Pageable pageable) {
        Page<Reading> page = readingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, ReadingStatus.ACTIVE, pageable);
        return page.map(this::toHistoryItemDTO);
    }

    @Transactional(readOnly = true)
    public ReadingDetailDTO getReadingDetail(UUID readingId, UUID userId) {
        Reading reading = readingRepository.findByIdAndUserId(readingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reading", readingId));
        List<ChatMessage> messages = chatMessageRepository.findByReadingIdOrderByCreatedAtAsc(readingId);
        return toDetailDTO(reading, messages);
    }

    @Transactional
    public void deleteReading(UUID readingId, UUID userId) {
        Reading reading = readingRepository.findByIdAndUserId(readingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reading", readingId));
        reading.setStatus(ReadingStatus.DELETED);
        reading.setDeletedAt(LocalDateTime.now());
        readingRepository.save(reading);
    }

    private ReadingHistoryItemDTO toHistoryItemDTO(Reading r) {
        String question = r.getQuestion();
        if (question != null && question.length() > QUESTION_TRUNCATE_LENGTH) {
            question = question.substring(0, QUESTION_TRUNCATE_LENGTH) + "...";
        }
        return ReadingHistoryItemDTO.builder()
                .id(r.getId())
                .question(question)
                .spreadType(r.getSpreadType())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ReadingDetailDTO toDetailDTO(Reading reading, List<ChatMessage> messages) {
        List<ChatMessageItemDTO> chatDtos = messages.stream()
                .map(this::toChatMessageItemDTO)
                .collect(Collectors.toList());
        return ReadingDetailDTO.builder()
                .id(reading.getId())
                .question(reading.getQuestion())
                .spreadType(reading.getSpreadType())
                .cardsJson(reading.getCardsJson())
                .interpretationText(reading.getInterpretationText())
                .createdAt(reading.getCreatedAt())
                .chatMessages(chatDtos)
                .build();
    }

    private ChatMessageItemDTO toChatMessageItemDTO(ChatMessage m) {
        return ChatMessageItemDTO.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
