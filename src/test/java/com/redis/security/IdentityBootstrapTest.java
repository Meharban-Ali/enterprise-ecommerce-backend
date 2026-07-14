package com.redis.security;

import com.redis.infrastructure.config.DataInitializer;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
public class IdentityBootstrapTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.redis.product.repository.ProductRepository productRepository;

    @Autowired
    private com.redis.monitoring.repository.AlertRuleRepository alertRuleRepository;

    @Autowired
    private com.redis.infrastructure.config.DataInitializerProperties properties;

    private TestDataInitializer dataInitializer;
    private final Map<String, String> mockEnv = new HashMap<>();

    @BeforeEach
    void setUp() {
        // Clean settings and bootstrap super admin users before each test
        jdbcTemplate.update("DELETE FROM system_settings WHERE setting_key = 'bootstrap.completed'");
        userRepository.findByRole(Role.ROLE_SUPER_ADMIN).forEach(user -> userRepository.delete(user));
        
        mockEnv.clear();
        dataInitializer = new TestDataInitializer(
                productRepository,
                userRepository,
                alertRuleRepository,
                passwordEncoder,
                properties,
                jdbcTemplate,
                mockEnv
        );
    }

    @Test
    void testBootstrapSkippedWhenEnvNotSet() {
        assertFalse(userRepository.existsByRole(Role.ROLE_SUPER_ADMIN));
        dataInitializer.initializeSuperAdmin();
        assertFalse(userRepository.existsByRole(Role.ROLE_SUPER_ADMIN));
    }

    @Test
    void testSuccessfulBootstrap() {
        mockEnv.put("SUPER_ADMIN_NAME", "superadmin");
        mockEnv.put("SUPER_ADMIN_EMAIL", "superadmin@ecommerce.local");
        mockEnv.put("SUPER_ADMIN_PASSWORD", "StrongPassword@12345");
        mockEnv.put("SUPER_ADMIN_PHONE", "+12345678901");

        assertFalse(userRepository.existsByRole(Role.ROLE_SUPER_ADMIN));
        dataInitializer.initializeSuperAdmin();

        assertTrue(userRepository.existsByRole(Role.ROLE_SUPER_ADMIN));
        User admin = userRepository.findByEmail("superadmin@ecommerce.local").orElse(null);
        assertNotNull(admin);
        assertEquals("superadmin", admin.getActualUsername());
        assertTrue(passwordEncoder.matches("StrongPassword@12345", admin.getPassword()));
        assertTrue(admin.isPasswordChangeRequired());
        assertEquals("+12345678901", admin.getPhone());

        // Check lock set in DB
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_settings WHERE setting_key = 'bootstrap.completed' AND setting_value = 'true'",
                Integer.class
        );
        assertEquals(1, count);
    }

    @Test
    void testInvalidEmailFailure() {
        mockEnv.put("SUPER_ADMIN_NAME", "superadmin");
        mockEnv.put("SUPER_ADMIN_EMAIL", "invalid-email-format");
        mockEnv.put("SUPER_ADMIN_PASSWORD", "StrongPassword@12345");
        mockEnv.put("SUPER_ADMIN_PHONE", "+12345678901");

        assertThrows(IllegalArgumentException.class, () -> {
            dataInitializer.initializeSuperAdmin();
        });
    }

    @Test
    void testWeakPasswordFailure() {
        mockEnv.put("SUPER_ADMIN_NAME", "superadmin");
        mockEnv.put("SUPER_ADMIN_EMAIL", "superadmin@ecommerce.local");
        mockEnv.put("SUPER_ADMIN_PASSWORD", "weakpass");
        mockEnv.put("SUPER_ADMIN_PHONE", "+12345678901");

        assertThrows(IllegalArgumentException.class, () -> {
            dataInitializer.initializeSuperAdmin();
        });
    }

    @Test
    void testInvalidPhoneFailure() {
        mockEnv.put("SUPER_ADMIN_NAME", "superadmin");
        mockEnv.put("SUPER_ADMIN_EMAIL", "superadmin@ecommerce.local");
        mockEnv.put("SUPER_ADMIN_PASSWORD", "StrongPassword@12345");
        mockEnv.put("SUPER_ADMIN_PHONE", "invalid-phone");

        assertThrows(IllegalArgumentException.class, () -> {
            dataInitializer.initializeSuperAdmin();
        });
    }

    // Subclass of DataInitializer to override getEnv
    private static class TestDataInitializer extends DataInitializer {
        private final Map<String, String> env;

        public TestDataInitializer(
                com.redis.product.repository.ProductRepository productRepository,
                UserRepository userRepository,
                com.redis.monitoring.repository.AlertRuleRepository alertRuleRepository,
                PasswordEncoder passwordEncoder,
                com.redis.infrastructure.config.DataInitializerProperties properties,
                JdbcTemplate jdbcTemplate,
                Map<String, String> env) {
            super(productRepository, userRepository, alertRuleRepository, passwordEncoder, properties, jdbcTemplate);
            this.env = env;
        }

        @Override
        protected String getEnv(String name) {
            return env.get(name);
        }
    }
}
