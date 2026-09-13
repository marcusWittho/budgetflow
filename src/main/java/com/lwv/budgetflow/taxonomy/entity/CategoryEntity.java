package com.lwv.budgetflow.taxonomy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 20, updatable = false)
    private String type;

    @Setter
    @Column(nullable = false, length = 120)
    private String name;

    @Setter
    @Column(nullable = false)
    private int position = 0;

    @Setter
    @Column(nullable = false)
    private boolean archived = false;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, name ASC")
    private List<SubcategoryEntity> subcategories = new ArrayList<>();

    public CategoryEntity(UUID userId, String type, String name) {
        this.userId = userId;
        this.type = type;
        this.name = name;
    }

    public SubcategoryEntity addSubcategory(String subcategoryName, String nature) {
        SubcategoryEntity sub = new SubcategoryEntity(this, subcategoryName, nature);
        sub.setPosition(subcategories.size());
        subcategories.add(sub);
        return sub;
    }

    public void archive() {
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
    }
}
