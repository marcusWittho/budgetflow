# BudgetFlow

SaaS web para geração de orçamentos (quotes).

## Stack

- **Backend:** Java 21 + Spring Boot (monólito modular — módulos internos: auth, quotes, billing, export)
- **Banco de dados:** PostgreSQL (via Liquibase para migrations)
- **Segurança:** Spring Security

## Rodando localmente

```bash
./gradlew bootRun
```

Testes:

```bash
./gradlew test
```

## Estrutura

```
src/main/java/com/lwv/budgetflow   código-fonte
src/main/resources                 configuração (application.yaml) e migrations (db/changelog)
src/test/java/com/lwv/budgetflow   testes
```
