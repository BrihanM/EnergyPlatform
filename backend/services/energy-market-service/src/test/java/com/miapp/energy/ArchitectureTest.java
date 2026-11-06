package com.miapp.energy;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Protects the dependency direction of the initial hexagonal boundary.
 */
@AnalyzeClasses(packages = "com.miapp.energy")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnFrameworks =
            noClasses().that().resideInAnyPackage("com.miapp.energy.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule applicationMustNotDependOnInfrastructure =
            noClasses().that().resideInAnyPackage("com.miapp.energy.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.miapp.energy.infrastructure..");
}
