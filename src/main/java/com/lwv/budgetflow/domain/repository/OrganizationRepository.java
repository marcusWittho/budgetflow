package com.lwv.budgetflow.domain.repository;

import com.lwv.budgetflow.domain.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {
}
