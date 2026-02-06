package com.mystictarot.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystictarot.backend.dto.request.CardDTO;
import com.mystictarot.backend.dto.request.FollowUpRequestDTO;
import com.mystictarot.backend.dto.request.InterpretRequestDTO;
import com.mystictarot.backend.dto.response.FollowUpResponseDTO;
import com.mystictarot.backend.dto.response.InterpretResponseDTO;
import com.mystictarot.backend.entity.ChatMessage;
import com.mystictarot.backend.entity.Reading;
import com.mystictarot.backend.entity.TarotCard;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.CardOrientation;
import com.mystictarot.backend.entity.enums.ChatRole;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.entity.enums.ReadingStatus;
import com.mystictarot.backend.entity.enums.SpreadType;
import com.mystictarot.backend.entity.enums.SuitType;
import com.mystictarot.backend.exception.InsufficientCreditsException;
import com.mystictarot.backend.exception.ReadingLimitExceededException;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.exception.ValidationException;
import com.mystictarot.backend.repository.ChatMessageRepository;
import com.mystictarot.backend.repository.ReadingRepository;
import com.mystictarot.backend.repository.TarotCardRepository;
import com.mystictarot.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TarotService.
 * Given-When-Then; isolation; Mockito for dependencies.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Tarot Service Tests")
class TarotServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReadingRepository readingRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private TarotCardRepository tarotCardRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private TarotService tarotService;

    private UUID userId;
    private User testUser;
    private Reading testReading;
    private UUID readingId;
    private List<CardDTO> threeCards;
    private List<TarotCard> tarotCards;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        readingId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .name("Test User")
                .plan(PlanType.FREE)
                .extraCredits(2)
                .build();
        testReading = Reading.builder()
                .id(readingId)
                .user(testUser)
                .question("What should I focus on?")
                .spreadType(SpreadType.THREE_CARDS)
                .cardsJson("[{\"id\":1,\"orientation\":\"UPRIGHT\"}]")
                .interpretationText("Initial interpretation.")
                .status(ReadingStatus.ACTIVE)
                .build();
        threeCards = List.of(
                CardDTO.builder().id(1).orientation(CardOrientation.UPRIGHT).build(),
                CardDTO.builder().id(2).orientation(CardOrientation.REVERSED).build(),
                CardDTO.builder().id(3).orientation(CardOrientation.UPRIGHT).build()
        );
        tarotCards = List.of(
                TarotCard.builder().id(1).name("The Magician").description("Magic").suit(SuitType.MAJOR_ARCANA).cardNumber(1).build(),
                TarotCard.builder().id(2).name("The High Priestess").description("Intuition").suit(SuitType.MAJOR_ARCANA).cardNumber(2).build(),
                TarotCard.builder().id(3).name("The Empress").description("Abundance").suit(SuitType.MAJOR_ARCANA).cardNumber(3).build()
        );
        ReflectionTestUtils.setField(tarotService, "freePlanLimit", 3);
        ReflectionTestUtils.setField(tarotService, "monthlyPlanLimit", 20);
        ReflectionTestUtils.setField(tarotService, "retail5PlanLimit", 5);
        ReflectionTestUtils.setField(tarotService, "objectMapper", new ObjectMapper());
    }

    @Test
    @DisplayName("Should interpret reading successfully when under quota")
    void shouldInterpretReading_SuccessfullyWhenUnderQuota() {
        InterpretRequestDTO request = InterpretRequestDTO.builder()
                .question("What should I focus on?")
                .spreadType(SpreadType.THREE_CARDS)
                .cards(threeCards)
                .build();
        String interpretation = "Your path suggests clarity.";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(readingRepository.countWeeklyReadingsByUserId(eq(userId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(1L);
        doReturn(tarotCards).when(tarotCardRepository).findAllById(anyIterable());
        when(geminiService.generateInterpretation(any(), eq(SpreadType.THREE_CARDS), anyString()))
                .thenReturn(interpretation);
        when(readingRepository.save(any(Reading.class))).thenAnswer(inv -> {
            Reading r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });

        InterpretResponseDTO response = tarotService.interpretReading(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getReadingId()).isNotNull();
        assertThat(response.getInterpretation()).isEqualTo(interpretation);
        verify(readingRepository).save(argThat(r ->
                r.getUser().getId().equals(userId)
                        && r.getQuestion().equals("What should I focus on?")
                        && r.getSpreadType() == SpreadType.THREE_CARDS
                        && r.getStatus() == ReadingStatus.ACTIVE
                        && r.getInterpretationText().equals(interpretation)));
        verify(geminiService).generateInterpretation(eq("What should I focus on?"), eq(SpreadType.THREE_CARDS), anyString());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found for interpret")
    void shouldThrowResourceNotFoundException_WhenUserNotFoundForInterpret() {
        InterpretRequestDTO request = InterpretRequestDTO.builder()
                .question("Q")
                .spreadType(SpreadType.THREE_CARDS)
                .cards(threeCards)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tarotService.interpretReading(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(geminiService, never()).generateInterpretation(any(), any(), any());
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when card count does not match spread")
    void shouldThrowValidationException_WhenCardCountDoesNotMatchSpread() {
        InterpretRequestDTO request = InterpretRequestDTO.builder()
                .question("Q")
                .spreadType(SpreadType.THREE_CARDS)
                .cards(List.of(threeCards.get(0), threeCards.get(1)))
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> tarotService.interpretReading(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Number of cards must be 3");

        verify(tarotCardRepository, never()).findAllById(any());
        verify(geminiService, never()).generateInterpretation(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw ValidationException when card IDs are invalid")
    void shouldThrowValidationException_WhenCardIdsAreInvalid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(readingRepository.countWeeklyReadingsByUserId(eq(userId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        doReturn(List.of(tarotCards.get(0), tarotCards.get(1))).when(tarotCardRepository).findAllById(anyIterable());

        InterpretRequestDTO request = InterpretRequestDTO.builder()
                .question("Q")
                .spreadType(SpreadType.THREE_CARDS)
                .cards(threeCards)
                .build();

        assertThatThrownBy(() -> tarotService.interpretReading(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("invalid");

        verify(geminiService, never()).generateInterpretation(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw ReadingLimitExceededException when weekly limit reached")
    void shouldThrowReadingLimitExceededException_WhenWeeklyLimitReached() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(readingRepository.countWeeklyReadingsByUserId(eq(userId), eq(ReadingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(3L);
        doReturn(tarotCards).when(tarotCardRepository).findAllById(anyIterable());

        InterpretRequestDTO request = InterpretRequestDTO.builder()
                .question("Q")
                .spreadType(SpreadType.THREE_CARDS)
                .cards(threeCards)
                .build();

        assertThatThrownBy(() -> tarotService.interpretReading(userId, request))
                .isInstanceOf(ReadingLimitExceededException.class)
                .hasMessageContaining("limit reached");

        verify(geminiService, never()).generateInterpretation(any(), any(), any());
    }

    @Test
    @DisplayName("Should allow interpret when plan is UNLIMITED regardless of count")
    void shouldAllowInterpret_WhenPlanIsUnlimited() {
        testUser.setPlan(PlanType.UNLIMITED);
        String interpretation = "Unlimited insight.";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doReturn(tarotCards).when(tarotCardRepository).findAllById(anyIterable());
        when(geminiService.generateInterpretation(any(), eq(SpreadType.THREE_CARDS), anyString()))
                .thenReturn(interpretation);
        when(readingRepository.save(any(Reading.class))).thenAnswer(inv -> {
            Reading r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        InterpretRequestDTO request = InterpretRequestDTO.builder()
                .question("Q")
                .spreadType(SpreadType.THREE_CARDS)
                .cards(threeCards)
                .build();

        InterpretResponseDTO response = tarotService.interpretReading(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getInterpretation()).isEqualTo(interpretation);
        verify(readingRepository, never()).countWeeklyReadingsByUserId(any(), any(), any());
    }

    @Test
    @DisplayName("Should follow up successfully and decrement extra credits")
    void shouldFollowUp_SuccessfullyAndDecrementCredits() {
        FollowUpRequestDTO request = FollowUpRequestDTO.builder()
                .readingId(readingId)
                .message("Can you elaborate?")
                .build();
        String aiResponse = "Certainly. The second card suggests...";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(readingRepository.findByIdAndUserId(readingId, userId)).thenReturn(Optional.of(testReading));
        when(chatMessageRepository.findByReadingIdOrderByCreatedAtAsc(readingId)).thenReturn(List.of());
        when(geminiService.generateFollowUpResponse(anyString(), eq("Can you elaborate?")))
                .thenReturn(aiResponse);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpResponseDTO response = tarotService.followUp(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo(aiResponse);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getExtraCredits()).isEqualTo(1);
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found for follow-up")
    void shouldThrowResourceNotFoundException_WhenUserNotFoundForFollowUp() {
        FollowUpRequestDTO request = FollowUpRequestDTO.builder()
                .readingId(readingId)
                .message("Elaborate?")
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tarotService.followUp(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(readingRepository, never()).findByIdAndUserId(any(), any());
        verify(geminiService, never()).generateFollowUpResponse(any(), any());
    }

    @Test
    @DisplayName("Should throw InsufficientCreditsException when extra credits is zero")
    void shouldThrowInsufficientCreditsException_WhenExtraCreditsIsZero() {
        testUser.setExtraCredits(0);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        FollowUpRequestDTO request = FollowUpRequestDTO.builder()
                .readingId(readingId)
                .message("Elaborate?")
                .build();

        assertThatThrownBy(() -> tarotService.followUp(userId, request))
                .isInstanceOf(InsufficientCreditsException.class)
                .hasMessageContaining("Insufficient credits");

        verify(readingRepository, never()).findByIdAndUserId(any(), any());
        verify(geminiService, never()).generateFollowUpResponse(any(), any());
    }

    @Test
    @DisplayName("Should throw InsufficientCreditsException when extra credits is null")
    void shouldThrowInsufficientCreditsException_WhenExtraCreditsIsNull() {
        testUser.setExtraCredits(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        FollowUpRequestDTO request = FollowUpRequestDTO.builder()
                .readingId(readingId)
                .message("Elaborate?")
                .build();

        assertThatThrownBy(() -> tarotService.followUp(userId, request))
                .isInstanceOf(InsufficientCreditsException.class);

        verify(readingRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reading not found for follow-up")
    void shouldThrowResourceNotFoundException_WhenReadingNotFoundForFollowUp() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(readingRepository.findByIdAndUserId(readingId, userId)).thenReturn(Optional.empty());

        FollowUpRequestDTO request = FollowUpRequestDTO.builder()
                .readingId(readingId)
                .message("Elaborate?")
                .build();

        assertThatThrownBy(() -> tarotService.followUp(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reading");

        verify(geminiService, never()).generateFollowUpResponse(any(), any());
    }

    @Test
    @DisplayName("Should throw ValidationException when reading status is not ACTIVE for follow-up")
    void shouldThrowValidationException_WhenReadingStatusNotActiveForFollowUp() {
        testReading.setStatus(ReadingStatus.DELETED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(readingRepository.findByIdAndUserId(readingId, userId)).thenReturn(Optional.of(testReading));

        FollowUpRequestDTO request = FollowUpRequestDTO.builder()
                .readingId(readingId)
                .message("Elaborate?")
                .build();

        assertThatThrownBy(() -> tarotService.followUp(userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("no longer available");

        verify(geminiService, never()).generateFollowUpResponse(any(), any());
    }

    @Test
    @DisplayName("Should include existing chat messages in follow-up context")
    void shouldIncludeExistingChatMessages_InFollowUpContext() {
        ChatMessage existingUser = ChatMessage.builder().reading(testReading).role(ChatRole.USER).content("First Q").build();
        ChatMessage existingAi = ChatMessage.builder().reading(testReading).role(ChatRole.AI).content("First A").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(readingRepository.findByIdAndUserId(readingId, userId)).thenReturn(Optional.of(testReading));
        when(chatMessageRepository.findByReadingIdOrderByCreatedAtAsc(readingId))
                .thenReturn(List.of(existingUser, existingAi));
        when(geminiService.generateFollowUpResponse(anyString(), eq("Second Q?")))
                .thenReturn("Second A");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        FollowUpRequestDTO request = FollowUpRequestDTO.builder()
                .readingId(readingId)
                .message("Second Q?")
                .build();

        tarotService.followUp(userId, request);

        verify(geminiService).generateFollowUpResponse(contextCaptor.capture(), eq("Second Q?"));
        String context = contextCaptor.getValue();
        assertThat(context).contains("What should I focus on?");
        assertThat(context).contains("Initial interpretation.");
        assertThat(context).contains("USER: First Q");
        assertThat(context).contains("AI: First A");
    }
}
