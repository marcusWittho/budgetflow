package com.lwv.budgetflow.ledger.repository;

import com.lwv.budgetflow.ledger.domain.Entry;
import com.lwv.budgetflow.ledger.domain.EntryGroup;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    /**
     * A consulta da tela principal.
     *
     * O join fetch traz os relacionamentos na MESMA ida ao banco. Sem ele,
     * o LAZY dispararia uma consulta por linha ao montar o JSON — o
     * problema N+1. Com 200 lancamentos e cinco relacionamentos, seriam
     * mais de mil consultas.
     *
     * "left join fetch" nos opcionais e obrigatorio: um join interno
     * descartaria os lancamentos sem subcategoria, conta ou forma.
     */
    @Query("""
           select e from Entry e
           join fetch e.category
           left join fetch e.subcategory
           left join fetch e.account
           left join fetch e.paymentMethod
           left join fetch e.group
           where e.userId = :userId and e.entryDate between :de and :ate
           order by e.entryDate desc, e.id desc
           """)
    List<Entry> findPeriodo(UUID userId, LocalDate de, LocalDate ate);

    Optional<Entry> findByIdAndUserId(UUID id, UUID userId);

    Optional<Entry> findTopByGroupOrderByEntryDateDesc(EntryGroup group);

    List<Entry> findByGroupAndStatusAndEntryDateAfter(
            EntryGroup group, String status, LocalDate depoisDe);
}
