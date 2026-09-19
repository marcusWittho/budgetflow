package com.lwv.budgetflow.accounts.repository;

import com.lwv.budgetflow.accounts.entity.PaymentMethodEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, UUID> {

    Optional<PaymentMethodEntity> findByIdAndUserId(UUID id, UUID userId);

    List<PaymentMethodEntity> findByUserIdAndArchivedFalseOrderByNameAsc(UUID userId);
}
