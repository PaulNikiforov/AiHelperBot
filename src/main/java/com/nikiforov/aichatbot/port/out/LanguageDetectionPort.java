package com.nikiforov.aichatbot.port.out;

import java.util.Optional;

public interface LanguageDetectionPort {

    Optional<String> detect(String text);
}
