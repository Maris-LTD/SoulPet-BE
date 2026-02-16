package com.mystictarot.backend.runner;

import com.mystictarot.backend.service.TarotSheetImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MAX_VALUE - 100)
@ConditionalOnProperty(name = "app.import.tarot.enabled", havingValue = "true")
public class TarotSheetImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TarotSheetImportRunner.class);

    private final TarotSheetImportService tarotSheetImportService;
    private final String csvUrl;

    public TarotSheetImportRunner(TarotSheetImportService tarotSheetImportService,
                                  @Value("${app.import.tarot.csv-url}") String csvUrl) {
        this.tarotSheetImportService = tarotSheetImportService;
        this.csvUrl = csvUrl;
    }

    @Override
    public void run(String... args) {
        log.info("Tarot import enabled: fetching CSV from configured URL and upserting tarot_cards + tarot_card_translations");
        try {
            TarotSheetImportService.ImportResult result = tarotSheetImportService.runImport(csvUrl);
            log.info("Tarot import completed: cards upserted={}, translations upserted={}, rows skipped={}",
                    result.cardsUpserted(), result.translationsUpserted(), result.rowsSkipped());
        } catch (Exception e) {
            log.error("Tarot import failed", e);
            throw new RuntimeException(e);
        }
    }
}
