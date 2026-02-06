package com.mystictarot.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystictarot.backend.entity.enums.SpreadType;
import com.mystictarot.backend.exception.GeminiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.base-url}")
    private String baseUrl;

    @Value("${gemini.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${gemini.read-timeout:60000}")
    private int readTimeout;

    @Value("${gemini.max-retries:3}")
    private int maxRetries;

    @Value("${gemini.retry-initial-delay:1000}")
    private long retryInitialDelay;

    @Value("${gemini.retry-max-delay:10000}")
    private long retryMaxDelay;

    private RestClient restClient;

    @jakarta.annotation.PostConstruct
    void initRestClient() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .build();
    }

    public String generateInterpretation(String question, SpreadType spreadType, String cardsDescriptionForPrompt) {
        String prompt = buildInterpretationPrompt(question, spreadType, cardsDescriptionForPrompt);
        return callGeminiWithRetry(prompt);
    }

    public String generateFollowUpResponse(String readingContext, String userMessage) {
        String prompt = buildFollowUpPrompt(readingContext, userMessage);
        return callGeminiWithRetry(prompt);
    }

    private String buildInterpretationPrompt(String question, SpreadType spreadType, String cardsDescriptionForPrompt) {
        return """
                You are an expert tarot reader. Provide a clear, insightful interpretation for this tarot reading.

                Spread type: %s

                Selected cards and orientations:
                %s

                Question from the seeker: %s

                Respond with a single coherent interpretation (2-4 paragraphs). Do not include titles or labels, only the interpretation text.
                """
                .formatted(spreadType.name(), cardsDescriptionForPrompt, question);
    }

    private String buildFollowUpPrompt(String readingContext, String userMessage) {
        return """
                You are an expert tarot reader. The seeker is asking a follow-up question about their previous reading.

                Previous reading context:
                %s

                Follow-up question from the seeker: %s

                Respond with a helpful, concise answer (1-3 paragraphs). Stay on topic and refer to the reading when relevant. Do not include titles or labels, only the answer text.
                """
                .formatted(readingContext, userMessage);
    }

    private String callGeminiWithRetry(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ));
        Exception lastException = null;
        long delay = retryInitialDelay;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return doCallGemini(body);
            } catch (Exception e) {
                lastException = e;
                if (e instanceof RestClientResponseException re && re.getStatusCode().is4xxClientError()) {
                    throw new GeminiServiceException("Gemini API client error: " + e.getMessage(), e);
                }
                log.warn("Gemini API call attempt {} failed: {}", attempt + 1, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new GeminiServiceException("Interrupted while retrying Gemini API", ie);
                    }
                    delay = Math.min(delay * 2, retryMaxDelay);
                }
            }
        }
        throw new GeminiServiceException("Gemini API failed after " + (maxRetries + 1) + " attempts", lastException);
    }

    private String doCallGemini(Map<String, Object> body) {
        String responseBody = restClient.post()
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new GeminiServiceException("Gemini API returned " + res.getStatusCode());
                })
                .body(String.class);

        return extractTextFromResponse(responseBody);
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                throw new GeminiServiceException("Gemini API returned no candidates");
            }
            JsonNode content = candidates.get(0).path("content").path("parts");
            if (content.isEmpty()) {
                throw new GeminiServiceException("Gemini API returned empty content");
            }
            String text = content.get(0).path("text").asText();
            return text != null ? text.trim() : "";
        } catch (GeminiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiServiceException("Failed to parse Gemini response", e);
        }
    }
}
