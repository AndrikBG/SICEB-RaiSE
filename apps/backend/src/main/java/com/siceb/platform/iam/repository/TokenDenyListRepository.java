package com.siceb.platform.iam.repository;

import com.siceb.platform.iam.entity.TokenDenyListEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface TokenDenyListRepository extends JpaRepository<TokenDenyListEntry, UUID> {

    boolean existsByJti(String jti);

    @Modifying
    @Query("DELETE FROM TokenDenyListEntry t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
