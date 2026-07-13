package com.redis.auth.service;

import com.redis.user.entity.User;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 *
 * Intentionally NOT cached with @Cacheable on loadUserByUsername.
 * Caching user details would prevent account lockout/disabling from taking immediate effect
 * because the cached (enabled) UserDetails would still be served after the account is disabled.
 * Security state changes must always reflect the live database state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user details from database for email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User lookup failed — email not found in database");
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
    }
}
