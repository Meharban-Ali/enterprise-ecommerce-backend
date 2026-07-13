package com.redis.reliability;

import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class TempDatabaseUpdaterTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    @Rollback(false)
    void updateRoles() {
        userRepository.findByEmail("admin@example.com").ifPresent(user -> {
            user.setRole(Role.ROLE_ADMIN);
            userRepository.save(user);
            System.out.println("USER UPDATED TO ADMIN SUCCESSFULLY!");
        });
    }
}
