package com.lwv.budgetflow.taxonomy.repository;

import com.lwv.budgetflow.taxonomy.domain.Subcategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubcategoryRepository extends JpaRepository<Subcategory, UUID> {

    /**
     * Escopo pelo dono da categoria pai: subcategoria nao tem user_id
     * proprio, ela herda o dono atraves da categoria.
     */
    Optional<Subcategory> findByIdAndCategoryUserId(UUID id, UUID userId);

    List<Subcategory> findByCategoryIdAndCategoryUserIdAndArchivedFalseOrderByPositionAscNameAsc(
            UUID categoryId, UUID userId);
}
