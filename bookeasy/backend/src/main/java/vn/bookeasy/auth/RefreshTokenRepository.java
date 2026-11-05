package vn.bookeasy.auth;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
