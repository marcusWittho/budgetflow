package com.lwv.budgetflow.taxonomy.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "subcategories")
public class Subcategory {

    public static final String FIXED = "fixed";
    public static final String VARIABLE = "variable";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** LAZY sempre: o padrao do JPA e EAGER, e ele causa o problema N+1. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, updatable = false)
    private Category category;

    @Column(nullable = false, length = 120)
    private String name;

    /** Fixa ou variavel. Nula em receita e investimento. */
    @Column(length = 20)
    private String nature;

    @Column(nullable = false)
    private int position = 0;

    @Column(nullable = false)
    private boolean archived = false;

    protected Subcategory() {
    }

    Subcategory(Category category, String name, String nature) {
        this.category = category;
        this.name = name;
        this.nature = nature;
    }

    public void archive() {
        this.archived = true;
    }

    public UUID getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isArchived() {
        return archived;
    }
}
