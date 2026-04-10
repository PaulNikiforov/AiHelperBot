package com.nikiforov.aichatbot.adapter.out.language;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.nikiforov.aichatbot.port.out.LanguageDetectionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LinguaLanguageAdapter implements LanguageDetectionPort {

    private final LanguageDetector languageDetector;

    @Override
    public Optional<String> detect(String text) {
        if (text == null || text.length() < 10) {
            return Optional.empty();
        }
        Language detected = languageDetector.detectLanguageOf(text);
        if (detected == Language.UNKNOWN) {
            return Optional.empty();
        }
        return Optional.of(detected.getIsoCode639_1().toString().toLowerCase());
    }
}
