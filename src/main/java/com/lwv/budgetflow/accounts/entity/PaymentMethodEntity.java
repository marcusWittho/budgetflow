package com.lwv.budgetflow.accounts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Como foi pago: Pix, Debito, Credito, Boleto. */
@Entity
@Table(name = "payment_methods")
@Getter
@NoArgsConstructor
public class PaymentMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Setter
    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean archived = false;

    public PaymentMethodEntity(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public void archive() {
        this.archived = true;
    }
}
