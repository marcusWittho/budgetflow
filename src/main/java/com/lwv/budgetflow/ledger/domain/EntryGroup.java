package com.lwv.budgetflow.ledger.domain;

import jakarta.persistence.*;
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
public class EntryGroup {

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

    /** Preenchido so em parcelamento. Assinatura nao tem total. */
    @Column(name = "total_installments")
    private Integer totalInstallments;

    @Column(length = 200)
    private String label;

    @Column(nullable = false)
    private boolean active = true;

    protected EntryGroup() {
    }

    /**
     * Fabricas estaticas em vez de construtor publico: os dois casos exigem
     * campos diferentes, e assim fica impossivel criar uma assinatura com
     * total de parcelas ou um parcelamento sem total.
     */
    public static EntryGroup installment(UUID userId, String code, int total, String label) {
        EntryGroup g = new EntryGroup();
        g.userId = userId;
        g.code = code;
        g.kind = INSTALLMENT;
        g.totalInstallments = total;
        g.label = label;
        return g;
    }

    public static EntryGroup recurring(UUID userId, String code, String label) {
        EntryGroup g = new EntryGroup();
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

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCode() {
        return code;
    }

    public String getKind() {
        return kind;
    }

    public Integer getTotalInstallments() {
        return totalInstallments;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return active;
    }
}
