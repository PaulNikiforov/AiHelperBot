package com.nikiforov.aichatbot.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity for bot feedback records.
 * <p>
 * <strong>IMPORTANT:</strong> This entity uses id-based equality for Hibernate compatibility.
 * The equals() and hashCode() methods use only the id field, which is safe for use in
 * collections and as Map keys even with Hibernate proxies.
 */
@Entity
@Table(name = "bot_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bot_feedback_seq")
    @SequenceGenerator(name = "bot_feedback_seq", sequenceName = "bot_feedback_seq", allocationSize = 50)
    private Long id;

    @Column(name = "question", nullable = false, length = 4000)
    private String question;

    @Column(name = "answer", nullable = false, length = 10000)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_feedback_type", nullable = false, length = 50)
    private BotFeedbackType botFeedbackType;

    @Column(name = "employee_email", length = 255)
    private String employeeEmail;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    /**
     * Equality based on id field only. This is safe for Hibernate proxies and
     * prevents issues when entities are used in collections before and after persistence.
     * <p>
     * Uses getClass() instead of instanceof for proper Hibernate proxy handling.
     * Transient entities (id == null) use object identity.
     *
     * @param o object to compare
     * @return true if ids are equal and both are persisted, or same object reference
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BotFeedback that = (BotFeedback) o;
        // For transient entities, use object identity
        // For persisted entities, use id equality
        return id != null && id.equals(that.id);
    }

    /**
     * Hash code based on class only. This is consistent with equals() and
     * prevents issues when entities are moved between collections.
     * <p>
     * Note: This means all transient instances have different hash codes,
     * which is acceptable since they shouldn't be used in hashed collections
     * before persistence.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * ToString excludes employeeEmail to prevent PII from appearing in logs.
     *
     * @return string representation without employee email
     */
    @Override
    public String toString() {
        return "BotFeedback{" +
                "id=" + id +
                ", question='" + question + '\'' +
                ", answer='" + answer + '\'' +
                ", botFeedbackType=" + botFeedbackType +
                ", createdAt=" + createdAt +
                '}';
    }
}
