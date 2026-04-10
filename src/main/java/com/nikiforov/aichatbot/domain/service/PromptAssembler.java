package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.model.DocumentChunk;

import java.util.List;

public class PromptAssembler {

    private static final String SYSTEM_PROMPT = """
        Answer the user's question using ONLY information from the provided text.
        Instructions:
        - Extract information that directly answers the question.
        - Provide ONLY relevant information. Do NOT include extra context or notes.
        - If the question is about errors, issues, problems, bugs, or technical help - look ONLY for Support section information.
        - If the question is about inbox, notifications, or requests - look for Inbox module information.
        - If the question is about how to schedule, launch, start, create, or request a review or sync-up:
          * Look for MANUAL steps (Request Review button, Employee profile, overlay, select type, set date, SAVE)
          * Provide step-by-step instructions with specific actions
          * For People Partner: same process as Manager (Employee List -> Employee profile -> Request Review)
          * Ignore any automatic generation information in the context unless explicitly asked
        - If the question is about peer selection - focus on Manager's actions for selecting peers from dropdown.
        - If the question is about reminding peers to submit their review - look for Review form submission tab and REMIND button information.
        - If the question is about growth plan, tasks, progress, statuses:
          * Look ONLY for Growth Plan section information.
          * Provide step-by-step instructions with specific actions (click Add, select status, SAVE, etc.)
          * Differentiate by roles: Employee vs Manager/People Partner
          * If role not specified, assume Employee role and provide instructions accordingly.
          * Include how to create tasks (from review or manually), update progress, change status, edit, cancel.
          * Ignore review process details unless directly related to moving competencies to tasks.
        - Keep answers concise with actionable steps.
        - Match key terms from the question with context.
        - DO NOT add information not in the text.
        - DO NOT include "Also note that..." sections.
        - If unrelated to User Guide - write: "Not specified in the User Guide".
        """;

    public ChatMessages build(String userQuestion, List<DocumentChunk> contextDocs) {
        StringBuilder context = new StringBuilder();
        for (DocumentChunk doc : contextDocs) {
            context.append("Page ").append(doc.pageNumber()).append(":\n")
                    .append(doc.content()).append("\n\n");
        }

        String userContent = "User Question:\n" + userQuestion + "\n\n"
                + "Context (User Guide excerpts):\n" + context;

        return new ChatMessages(SYSTEM_PROMPT, userContent);
    }

    public record ChatMessages(String systemMessage, String userMessage) {}
}
