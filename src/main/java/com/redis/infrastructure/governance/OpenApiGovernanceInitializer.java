package com.redis.infrastructure.governance;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class OpenApiGovernanceInitializer implements CommandLineRunner {

    @Autowired(required = false)
    private OpenAPI openAPI;

    @Override
    public void run(String... args) throws Exception {
        validateOpenApi();
    }

    public List<String> validateOpenApi() {
        List<String> warnings = new ArrayList<>();
        if (openAPI == null) {
            log.warn("OpenAPI bean not found; skipping OpenAPI Governance verification.");
            return warnings;
        }

        log.info("Starting OpenAPI Governance validation...");

        if (openAPI.getPaths() != null) {
            Set<String> normalizedPaths = new HashSet<>();

            openAPI.getPaths().forEach((path, pathItem) -> {
                String normalized = path.replaceAll("\\{[^}]+\\}", "{}");
                if (normalizedPaths.contains(normalized)) {
                    String warning = "Duplicate or overlapping path pattern detected: " + path + " (matches: " + normalized + ")";
                    log.warn("[OpenAPI Governance] " + warning);
                    warnings.add(warning);
                } else {
                    normalizedPaths.add(normalized);
                }

                validateOperation(path, "GET", pathItem.getGet(), warnings);
                validateOperation(path, "POST", pathItem.getPost(), warnings);
                validateOperation(path, "PUT", pathItem.getPut(), warnings);
                validateOperation(path, "DELETE", pathItem.getDelete(), warnings);
                validateOperation(path, "PATCH", pathItem.getPatch(), warnings);
            });
        }

        log.info("OpenAPI Governance validation completed with {} warnings.", warnings.size());
        return warnings;
    }

    private void validateOperation(String path, String method, Operation operation, List<String> warnings) {
        if (operation == null) return;

        if (operation.getDescription() == null || operation.getDescription().trim().isEmpty()) {
            String warning = "Missing description for operation: " + method + " " + path;
            log.warn("[OpenAPI Governance] " + warning);
            warnings.add(warning);
        }

        if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
            String warning = "Missing response schemas for operation: " + method + " " + path;
            log.warn("[OpenAPI Governance] " + warning);
            warnings.add(warning);
        } else {
            operation.getResponses().forEach((status, response) -> {
                if (response.getContent() == null || response.getContent().isEmpty()) {
                    String warning = "Missing content media schema for response status " + status + " in operation: " + method + " " + path;
                    log.warn("[OpenAPI Governance] " + warning);
                    warnings.add(warning);
                }
            });
        }

        if (operation.getSecurity() == null || operation.getSecurity().isEmpty()) {
            if (openAPI.getSecurity() == null || openAPI.getSecurity().isEmpty()) {
                String warning = "Missing security annotations for operation: " + method + " " + path;
                log.warn("[OpenAPI Governance] " + warning);
                warnings.add(warning);
            }
        }
    }
}
