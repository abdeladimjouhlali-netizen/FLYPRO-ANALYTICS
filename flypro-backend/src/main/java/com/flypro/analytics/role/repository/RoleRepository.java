package com.flypro.analytics.role.repository;

import com.flypro.analytics.common.enums.RoleName;
import com.flypro.analytics.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistance des rôles.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
