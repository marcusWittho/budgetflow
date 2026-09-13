package com.lwv.budgetflow.accounts.repository;

import com.lwv.budgetflow.accounts.entity.AccountEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByIdAndUserId(UUID id, UUID userId);

    List<AccountEntity> findByUserIdAndArchivedFalseOrderByNameAsc(UUID userId);
}
