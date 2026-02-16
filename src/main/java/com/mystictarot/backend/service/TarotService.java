package com.mystictarot.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystictarot.backend.dto.request.CardDTO;
import com.mystictarot.backend.dto.request.FollowUpRequestDTO;
import com.mystictarot.backend.dto.request.InterpretRequestDTO;
import com.mystictarot.backend.dto.response.FollowUpResponseDTO;
import com.mystictarot.backend.dto.response.InterpretResponseDTO;
import com.mystictarot.backend.dto.response.TarotCardResponseDTO;
import com.mystictarot.backend.entity.ChatMessage;
import com.mystictarot.backend.entity.Reading;
import com.mystictarot.backend.entity.TarotCard;
import com.mystictarot.backend.entity.TarotCardTranslation;
import com.mystictarot.backend.entity.User;
import com.mystictarot.backend.entity.enums.ChatRole;
import com.mystictarot.backend.entity.enums.PlanType;
import com.mystictarot.backend.entity.enums.ReadingStatus;
import com.mystictarot.backend.entity.enums.SpreadType;
import com.mystictarot.backend.exception.InsufficientCreditsException;
import com.mystictarot.backend.exception.ReadingLimitExceededException;
import com.mystictarot.backend.exception.ResourceNotFoundException;
import com.mystictarot.backend.exception.ValidationException;
import com.mystictarot.backend.repository.ChatMessageRepository;
import com.mystictarot.backend.repository.ReadingRepository;
import com.mystictarot.backend.repository.TarotCardRepository;
import com.mystictarot.backend.repository.TarotCardTranslationRepository;
import com.mystictarot.backend.repository.UserRepository;
import com.mystictarot.backend.util.LocaleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TarotService {

    private static final Map<SpreadType, Integer> EXPECTED_CARD_COUNT = Map.of(
            SpreadType.THREE_CARDS, 3,
            SpreadType.CELTIC_CROSS, 10,
            SpreadType.DAILY_DRAW, 1,
            SpreadType.PAST_PRESENT_FUTURE, 3,
            SpreadType.RELATIONSHIP_SPREAD, 2
    );

    private final UserRepository userRepository;
    private final ReadingRepository readingRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TarotCardRepository tarotCardRepository;
    private final TarotCardTranslationRepository tarotCardTranslationRepository;
    private final GeminiService geminiService;
    private final LocaleUtil localeUtil;
    private final ObjectMapper objectMapper;

    @Value("${subscription.plan.limits.free:3}")
    private Integer freePlanLimit;

    @Value("${subscription.plan.limits.monthly:20}")
    private Integer monthlyPlanLimit;

    @Value("${subscription.plan.limits.retail5:5}")
    private Integer retail5PlanLimit;

    @Transactional
    public InterpretResponseDTO interpretReading(UUID userId, InterpretRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        validateInterpretRequest(request);
        validateReadingQuota(userId, user.getPlan());

        String locale = localeUtil.resolve(request.getLang());
        String cardsJson = serializeCardsToJson(request.getCards());
        String cardsDescription = buildCardsDescriptionForPrompt(request.getCards(), locale);
        String interpretation = geminiService.generateInterpretation(
                request.getQuestion(), request.getSpreadType(), cardsDescription, locale);

        Reading reading = Reading.builder()
                .user(user)
                .question(request.getQuestion().trim())
                .spreadType(request.getSpreadType())
                .cardsJson(cardsJson)
                .interpretationText(interpretation)
                .status(ReadingStatus.ACTIVE)
                .build();
        reading = readingRepository.save(reading);
        log.info("Reading created: readingId={}, userId={}", reading.getId(), userId);

        return InterpretResponseDTO.builder()
                .readingId(reading.getId())
                .interpretation(interpretation)
                .build();
    }

    @Transactional
    public FollowUpResponseDTO followUp(UUID userId, FollowUpRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getExtraCredits() == null || user.getExtraCredits() < 1) {
            throw new InsufficientCreditsException("Insufficient credits for follow-up. Purchase extra credits to continue.");
        }

        Reading reading = readingRepository.findByIdAndUserId(request.getReadingId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reading", request.getReadingId()));

        if (reading.getStatus() != ReadingStatus.ACTIVE) {
            throw new ValidationException("This reading is no longer available for follow-up.");
        }

        List<ChatMessage> existingMessages = chatMessageRepository.findByReadingIdOrderByCreatedAtAsc(reading.getId());
        String context = buildFollowUpContext(reading, existingMessages);
        String aiResponse = geminiService.generateFollowUpResponse(context, request.getMessage().trim());

        ChatMessage userMsg = ChatMessage.builder()
                .reading(reading)
                .role(ChatRole.USER)
                .content(request.getMessage().trim())
                .build();
        ChatMessage aiMsg = ChatMessage.builder()
                .reading(reading)
                .role(ChatRole.AI)
                .content(aiResponse)
                .build();
        chatMessageRepository.save(userMsg);
        chatMessageRepository.save(aiMsg);

        user.setExtraCredits(user.getExtraCredits() - 1);
        userRepository.save(user);
        log.info("Follow-up completed: readingId={}, userId={}, creditsRemaining={}", reading.getId(), userId, user.getExtraCredits());

        return FollowUpResponseDTO.builder()
                .content(aiResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TarotCardResponseDTO> getDeck(String lang) {
        String locale = localeUtil.resolve(lang);
        List<TarotCardTranslation> primary = tarotCardTranslationRepository.findAllByLocaleOrderByTarotCard_CardNumberAsc(locale);
        String defaultLocale = localeUtil.getDefaultLocale();
        List<TarotCardTranslation> fallback = locale.equals(defaultLocale)
                ? List.of()
                : tarotCardTranslationRepository.findAllByLocaleOrderByTarotCard_CardNumberAsc(defaultLocale);
        fallback.stream()
                .collect(Collectors.toMap(t -> t.getTarotCard().getId(), t -> t));
        List<TarotCardResponseDTO> result = new ArrayList<>();
        for (TarotCardTranslation t : primary) {
            TarotCard card = t.getTarotCard();
            result.add(TarotCardResponseDTO.builder()
                    .id(card.getId())
                    .cardNumber(card.getCardNumber())
                    .suit(card.getSuit())
                    .name(t.getName())
                    .description(t.getDescription())
                    .imageUrl(card.getImageUrl())
                    .build());
        }
        for (TarotCardTranslation t : fallback) {
            int cardId = t.getTarotCard().getId();
            if (primary.stream().noneMatch(p -> p.getTarotCard().getId() == cardId)) {
                TarotCard card = t.getTarotCard();
                result.add(TarotCardResponseDTO.builder()
                        .id(card.getId())
                        .cardNumber(card.getCardNumber())
                        .suit(card.getSuit())
                        .name(t.getName())
                        .description(t.getDescription())
                        .imageUrl(card.getImageUrl())
                        .build());
            }
        }
        result.sort(Comparator.comparing(TarotCardResponseDTO::getId));
        return result;
    }

    private void validateInterpretRequest(InterpretRequestDTO request) {
        Integer expected = EXPECTED_CARD_COUNT.get(request.getSpreadType());
        if (expected == null) {
            throw new ValidationException("Invalid spread type: " + request.getSpreadType());
        }
        List<CardDTO> cards = request.getCards();
        if (cards == null || cards.size() != expected) {
            throw new ValidationException(
                    "Number of cards must be " + expected + " for spread type " + request.getSpreadType());
        }
        Set<Integer> cardIds = cards.stream().map(CardDTO::getId).collect(Collectors.toSet());
        List<TarotCard> existing = tarotCardRepository.findAllById(cardIds);
        if (existing.size() != cardIds.size()) {
            throw new ValidationException("One or more card IDs are invalid. All card IDs must exist in the deck.");
        }
    }

    private void validateReadingQuota(UUID userId, PlanType plan) {
        Integer limit = getWeeklyReadingsLimit(plan);
        if (limit != null && limit < 0) {
            return;
        }
        LocalDateTime weekStart = getStartOfCurrentWeek();
        long used = readingRepository.countWeeklyReadingsByUserId(userId, ReadingStatus.ACTIVE, weekStart);
        if (limit != null && used >= limit) {
            throw new ReadingLimitExceededException(
                    "Weekly reading limit reached (" + used + "/" + limit + "). Upgrade your plan or wait until next week.");
        }
    }

    private Integer getWeeklyReadingsLimit(PlanType plan) {
        return switch (plan) {
            case FREE -> freePlanLimit;
            case MONTHLY -> monthlyPlanLimit;
            case UNLIMITED -> -1;
            case RETAIL_5 -> retail5PlanLimit;
        };
    }

    private LocalDateTime getStartOfCurrentWeek() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        int daysToSubtract = dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue();
        if (daysToSubtract < 0) {
            daysToSubtract += 7;
        }
        return now.minusDays(daysToSubtract)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    private String serializeCardsToJson(List<CardDTO> cards) {
        try {
            return objectMapper.writeValueAsString(cards);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Invalid cards data");
        }
    }

    private String buildCardsDescriptionForPrompt(List<CardDTO> cards, String locale) {
        List<Integer> cardIds = cards.stream().map(CardDTO::getId).toList();
        List<TarotCardTranslation> translations = tarotCardTranslationRepository.findByTarotCard_IdInAndLocale(cardIds, locale);
        String defaultLocale = localeUtil.getDefaultLocale();
        if (translations.size() < cardIds.size() && !locale.equals(defaultLocale)) {
            Set<Integer> haveIds = translations.stream().map(t -> t.getTarotCard().getId()).collect(Collectors.toSet());
            List<Integer> missing = cardIds.stream().filter(id -> !haveIds.contains(id)).toList();
            List<TarotCardTranslation> fallback = tarotCardTranslationRepository.findByTarotCard_IdInAndLocale(missing, defaultLocale);
            translations = new ArrayList<>(translations);
            for (TarotCardTranslation t : fallback) {
                if (!haveIds.contains(t.getTarotCard().getId())) {
                    translations.add(t);
                }
            }
        }
        Map<Integer, TarotCardTranslation> byCardId = translations.stream().collect(Collectors.toMap(t -> t.getTarotCard().getId(), t -> t));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            CardDTO dto = cards.get(i);
            TarotCardTranslation trans = byCardId.get(dto.getId());
            if (trans != null) {
                sb.append(i + 1).append(". ").append(trans.getName())
                        .append(" (").append(dto.getOrientation().name()).append(")");
                if (trans.getDescription() != null && !trans.getDescription().isBlank()) {
                    sb.append(": ").append(trans.getDescription());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String buildFollowUpContext(Reading reading, List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(reading.getQuestion()).append("\n");
        sb.append("Initial interpretation: ").append(reading.getInterpretationText()).append("\n");
        if (!messages.isEmpty()) {
            sb.append("Previous Q&A:\n");
            for (ChatMessage m : messages) {
                sb.append(m.getRole().name()).append(": ").append(m.getContent()).append("\n");
            }
        }
        return sb.toString();
    }
}
