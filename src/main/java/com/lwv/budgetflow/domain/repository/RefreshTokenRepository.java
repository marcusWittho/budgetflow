package com.lwv.budgetflow.domain.repository;

import com.lwv.budgetflow.domain.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

  RefreshTokenEntity findByTokenHashAndRevokedAtIsNull(String hash);
}
