package com.flypro.analytics.user.repository;

import com.flypro.analytics.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistance des utilisateurs.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeId(Long employeeId);
}
