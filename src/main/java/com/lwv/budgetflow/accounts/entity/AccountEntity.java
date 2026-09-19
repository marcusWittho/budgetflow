package com.lwv.budgetflow.accounts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** De onde o dinheiro saiu ou entrou: Conta Corrente, Carteira, Poupanca. */
@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor
public class AccountEntity {

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

    public AccountEntity(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public void archive() {
        this.archived = true;
    }
}
