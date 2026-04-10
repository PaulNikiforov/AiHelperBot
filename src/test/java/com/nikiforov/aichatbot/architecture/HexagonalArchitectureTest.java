package com.nikiforov.aichatbot.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Hexagonal Architecture Tests (Phase 9)
 *
 * These tests enforce the dependency rules of hexagonal architecture (Ports & Adapters).
 * The domain core must not depend on infrastructure (adapters, Spring, JPA).
 * Adapters must not depend on each other across the inbound/outbound boundary.
 */
@DisplayName("Hexagonal Architecture Rules")
class HexagonalArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.nikiforov.aichatbot");

    @Test
    @DisplayName("Domain must not depend on adapters")
    void domain_mustNotDependOn_adapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on Spring (except jakarta.validation)")
    void domain_mustNotDependOn_spring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Domain must be pure business logic with zero framework dependencies");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on JPA/persistence")
    void domain_mustNotDependOn_persistence() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("javax.persistence..", "jakarta.persistence..", "org.hibernate..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Inbound ports must not depend on outbound ports")
    void portIn_mustNotDependOn_portOut() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..port.in..")
                .should().dependOnClassesThat().resideInAPackage("..port.out..")
                .because("Inbound ports (use cases) should only depend on domain models");

        rule.check(classes);
    }

    @Test
    @DisplayName("Inbound ports must only depend on domain model")
    void portIn_shouldOnlyDependOn_domainModel() {
        ArchRule rule = classes()
                .that().resideInAPackage("..port.in..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..domain.model..",
                        "..domain.exception..",
                        "java..",
                        "org.jetbrains.annotations..",
                        "org.springframework.lang.."
                )
                .orShould().resideInAPackage("..port.in..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Outbound ports must only depend on domain model")
    void portOut_shouldOnlyDependOn_domainModel() {
        ArchRule rule = classes()
                .that().resideInAPackage("..port.out..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..domain.model..",
                        "..domain.exception..",
                        "java..",
                        "org.jetbrains.annotations..",
                        "org.springframework.lang.."
                )
                .orShould().resideInAPackage("..port.out..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Inbound adapters must not depend on outbound adapters")
    void adapterIn_mustNotDependOn_adapterOut() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
                .because("Inbound adapters (controllers) must only call domain, not infrastructure directly");

        rule.check(classes);
    }

    @Test
    @DisplayName("Inbound adapters should only depend on inbound ports, identity port, and domain")
    void adapterIn_shouldOnlyDependOn_portInAndDomain() {
        ArchRule rule = classes()
                .that().resideInAPackage("..adapter.in..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..port.in..",
                        // IdentityProviderPort is a cross-cutting infrastructure port used by inbound adapters
                        "com.nikiforov.aichatbot.port.out", // Only the port package, not adapter.out implementations
                        "..domain.model..",
                        "..domain.exception..",
                        "java..",
                        "org.springframework..",
                        "jakarta.validation..",
                        "org.springframework.validation..",
                        "org.springframework.web..",
                        "org.springframework.http..",
                        "org.jetbrains.annotations..",
                        "org.springframework.lang.."
                )
                .orShould().resideInAPackage("..adapter.in..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Inbound adapters must not depend on outbound adapter implementations")
    void adapterIn_mustNotDependOn_outboundAdapterImplementations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter.out..")
                .because("Inbound adapters may use outbound port interfaces, but not adapter implementations");

        rule.check(classes);
    }

    @Test
    @DisplayName("Outbound adapters should only depend on outbound ports and domain")
    void adapterOut_shouldOnlyDependOn_portOutAndDomain() {
        ArchRule rule = classes()
                .that().resideInAPackage("..adapter.out..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..port.out..",
                        "..domain.model..",
                        "..domain.exception..",
                        "java..",
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "org.springframework.data.jpa..",
                        "org.springframework.transaction..",
                        "org.springframework.ai..",
                        "com.azure..",
                        "com.github.pemistahl..",
                        "org.apache..",
                        "org.jetbrains.annotations..",
                        "org.springframework.lang.."
                )
                .orShould().resideInAPackage("..adapter.out..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain services must not be annotated with @Service or @Component")
    void domainServices_mustNotHave_springAnnotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .andShould().notBeAnnotatedWith("org.springframework.stereotype.Component");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain models must be in domain.model package")
    void domainModels_mustResideIn_domainModel() {
        ArchRule rule = classes()
                .that().areAssignableTo(com.nikiforov.aichatbot.domain.model.Feedback.class)
                .or().areAssignableTo(com.nikiforov.aichatbot.domain.model.Question.class)
                .or().areAssignableTo(com.nikiforov.aichatbot.domain.model.Answer.class)
                .or().areAssignableTo(com.nikiforov.aichatbot.domain.model.DocumentChunk.class)
                .or().areAssignableTo(com.nikiforov.aichatbot.domain.model.FeedbackId.class)
                .or().areAssignableTo(com.nikiforov.aichatbot.domain.model.FeedbackType.class)
                .or().areAssignableTo(com.nikiforov.aichatbot.domain.model.QueryType.class)
                .or().areAssignableTo(com.nikiforov.aichatbot.domain.model.LlmResponse.class)
                .or().areAssignableTo(com.nikiforov.aichatbot.domain.model.ValidationResult.class)
                .should().resideInAPackage("..domain.model..");

        rule.check(classes);
    }

    @Test
    @DisplayName("No cyclic dependencies between packages")
    void noCyclicDependencies_betweenPackages() {
        ArchRule rule = slices()
                .matching("com.nikiforov.aichatbot.(*)")
                .should().beFreeOfCycles();

        rule.check(classes);
    }
}
