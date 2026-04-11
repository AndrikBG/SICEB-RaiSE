package com.siceb.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * CRN-27 enforcement: module dependency graph must be a directed acyclic graph.
 * Validates layering rules and prevents circular dependencies.
 */
class DependencyArchTest {

    private static final JavaClasses allClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.siceb");

    @Test
    void domainModulesFreeOfCycles() {
        SlicesRuleDefinition.slices()
                .matching("com.siceb.domain.(*)..")
                .should().beFreeOfCycles()
                .check(allClasses);
    }

    @Test
    void allTopLevelSlicesFreeOfCycles() {
        // G4 tech debt: platform.consent→domain.clinicalcare cycle via LfpdpppComplianceTracker.
        // Tracked in work/tech-debt.md — needs port/interface extraction to break the cycle.
        SlicesRuleDefinition.slices()
                .matching("com.siceb.(*)..")
                .should().beFreeOfCycles()
                .ignoreDependency(
                        JavaClass.Predicates.resideInAPackage("com.siceb.platform.consent.."),
                        JavaClass.Predicates.resideInAPackage("com.siceb.domain.."))
                .check(allClasses);
    }

    @Test
    void sharedKernelDoesNotDependOnDomain() {
        noClasses()
                .that().resideInAPackage("com.siceb.shared..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.siceb.domain..")
                .because("Shared Kernel is a leaf dependency — it must not reference domain modules")
                .check(allClasses);
    }

    @Test
    void sharedKernelDoesNotDependOnPlatform() {
        noClasses()
                .that().resideInAPackage("com.siceb.shared..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.siceb.platform..")
                .because("Shared Kernel is a leaf dependency — it must not reference platform modules")
                .check(allClasses);
    }

    @Test
    void sharedKernelDoesNotDependOnConfig() {
        noClasses()
                .that().resideInAPackage("com.siceb.shared..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.siceb.config..")
                .because("Shared Kernel must not depend on Spring configuration")
                .check(allClasses);
    }

    @Test
    void domainDoesNotDependOnApiLayer() {
        noClasses()
                .that().resideInAPackage("com.siceb.domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.siceb.api..")
                .because("Domain modules must not depend on REST controllers")
                .check(allClasses);
    }

    @Test
    void domainDoesNotDependOnConfig() {
        noClasses()
                .that().resideInAPackage("com.siceb.domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.siceb.config..")
                .because("Domain modules must not depend on Spring configuration")
                .check(allClasses);
    }

    @Test
    void platformDoesNotDependOnDomain() {
        // G4 tech debt: LfpdpppComplianceTracker depends on ClinicalEventStore/ClinicalEvent.
        // Tracked in work/tech-debt.md — needs port/interface extraction.
        noClasses()
                .that().resideInAPackage("com.siceb.platform..")
                .and().doNotHaveFullyQualifiedName("com.siceb.platform.consent.service.LfpdpppComplianceTracker")
                .should().dependOnClassesThat()
                .resideInAPackage("com.siceb.domain..")
                .because("Platform provides cross-cutting infrastructure — it must not reference domain modules")
                .check(allClasses);
    }

    @Test
    void platformDoesNotDependOnApi() {
        noClasses()
                .that().resideInAPackage("com.siceb.platform..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.siceb.api..")
                .because("Platform must not depend on REST controllers")
                .check(allClasses);
    }
}
