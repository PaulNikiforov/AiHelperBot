package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.model.QueryType;

import java.util.List;
import java.util.regex.Pattern;

public class QueryClassifier {

    private static final Pattern DEFINITION_PATTERN = Pattern.compile(
            "(?i)\\b(what is|what does|who is|define|what mean)\\b");
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?i)\\b(when|date|deadline|schedule|time|first)\\b");
    private static final Pattern SUPPORT_PATTERN = Pattern.compile(
            "(?i)\\b(error|issue|problem|help|support|technical|bug|not working|doesn't work|contact)\\b");
    private static final Pattern INBOX_PATTERN = Pattern.compile(
            "(?i)\\b(inbox|notification|notifications|request|requests|find.*request|where.*request|see.*request|check.*request|view.*request|sent|approve|deny|open issue|suggest peer|remind|get to inbox|access inbox|search.*inbox|comment.*inbox|set up.*notification|filter.*notification|mobile.*inbox|history.*request|cancel.*request|edit.*request|overdue.*request|anonymous.*notification)\\b");
    private static final Pattern PEERS_PATTERN = Pattern.compile(
            "(?i)\\b(assign.*peer|select.*peer|choose.*peer|find.*peer|peer.*assessment|peer.*selection|suggest.*peer|peers for assessment|select peers|where.*find.*peer)\\b");
    private static final Pattern REVIEW_PROCESS_PATTERN = Pattern.compile(
            "(?i)\\b(how.*schedule|how.*launch|how.*start|how.*create|how.*request|how.*initiate|schedule.*review|schedule.*sync.?up|launch.*review|launch.*sync.?up|start.*review|start.*sync.?up|create.*review|create.*sync.?up|request.*review|request.*sync.?up|initiate.*review|initiate.*sync.?up)\\b");
    private static final Pattern REMIND_PEERS_PATTERN = Pattern.compile(
            "(?i)\\b(remind.*peer|remind.*peers|remind.*submission|remind.*button)\\b");
    private static final Pattern GROWTH_PLAN_PATTERN = Pattern.compile(
            "(?i)\\b(growth plan|growthplan|tasks?\\b|progress|status|move competence|add task|update task|change status|edit task|cancel task|how to go through growth plan)\\b");
    private static final Pattern ARTICLE_PATTERN = Pattern.compile("^(a |the )", Pattern.CASE_INSENSITIVE);
    private static final int MAX_TERM_WORDS = 3;

    private static final List<String> DEFINITION_PHRASES = List.of(
            "what is", "what does", "who is", "define", "what mean");

    private static final List<String> TIME_PHRASES = List.of(
            "when is", "what is the date", "what is the deadline", "what is the schedule",
            "what time", "how long", "how much time");

    public QueryType classify(String query) {
        if (SUPPORT_PATTERN.matcher(query).find()) return QueryType.SUPPORT;
        if (REVIEW_PROCESS_PATTERN.matcher(query).find()) return QueryType.REVIEW_PROCESS;
        if (PEERS_PATTERN.matcher(query).find()) return QueryType.PEERS;
        if (REMIND_PEERS_PATTERN.matcher(query).find()) return QueryType.REMIND_PEERS;
        if (INBOX_PATTERN.matcher(query).find()) return QueryType.INBOX;
        if (GROWTH_PLAN_PATTERN.matcher(query).find()) return QueryType.GROWTH_PLAN;
        if (TIME_PATTERN.matcher(query).find()) return QueryType.TIME_RELATED;
        if (DEFINITION_PATTERN.matcher(query).find()) {
            String term = extractTerm(query, false);
            return isValidDefinitionTerm(term) ? QueryType.DEFINITION : QueryType.GENERAL;
        }
        return QueryType.GENERAL;
    }

    public String extractTerm(String query, boolean isTimeQuery) {
        String lowerQuery = query.toLowerCase();
        int startIndex = findTermStartIndex(lowerQuery, isTimeQuery);
        if (startIndex == -1) return query;
        return cleanTerm(query.substring(startIndex));
    }

    private int findTermStartIndex(String lowerQuery, boolean isTimeQuery) {
        List<String> phrases = isTimeQuery ? TIME_PHRASES : DEFINITION_PHRASES;
        return phrases.stream()
                .filter(lowerQuery::contains)
                .findFirst()
                .map(phrase -> lowerQuery.indexOf(phrase) + phrase.length() + 1)
                .orElse(-1);
    }

    private String cleanTerm(String rawTerm) {
        return ARTICLE_PATTERN.matcher(rawTerm.trim().replace("?", ""))
                .replaceFirst("")
                .trim();
    }

    private boolean isValidDefinitionTerm(String term) {
        return term.split("\\s+").length <= MAX_TERM_WORDS;
    }
}
