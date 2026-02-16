package com.mystictarot.backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LocaleUtil {

    private final String defaultLocale;
    private final Set<String> supportedLocales;

    public LocaleUtil(
            @Value("${app.locale.default:vi}") String defaultLocale,
            @Value("${app.locale.supported:vi,en}") String supportedLocales) {
        this.defaultLocale = defaultLocale != null ? defaultLocale.trim() : "vi";
        this.supportedLocales = supportedLocales == null || supportedLocales.isBlank()
                ? Set.of("vi", "en")
                : Arrays.stream(supportedLocales.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public String normalize(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return defaultLocale;
        }
        String lower = languageCode.trim().toLowerCase().replace('-', '_');
        if (lower.startsWith("vi") || lower.equals("vn")) {
            return "vi";
        }
        if (lower.startsWith("en")) {
            return "en";
        }
        if (supportedLocales.contains(lower)) {
            return lower;
        }
        if (lower.length() >= 2 && supportedLocales.contains(lower.substring(0, 2))) {
            return lower.substring(0, 2);
        }
        return defaultLocale;
    }

    public String resolve(String languageCode) {
        String normalized = normalize(languageCode);
        return supportedLocales.contains(normalized) ? normalized : defaultLocale;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public Set<String> getSupportedLocales() {
        return Set.copyOf(supportedLocales);
    }
}
