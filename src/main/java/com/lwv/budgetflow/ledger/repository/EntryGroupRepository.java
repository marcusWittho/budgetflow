package com.lwv.budgetflow.ledger.repository;

import com.lwv.budgetflow.ledger.entity.EntryGroupEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntryGroupRepository extends JpaRepository<EntryGroupEntity, UUID> {

    Optional<EntryGroupEntity> findByIdAndUserId(UUID id, UUID userId);

    List<EntryGroupEntity> findByKindAndActiveTrue(String kind);

    /**
     * Maior numero ja usado num prefixo (P ou R) por este usuario; 0 se
     * nenhum.
     *
     * Query nativa porque SUBSTRING e CAST variam entre versoes do
     * Hibernate, e o projeto ja esta preso ao Postgres de qualquer forma.
     */
    @Query(value = """
           SELECT COALESCE(MAX(CAST(SUBSTRING(code FROM 2) AS INTEGER)), 0)
           FROM entry_groups
           WHERE user_id = :userId AND code LIKE :prefixo
           """, nativeQuery = true)
    int maiorNumeroDoCodigo(@Param("userId") UUID userId, @Param("prefixo") String prefixo);
}
