package com.lwv.budgetflow.taxonomy.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Dono do dado. Nao e um @ManyToOne para a entidade de usuario de
     * proposito: mantem este modulo independente do modulo auth. A foreign
     * key existe no banco de qualquer forma.
     */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 20, updatable = false)
    private String type;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int position = 0;

    @Column(nullable = false)
    private boolean archived = false;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, name ASC")
    private List<Subcategory> subcategories = new ArrayList<>();

    protected Category() {
    }

    public Category(UUID userId, String type, String name) {
        this.userId = userId;
        this.type = type;
        this.name = name;
    }

    public Subcategory addSubcategory(String subcategoryName, String nature) {
        Subcategory sub = new Subcategory(this, subcategoryName, nature);
        sub.setPosition(subcategories.size());
        subcategories.add(sub);
        return sub;
    }

    /**
     * Arquivar em vez de apagar. Se houver lancamento apontando para esta
     * categoria, apagar levaria o historico junto ou seria recusado pelo
     * banco. Arquivar tira dos dropdowns e preserva o passado.
     */
    public void archive() {
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<Subcategory> getSubcategories() {
        return subcategories;
    }
}
