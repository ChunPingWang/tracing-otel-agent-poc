package com.ecommerce.payment;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

public class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.ecommerce.payment");

    @Test
    void hexagonal_architecture_layers_should_be_respected() {
        Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy("com.ecommerce.payment.domain..")
                .layer("Application").definedBy("com.ecommerce.payment.application..")
                .layer("Infrastructure").definedBy("com.ecommerce.payment.infrastructure..")
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Application").mayOnlyAccessLayers("Domain")
                .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")
                .check(classes);
    }
}
