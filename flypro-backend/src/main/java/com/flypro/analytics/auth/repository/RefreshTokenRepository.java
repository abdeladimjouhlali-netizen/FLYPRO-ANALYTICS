package com.flypro.analytics.auth.repository;

import com.flypro.analytics.auth.entity.RefreshToken;
import com.flypro.analytics.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistance des refresh tokens.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken rt set rt.revoked = true where rt.user = :user and rt.revoked = false")
    int revokeAllByUser(@Param("user") User user);
}
