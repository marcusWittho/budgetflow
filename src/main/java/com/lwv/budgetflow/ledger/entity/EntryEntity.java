package com.lwv.budgetflow.ledger.entity;

import com.lwv.budgetflow.accounts.entity.AccountEntity;
import com.lwv.budgetflow.accounts.entity.PaymentMethodEntity;
import com.lwv.budgetflow.taxonomy.domain.Category;
import com.lwv.budgetflow.taxonomy.domain.Subcategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha do livro-caixa. Tabela central do sistema.
 *
 * Duas decisoes que valem explicacao:
 *
 * 1) amount e BigDecimal, nunca double. O app em Python usava float e
 *    sobrevivia porque o Excel arredondava na exibicao; aqui a soma
 *    acontece no banco e a diferenca de centavo apareceria.
 *
 * 2) guardamos category E subcategory. E redundante, porque a subcategoria
 *    ja conhece sua categoria. Mas subcategoria e opcional e quase toda
 *    agregacao agrupa por categoria — assim ela nao precisa de join extra
 *    nem de tratar o caso nulo.
 */
@Entity
@Table(name = "entries")
@Getter
@NoArgsConstructor
public class EntryEntity {

    public static final String PAID = "paid";
    public static final String PENDING = "pending";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Setter
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethodEntity paymentMethod;

    @Setter
    @Column(nullable = false, length = 300)
    private String description;

    @Setter
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Setter
    @Column(nullable = false, length = 20)
    private String status;

    @Setter
    @Column(columnDefinition = "text")
    private String notes;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private EntryGroupEntity group;

    @Setter
    @Column(name = "installment_number")
    private Integer installmentNumber;

    public EntryEntity(UUID userId, LocalDate entryDate, Category category,
                       String description, BigDecimal amount, String status) {
        this.userId = userId;
        this.entryDate = entryDate;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.status = status;
    }

    public EntryEntity copyForDate(LocalDate newDate, String newStatus) {
        EntryEntity copy = new EntryEntity(userId, newDate, category, description, amount, newStatus);
        copy.subcategory = this.subcategory;
        copy.account = this.account;
        copy.paymentMethod = this.paymentMethod;
        copy.notes = this.notes;
        return copy;
    }

    public void attachToGroup(EntryGroupEntity group, int installmentNumber) {
        this.group = group;
        this.installmentNumber = installmentNumber;
    }

    public String installmentLabel() {
        if (group == null) {
            return "";
        }
        if (group.isRecurring()) {
            return "recorrente";
        }
        return installmentNumber + "/" + group.getTotalInstallments();
    }

    public void markPaid() {
        this.status = PAID;
    }
}
