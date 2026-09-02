package com.lwv.budgetflow.accounts.repository;

import com.lwv.budgetflow.accounts.domain.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    List<Account> findByUserIdAndArchivedFalseOrderByNameAsc(UUID userId);
}
