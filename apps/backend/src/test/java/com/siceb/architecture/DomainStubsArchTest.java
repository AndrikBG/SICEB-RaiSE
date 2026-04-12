package com.siceb.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.*;

/**
 * IC-01 enforcement: domain modules that have not been activated in their iteration
 * must remain empty stubs (only {@code package-info.java}).
 * Activated modules are listed in {@link #ACTIVATED_MODULES}.
 */
class DomainStubsArchTest {

    /**
     * Modules activated so far — Phase 2 activates clinicalcare, prescriptions, laboratory.
     * Add modules here as their iteration begins.
     */
    private static final Set<String> ACTIVATED_MODULES = Set.of(
            "com.siceb.domain.clinicalcare",
            "com.siceb.domain.prescriptions",
            "com.siceb.domain.laboratory"
    );

    private static final JavaClasses domainClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.siceb.domain");

    private static boolean isInActivatedModule(JavaClass javaClass) {
        for (String activated : ACTIVATED_MODULES) {
            if (javaClass.getPackageName().startsWith(activated)) return true;
        }
        return false;
    }

    @Test
    void noBusinessLogicInStubModules() {
        for (JavaClass javaClass : domainClasses) {
            if (javaClass.getSimpleName().equals("package-info")) continue;
            if (isInActivatedModule(javaClass)) continue;
            fail("IC-01 violation: '" + javaClass.getName() +
                    "' found in stub domain module. Only package-info.java is allowed until the module's iteration.");
        }
    }

    @Test
    void noJpaEntitiesInStubModules() {
        for (JavaClass javaClass : domainClasses) {
            if (isInActivatedModule(javaClass)) continue;
            assertFalse(javaClass.isAnnotatedWith("jakarta.persistence.Entity"),
                    "IC-01: JPA entity '" + javaClass.getName() + "' found in stub module");
        }
    }

    @Test
    void noSpringComponentsInStubModules() {
        for (JavaClass javaClass : domainClasses) {
            if (isInActivatedModule(javaClass)) continue;
            Set<String> banned = Set.of(
                    "org.springframework.stereotype.Component",
                    "org.springframework.stereotype.Service",
                    "org.springframework.stereotype.Repository",
                    "org.springframework.web.bind.annotation.RestController"
            );
            for (String annotation : banned) {
                assertFalse(javaClass.isAnnotatedWith(annotation),
                        "IC-01: Spring component '" + javaClass.getName() + "' found in stub module");
            }
        }
    }

    @Test
    void allTenDomainModulesExist() {
        Set<String> packages = new HashSet<>();
        for (JavaClass javaClass : domainClasses) {
            String pkg = javaClass.getPackageName();
            packages.add(pkg);
            for (String activated : ACTIVATED_MODULES) {
                if (pkg.startsWith(activated) && !pkg.equals(activated)) {
                    packages.add(activated);
                }
            }
        }

        List<String> expected = List.of(
                "com.siceb.domain.clinicalcare",
                "com.siceb.domain.prescriptions",
                "com.siceb.domain.pharmacy",
                "com.siceb.domain.laboratory",
                "com.siceb.domain.inventory",
                "com.siceb.domain.supplychain",
                "com.siceb.domain.scheduling",
                "com.siceb.domain.billing",
                "com.siceb.domain.reporting",
                "com.siceb.domain.training"
        );

        for (String pkg : expected) {
            assertTrue(packages.contains(pkg), "Missing domain module stub: " + pkg);
        }
    }
}
