package com.lwv.budgetflow.auth.repository;

import com.lwv.budgetflow.auth.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

  RefreshTokenEntity findByTokenHashAndRevokedAtIsNull(String hash);
}
