package com.lwv.budgetflow.taxonomy.controller;

import com.lwv.budgetflow.taxonomy.dto.CategoryResponse;
import com.lwv.budgetflow.auth.security.UserPrincipal;
import com.lwv.budgetflow.taxonomy.repository.CategoryRepository;
import com.lwv.budgetflow.taxonomy.service.TaxonomyProvisioningService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Alimenta os dropdowns da tela de lancamento: tipo -> categoria ->
 * subcategoria, com as subcategorias aninhadas na resposta.
 *
 * O provision esta exposto como endpoint por pragmatismo: o ideal e chamar
 * TaxonomyProvisioningService.provisionar() dentro do fluxo de cadastro,
 * mas isso exige mexer no AuthService. Como o metodo e idempotente, os dois
 * caminhos coexistem sem duplicar — quando o cadastro passar a chamar
 * sozinho, este endpoint vira apenas um reparo manual.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository repository;
    private final TaxonomyProvisioningService provisioning;

    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoryResponse> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return repository.findArvore(principal.getId())
                .stream().map(CategoryResponse::from).toList();
    }

    @PostMapping("/provision")
    public TaxonomyProvisioningService.Resultado provisionar(
            @AuthenticationPrincipal UserPrincipal principal) {
        return provisioning.provisionar(principal.getId());
    }
}
