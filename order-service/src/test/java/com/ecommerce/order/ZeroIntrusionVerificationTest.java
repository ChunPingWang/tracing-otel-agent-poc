package com.ecommerce.order;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * US5 Benchmark - Zero Intrusion Verification Test.
 *
 * Scans all Java source files under all 5 services' src/main/java
 * and asserts NO imports of io.opentelemetry, io.opentracing, or
 * similar tracing packages exist.
 *
 * This proves the OTel Java Agent approach is truly zero-intrusion:
 * business code contains no tracing dependencies whatsoever.
 */
public class ZeroIntrusionVerificationTest {

    @Test
    void no_otel_imports_in_any_service() throws IOException {
        Path projectRoot = Paths.get("").toAbsolutePath().getParent(); // parent of order-service
        String[] services = {"order-service", "product-service", "inventory-service",
                             "payment-service", "notification-service"};

        List<String> violations = new ArrayList<String>();

        for (String service : services) {
            Path srcMain = projectRoot.resolve(service).resolve("src/main/java");
            if (!Files.exists(srcMain)) {
                continue;
            }

            final List<Path> javaFiles = new ArrayList<Path>();
            Files.walkFileTree(srcMain, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        javaFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            for (Path javaFile : javaFiles) {
                String content = new String(Files.readAllBytes(javaFile), "UTF-8");
                if (content.contains("import io.opentelemetry")) {
                    violations.add("Found OTel import in: " + javaFile);
                }
                if (content.contains("import io.opentracing")) {
                    violations.add("Found OpenTracing import in: " + javaFile);
                }
                if (content.contains("import io.micrometer.tracing")) {
                    violations.add("Found Micrometer Tracing import in: " + javaFile);
                }
                if (content.contains("import brave.")) {
                    violations.add("Found Brave Tracing import in: " + javaFile);
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Zero-intrusion violation! Tracing imports found in business code:\n"
                + join(violations, "\n"));
    }

    private static String join(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
