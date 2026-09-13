package com.lwv.budgetflow.ledger.domain;

import com.lwv.budgetflow.accounts.entity.AccountEntity;
import com.lwv.budgetflow.accounts.entity.PaymentMethodEntity;
import com.lwv.budgetflow.taxonomy.domain.Category;
import com.lwv.budgetflow.taxonomy.domain.Subcategory;
import jakarta.persistence.*;
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
public class Entry {

    public static final String PAID = "paid";
    public static final String PENDING = "pending";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethodEntity paymentMethod;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private EntryGroup group;

    /** O 9 em "9 de 21". Em assinatura, o contador da mensalidade. */
    @Column(name = "installment_number")
    private Integer installmentNumber;

    protected Entry() {
    }

    public Entry(UUID userId, LocalDate entryDate, Category category,
                 String description, BigDecimal amount, String status) {
        this.userId = userId;
        this.entryDate = entryDate;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.status = status;
    }

    /**
     * Copia rasa, usada para gerar as parcelas seguintes a partir da
     * primeira. Herda conta e forma: uma compra parcelada no cartao
     * continua no cartao nos meses seguintes.
     */
    public Entry copyForDate(LocalDate newDate, String newStatus) {
        Entry copy = new Entry(userId, newDate, category, description, amount, newStatus);
        copy.subcategory = this.subcategory;
        copy.account = this.account;
        copy.paymentMethod = this.paymentMethod;
        copy.notes = this.notes;
        return copy;
    }

    public void attachToGroup(EntryGroup group, int installmentNumber) {
        this.group = group;
        this.installmentNumber = installmentNumber;
    }

    /** "9/21" em parcelamento, "recorrente" em assinatura, vazio no resto. */
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

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Subcategory getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(Subcategory subcategory) {
        this.subcategory = subcategory;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public void setAccount(AccountEntity account) {
        this.account = account;
    }

    public PaymentMethodEntity getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethodEntity paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public EntryGroup getGroup() {
        return group;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }
}
