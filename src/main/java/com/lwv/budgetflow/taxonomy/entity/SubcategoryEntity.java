package com.lwv.budgetflow.taxonomy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "subcategories")
@Getter
@NoArgsConstructor
public class SubcategoryEntity {

    public static final String FIXED = "fixed";
    public static final String VARIABLE = "variable";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, updatable = false)
    private CategoryEntity category;

    @Setter
    @Column(nullable = false, length = 120)
    private String name;

    @Setter
    @Column(length = 20)
    private String nature;

    @Setter
    @Column(nullable = false)
    private int position = 0;

    @Setter
    @Column(nullable = false)
    private boolean archived = false;

    SubcategoryEntity(CategoryEntity category, String name, String nature) {
        this.category = category;
        this.name = name;
        this.nature = nature;
    }

    public void archive() {
        this.archived = true;
    }
}
