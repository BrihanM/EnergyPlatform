package com.miapp.auth;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifica que las capas internas no dependan de frameworks de infraestructura.
 */
@AnalyzeClasses(packages = "com.miapp.auth")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnInfrastructure =
            noClasses().that().resideInAnyPackage("com.miapp.auth.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule applicationMustNotDependOnPersistence =
            noClasses().that().resideInAnyPackage("com.miapp.auth.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.miapp.auth.infrastructure..", "org.springframework.data..",
                            "jakarta.persistence..");
}
