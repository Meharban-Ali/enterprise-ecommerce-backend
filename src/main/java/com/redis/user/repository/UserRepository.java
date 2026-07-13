package com.redis.user.repository;

import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Fetch user by email for authentication purposes
    Optional<User> findByEmail(String email);

    // Fast check for registration duplicate validation
    boolean existsByEmail(String email);

    // Checks if any user exists with the specified role
    boolean existsByRole(Role role);

    /**
     * Search and filter users by role, search keyword (username/email), enabled flag, and nonLocked flag.
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:enabled IS NULL OR u.accountEnabled = :enabled) AND " +
           "(:nonLocked IS NULL OR u.accountNonLocked = :nonLocked)")
    Page<User> searchAndFilterUsers(
            @Param("role") Role role,
            @Param("search") String search,
            @Param("enabled") Boolean enabled,
            @Param("nonLocked") Boolean nonLocked,
            Pageable pageable);

    java.util.List<User> findByRole(Role role);
}
