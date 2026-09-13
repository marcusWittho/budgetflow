package com.lwv.budgetflow.taxonomy.repository;

import com.lwv.budgetflow.taxonomy.entity.SubcategoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubcategoryRepository extends JpaRepository<SubcategoryEntity, UUID> {

    /**
     * Escopo pelo dono da categoria pai: subcategoria nao tem user_id
     * proprio, ela herda o dono atraves da categoria.
     */
    Optional<SubcategoryEntity> findByIdAndCategoryUserId(UUID id, UUID userId);

    List<SubcategoryEntity> findByCategoryIdAndCategoryUserIdAndArchivedFalseOrderByPositionAscNameAsc(
            UUID categoryId, UUID userId);
}
