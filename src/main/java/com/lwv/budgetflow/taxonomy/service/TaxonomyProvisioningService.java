package com.lwv.budgetflow.taxonomy.service;

import com.lwv.budgetflow.accounts.domain.Account;
import com.lwv.budgetflow.accounts.domain.PaymentMethod;
import com.lwv.budgetflow.accounts.repository.AccountRepository;
import com.lwv.budgetflow.accounts.repository.PaymentMethodRepository;
import com.lwv.budgetflow.taxonomy.domain.Category;
import com.lwv.budgetflow.taxonomy.repository.CategoryRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria a taxonomia inicial de um usuario: 22 categorias, 85 subcategorias,
 * 5 contas e 6 formas de pagamento, extraidas da planilha que originou
 * este sistema.
 *
 * Sem isso nao da para lancar nada — categoryId e obrigatorio e as tabelas
 * nascem vazias para cada assinante novo.
 *
 * Por que cada usuario recebe uma COPIA em vez de referenciar um catalogo
 * global: ele vai querer renomear "Cuidados pessoais" para "Beleza". Com
 * catalogo compartilhado, isso renomearia para todo mundo, e voce
 * precisaria de tabelas de apelidos, de itens escondidos e de itens
 * customizados por usuario — tres junções e um UNION para montar um
 * dropdown. Copia resolve com "where user_id = ?".
 *
 * Linha de banco e barata: 10 mil assinantes dao ~1 milhao de linhas nestas
 * duas tabelas, o que e irrelevante para o Postgres.
 */
@Service
public class TaxonomyProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyProvisioningService.class);

    private static final String TAXONOMY_CSV = "taxonomy/default-taxonomy.csv";
    private static final String LISTS_CSV = "taxonomy/default-lists.csv";

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public TaxonomyProvisioningService(CategoryRepository categoryRepository,
                                       AccountRepository accountRepository,
                                       PaymentMethodRepository paymentMethodRepository) {
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public record Resultado(int categorias, int subcategorias,
                            int contas, int formas, boolean jaExistia) {
    }

    /**
     * Idempotente: se o usuario ja tem categorias, nao faz nada. Assim pode
     * ser chamado no cadastro e de novo por um endpoint manual sem
     * duplicar.
     */
    @Transactional
    public Resultado provisionar(UUID userId) {
        if (categoryRepository.existsByUserId(userId)) {
            return new Resultado(0, 0, 0, 0, true);
        }

        int[] tax = provisionarTaxonomia(userId);
        int[] listas = provisionarListas(userId);

        log.info("Taxonomia provisionada para {}: {} categorias, {} subcategorias.",
                userId, tax[0], tax[1]);

        return new Resultado(tax[0], tax[1], listas[0], listas[1], false);
    }

    private int[] provisionarTaxonomia(UUID userId) {
        // LinkedHashMap preserva a ordem do CSV, que vira o campo position.
        // Assim o dropdown sai na ordem que voce desenhou na planilha, e
        // nao em ordem alfabetica.
        Map<String, Category> porChave = new LinkedHashMap<>();
        int subcategorias = 0;
        int posicao = 0;

        for (String[] linha : lerCsv(TAXONOMY_CSV)) {
            String tipo = linha[0];
            String nomeCategoria = linha[1];
            String nomeSubcategoria = linha[2];
            String natureza = (linha.length > 3 && !linha[3].isBlank()) ? linha[3] : null;

            String chave = tipo + "|" + nomeCategoria;
            Category categoria = porChave.get(chave);

            if (categoria == null) {
                categoria = new Category(userId, tipo, nomeCategoria);
                categoria.setPosition(posicao++);
                porChave.put(chave, categoria);
            }

            categoria.addSubcategory(nomeSubcategoria, natureza);
            subcategorias++;
        }

        // O cascade da Category grava as subcategorias junto.
        categoryRepository.saveAll(porChave.values());
        return new int[]{porChave.size(), subcategorias};
    }

    private int[] provisionarListas(UUID userId) {
        List<Account> contas = new ArrayList<>();
        List<PaymentMethod> formas = new ArrayList<>();

        for (String[] linha : lerCsv(LISTS_CSV)) {
            switch (linha[0]) {
                case "account" -> contas.add(new Account(userId, linha[1]));
                case "payment_method" -> formas.add(new PaymentMethod(userId, linha[1]));
                default -> throw new IllegalStateException("Tipo invalido no CSV: " + linha[0]);
            }
        }

        accountRepository.saveAll(contas);
        paymentMethodRepository.saveAll(formas);
        return new int[]{contas.size(), formas.size()};
    }

    /**
     * Leitor de CSV simples de proposito: os arquivos sao nossos, nao tem
     * virgula dentro de campo nem aspas. Trazer uma biblioteca so para isso
     * seria peso desnecessario.
     */
    private List<String[]> lerCsv(String caminho) {
        List<String[]> linhas = new ArrayList<>();
        ClassPathResource recurso = new ClassPathResource(caminho);

        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {

            leitor.readLine(); // cabecalho

            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (!linha.isBlank()) {
                    linhas.add(linha.split(",", -1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Nao consegui ler " + caminho, e);
        }

        return linhas;
    }
}
