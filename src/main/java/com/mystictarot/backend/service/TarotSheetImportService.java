package com.mystictarot.backend.service;

import com.mystictarot.backend.entity.TarotCard;
import com.mystictarot.backend.entity.TarotCardTranslation;
import com.mystictarot.backend.entity.enums.SuitType;
import com.mystictarot.backend.repository.TarotCardRepository;
import com.mystictarot.backend.repository.TarotCardTranslationRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class TarotSheetImportService {

    private static final Logger log = LoggerFactory.getLogger(TarotSheetImportService.class);
    private static final int HEADER_ROWS = 2;
    private static final int MIN_COLUMNS = 18;
    private static final String[] LOCALES = {"vi", "en", "zh", "fr", "it"};

    private final TarotCardRepository tarotCardRepository;
    private final TarotCardTranslationRepository tarotCardTranslationRepository;

    public TarotSheetImportService(TarotCardRepository tarotCardRepository,
                                  TarotCardTranslationRepository tarotCardTranslationRepository) {
        this.tarotCardRepository = tarotCardRepository;
        this.tarotCardTranslationRepository = tarotCardTranslationRepository;
    }

    @Transactional
    public ImportResult runImport(String csvUrl) {
        String csv;
        try {
            csv = fetchCsv(csvUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch CSV from URL", e);
        }
        List<CSVRecord> rows = parseCsv(csv);
        int dataRowCount = Math.max(0, rows.size() - HEADER_ROWS);
        log.info("Tarot import: CSV parsed, total rows={}, data rows={}, first data row columns={}",
                rows.size(), dataRowCount,
                dataRowCount > 0 ? rows.get(HEADER_ROWS).size() : 0);
        int cardsUpserted = 0;
        int translationsUpserted = 0;
        int rowsSkipped = 0;
        for (int i = HEADER_ROWS; i < rows.size(); i++) {
            CSVRecord record = rows.get(i);
            if (record.size() < MIN_COLUMNS) {
                log.warn("Tarot import: row {} skipped, column count={} (need >= {})", i + 1, record.size(), MIN_COLUMNS);
                rowsSkipped++;
                continue;
            }
            Integer cardNumber = parseCardNumber(record.get(0));
            if (cardNumber == null || cardNumber < 0 || cardNumber > 77) {
                log.warn("Tarot import: row {} skipped, invalid card_number='{}'", i + 1, record.get(0));
                rowsSkipped++;
                continue;
            }
            SuitType suit = mapSuit(record.get(1));
            if (suit == null) {
                log.warn("Tarot import: row {} skipped, suit not mapped='{}'", i + 1, record.get(1));
                rowsSkipped++;
                continue;
            }
            String imageUrl = nullOrTrim(record.get(2));
            int id = cardNumber + 1;
            TarotCard card = upsertCard(id, cardNumber, suit, imageUrl);
            cardsUpserted++;
            if (cardsUpserted == 1 || cardsUpserted % 20 == 0) {
                log.info("Tarot import progress: card {}/{} (id={}, {})", cardsUpserted, dataRowCount, id, record.get(4));
            }
            for (int locIdx = 0; locIdx < LOCALES.length; locIdx++) {
                int base = 3 + locIdx * 3;
                String name = nullOrTrim(record.get(base + 1));
                if (name == null || name.isEmpty()) continue;
                String description = nullOrTrim(record.get(base + 2));
                upsertTranslation(card, LOCALES[locIdx], name, description);
                translationsUpserted++;
            }
        }
        return new ImportResult(cardsUpserted, translationsUpserted, rowsSkipped);
    }

    private String fetchCsv(String csvUrl) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(csvUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/csv,text/plain,*/*")
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        String body = new String(response.body(), StandardCharsets.UTF_8);
        if (response.statusCode() != 200) {
            throw new RuntimeException("CSV fetch failed: HTTP " + response.statusCode());
        }
        if (body.trim().startsWith("<!") || body.trim().startsWith("<html")) {
            log.warn("Tarot import: response looks like HTML, not CSV. Check URL and access (Publish to web).");
            throw new RuntimeException("Server returned HTML instead of CSV - check URL and Publish to web settings");
        }
        return body;
    }

    private List<CSVRecord> parseCsv(String csv) {
        List<CSVRecord> rows = parseCsvWithDelimiter(csv, ',');
        if (rows.size() > HEADER_ROWS && rows.get(HEADER_ROWS).size() == 1) {
            log.info("Tarot import: comma delimiter gave 1 column, retrying with semicolon (Google Sheets locale)");
            rows = parseCsvWithDelimiter(csv, ';');
        }
        return rows;
    }

    private List<CSVRecord> parseCsvWithDelimiter(String csv, char delimiter) {
        try (StringReader reader = new StringReader(csv);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setDelimiter(delimiter)
                     .setSkipHeaderRecord(false)
                     .setIgnoreEmptyLines(false)
                     .build())) {
            return parser.getRecords();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV", e);
        }
    }

    private Integer parseCardNumber(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static SuitType mapSuit(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim().toUpperCase().replace(" ", "_");
        if ("MAJOR_ARCANA".equals(v)) return SuitType.MAJOR_ARCANA;
        if ("WANDS".equals(v)) return SuitType.WANDS;
        if ("CUPS".equals(v)) return SuitType.CUPS;
        if ("SWORDS".equals(v)) return SuitType.SWORDS;
        if ("PENTACLES".equals(v)) return SuitType.PENTACLES;
        return null;
    }

    private static String nullOrTrim(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private TarotCard upsertCard(int id, int cardNumber, SuitType suit, String imageUrl) {
        Optional<TarotCard> existing = tarotCardRepository.findById(id);
        TarotCard card;
        if (existing.isPresent()) {
            card = existing.get();
            card.setCardNumber(cardNumber);
            card.setSuit(suit);
            card.setImageUrl(imageUrl);
        } else {
            card = TarotCard.builder()
                    .id(id)
                    .cardNumber(cardNumber)
                    .suit(suit)
                    .imageUrl(imageUrl)
                    .build();
        }
        return tarotCardRepository.save(card);
    }

    private void upsertTranslation(TarotCard card, String locale, String name, String description) {
        Optional<TarotCardTranslation> existing = tarotCardTranslationRepository.findByTarotCard_IdAndLocale(card.getId(), locale);
        TarotCardTranslation trans;
        if (existing.isPresent()) {
            trans = existing.get();
            trans.setName(name);
            trans.setDescription(description);
        } else {
            trans = TarotCardTranslation.builder()
                    .tarotCard(card)
                    .locale(locale)
                    .name(name)
                    .description(description)
                    .build();
        }
        tarotCardTranslationRepository.save(trans);
    }

    public record ImportResult(int cardsUpserted, int translationsUpserted, int rowsSkipped) {}
}
