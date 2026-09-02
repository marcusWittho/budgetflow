package com.lwv.budgetflow.taxonomy.web;

import com.lwv.budgetflow.taxonomy.domain.Category;
import com.lwv.budgetflow.taxonomy.domain.Subcategory;
import java.util.List;
import java.util.UUID;

/** Categoria com as subcategorias aninhadas, pronta para o dropdown. */
public record CategoryResponse(
        UUID id,
        String type,
        String name,
        List<SubcategoryResponse> subcategories
) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getType(),
                c.getName(),
                c.getSubcategories().stream()
                        .filter(s -> !s.isArchived())
                        .map(SubcategoryResponse::from)
                        .toList());
    }

    public record SubcategoryResponse(UUID id, String name, String nature) {

        public static SubcategoryResponse from(Subcategory s) {
            return new SubcategoryResponse(s.getId(), s.getName(), s.getNature());
        }
    }
}
