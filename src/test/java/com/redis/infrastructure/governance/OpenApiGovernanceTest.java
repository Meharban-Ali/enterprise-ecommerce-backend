package com.redis.infrastructure.governance;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class OpenApiGovernanceTest {

    @Test
    void testOpenApiGovernanceChecks() {
        OpenAPI mockOpenAPI = new OpenAPI();
        Paths paths = new Paths();
        PathItem pathItem = new PathItem();
        Operation getOp = new Operation();
        pathItem.setGet(getOp);
        paths.addPathItem("/api/test", pathItem);
        mockOpenAPI.setPaths(paths);

        OpenApiGovernanceInitializer initializer = new OpenApiGovernanceInitializer();
        org.springframework.test.util.ReflectionTestUtils.setField(initializer, "openAPI", mockOpenAPI);

        List<String> warnings = initializer.validateOpenApi();

        assertFalse(warnings.isEmpty());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Missing description")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Missing response schemas")));
    }
}
