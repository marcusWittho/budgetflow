package com.lwv.budgetflow.taxonomy.repository;

import com.lwv.budgetflow.taxonomy.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * TODO metodo leva userId. Isso nao e repeticao boba: e o filtro de
 * tenancy. Um findById solto devolveria a categoria de outro assinante se
 * alguem chutasse o UUID na URL.
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserId(UUID userId);

    /**
     * Arvore completa para os dropdowns, com as subcategorias na mesma ida
     * ao banco. Sem o join fetch, o LAZY dispararia uma consulta por
     * categoria — 22 consultas em vez de uma.
     */
    @Query("""
           select distinct c from Category c
           left join fetch c.subcategories s
           where c.userId = :userId and c.archived = false
           order by c.type asc, c.position asc, c.name asc
           """)
    List<Category> findArvore(UUID userId);

    List<Category> findByUserIdAndTypeAndArchivedFalseOrderByPositionAscNameAsc(
            UUID userId, String type);
}
