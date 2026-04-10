package com.nikiforov.aichatbot.port.out;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class LanguageDetectionPortContractTest {

    public abstract LanguageDetectionPort createPort();

    @Test
    void detectsEnglishText_returnsEn() {
        LanguageDetectionPort port = createPort();

        Optional<String> lang = port.detect("What is the performance review process?");

        assertThat(lang).isPresent();
        assertThat(lang.get()).isEqualTo("en");
    }

    @Test
    void detectsNonEnglish_returnsDifferentCode() {
        LanguageDetectionPort port = createPort();

        Optional<String> lang = port.detect("Bonjour comment allez-vous aujourd'hui");

        assertThat(lang).isPresent();
        assertThat(lang.get()).isNotEqualTo("en");
    }

    @Test
    void veryShortInput_returnsEmpty() {
        LanguageDetectionPort port = createPort();

        Optional<String> lang = port.detect("hi");

        assertThat(lang).isEmpty();
    }
}
