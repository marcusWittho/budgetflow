package com.lwv.budgetflow.ledger.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Amarra as parcelas de uma compra ou as mensalidades de uma assinatura.
 * Na planilha isso era a coluna "grupo", com codigos P001 e R001.
 *
 * O code continua existindo porque e util na tela, mas quem manda e o id.
 * Se o codigo se perder ou repetir, nada quebra.
 */
@Entity
@Table(name = "entry_groups")
@Getter
@NoArgsConstructor
public class EntryGroupEntity {

    public static final String INSTALLMENT = "installment";
    public static final String RECURRING = "recurring";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 16, updatable = false)
    private String code;

    @Column(nullable = false, length = 20, updatable = false)
    private String kind;

    @Column(name = "total_installments")
    private Integer totalInstallments;

    @Setter
    @Column(length = 200)
    private String label;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    public static EntryGroupEntity installment(UUID userId, String code, int total, String label) {
        EntryGroupEntity g = new EntryGroupEntity();
        g.userId = userId;
        g.code = code;
        g.kind = INSTALLMENT;
        g.totalInstallments = total;
        g.label = label;
        return g;
    }

    public static EntryGroupEntity recurring(UUID userId, String code, String label) {
        EntryGroupEntity g = new EntryGroupEntity();
        g.userId = userId;
        g.code = code;
        g.kind = RECURRING;
        g.label = label;
        return g;
    }

    public boolean isRecurring() {
        return RECURRING.equals(kind);
    }

    public void deactivate() {
        this.active = false;
    }
}
