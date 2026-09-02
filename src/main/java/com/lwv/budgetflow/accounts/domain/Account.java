package com.lwv.budgetflow.accounts.domain;

import jakarta.persistence.*;
import java.util.UUID;

/** De onde o dinheiro saiu ou entrou: Conta Corrente, Carteira, Poupanca. */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean archived = false;

    protected Account() {
    }

    public Account(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public void archive() {
        this.archived = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isArchived() {
        return archived;
    }
}
