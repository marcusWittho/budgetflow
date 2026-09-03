# BudgetFlow

SaaS web para geração e gestão de orçamentos pessoais (quotes). Plataforma de multi-tenant para controle de finanças com taxonomia customizável e rastreamento de entradas orçamentárias.

## Stack

- **Backend:** Java 21 + Spring Boot 4.1.0
- **Banco de dados:** PostgreSQL (migrations via Liquibase)
- **Segurança:** Spring Security + JWT (access token 15m + refresh token 7d em httpOnly cookie)
- **ORM:** Spring Data JPA / Hibernate
- **Validação:** Spring Validation + Jakarta Bean Validation
- **Build:** Gradle (Java 21 toolchain)

## Arquitetura

O projeto segue arquitetura **Feature-First** modular. Cada domínio de negócio é uma feature independente com suas próprias camadas: controller, service, repository, entity e dto.

```
src/main/java/com/lwv/budgetflow/
├── config/                    # Configuração global (JWT, CORS, Security)
├── shared/                    # Cross-cutting concerns
│   └── exception/            # Tratamento centralizado de exceções
├── auth/                      # Feature: Autenticação & Usuários
│   ├── controller/           # AuthController, UserController
│   ├── service/              # AuthService (register, login, refresh, logout)
│   ├── repository/           # UserRepository, RefreshTokenRepository, OrganizationRepository
│   ├── entity/               # UserEntity, RefreshTokenEntity, OrganizationEntity + enums
│   ├── security/             # JWT (JwtService, UserPrincipal, filters, entry point)
│   └── dto/                  # AuthResponse, LoginRequest, RegisterRequest, UserResponse
├── ledger/                    # Feature: Orçamento & Entradas
│   ├── controller/           # EntryController
│   ├── service/              # EntryService, EntryScheduleService
│   ├── repository/           # EntryRepository, EntryGroupRepository
│   ├── entity/               # Entry, EntryGroup
│   ├── web/                  # REST endpoints
│   └── dto/                  # EntryRequest, EntryResponse
├── taxonomy/                  # Feature: Categorias & Taxonomia
│   ├── controller/           # CategoryController
│   ├── service/              # TaxonomyProvisioningService (auto-provisão na criação de user)
│   ├── repository/           # CategoryRepository, SubcategoryRepository
│   ├── entity/               # Category, Subcategory
│   ├── web/                  # REST endpoints
│   └── dto/                  # CategoryResponse
├── accounts/                  # Feature: Contas & Formas de Pagamento
│   ├── controller/           # LookupController
│   ├── repository/           # AccountRepository, PaymentMethodRepository
│   ├── entity/               # Account, PaymentMethod
│   ├── web/                  # REST endpoints
│   └── dto/                  # LookupResponse
└── BudgetflowApplication.java # Application entrypoint
```

## Fluxos Principais

### 1. Registro & Autenticação
```
User.register(email, password, fullName)
  ↓
AuthService.register() → UserEntity criado
  ↓
TaxonomyProvisioningService.provisionar() → Categories/Subcategories/Accounts/PaymentMethods criados
  ↓
JwtService.generateAccessToken() + generateOpaqueRefreshToken()
  ↓
Refresh token salvo (hash SHA-256) em refresh_tokens com TTL 7 dias
  ↓
Response: accessToken (15m) + refreshToken (httpOnly cookie)
```

### 2. Token Refresh
```
Cookie: refresh_token
  ↓
AuthService.refresh(refreshToken)
  ↓
Hash token, valida expiração e revogação
  ↓
Novo par de tokens emitido
```

### 3. Acesso Protegido
```
Header: Authorization: Bearer <accessToken>
  ↓
JwtAuthenticationFilter.doFilterInternal()
  ↓
JwtService.parseAndValidate() → Claims extrai userId e email
  ↓
UserPrincipal.fromToken() → Autenticação sem DB (stateless)
  ↓
SecurityContext configurado → Controllers acessam via @AuthenticationPrincipal
```

### 4. Orçamentos Pessoais
```
User cria orçamento/lançamento via EntryController
  ↓
EntryService aplica regra de negócio (single, parcelado, recorrente)
  ↓
Entidades Entry (individual) e EntryGroup (agrupadas por parcelamento)
  ↓
EntryScheduleService executa manutenção recorrente (agendada)
```

### 5. Taxonomia Customizável
```
User registra
  ↓
TaxonomyProvisioningService carrega templates (CSVs em resources/taxonomy/)
  ↓
22 categorias + 85 subcategorias + 5 contas + 6 formas de pagamento criadas
  ↓
User pode renomear/customizar sua cópia (não afeta outros)
  ↓
CategoryController alimenta dropdowns (GET /api/categories)
```

## Como Rodar Localmente

### Pré-requisitos
- Java 21
- PostgreSQL
- Gradle 9.5+

### Variáveis de Ambiente
```bash
# Banco de dados
export URL_DB=localhost:5432/budgetflow_db
export USERNAME_DB=postgres
export PASSWORD_DB=postgres

# JWT
export JWT_SECRET=sua-chave-super-secreta-com-minimo-32-caracteres

# CORS
export CORS_ALLOWED_ORIGINS=http://localhost:5173

# Spring profile
export SPRING_PROFILES_ACTIVE=dev
```

### Executar
```bash
# Aplicação
./gradlew bootRun

# Testes
./gradlew test

# Teste específico
./gradlew test --tests com.lwv.budgetflow.SomeTest

# Build (JAR)
./gradlew build
```

A aplicação inicia em `http://localhost:8080`.

## Database & Migrations

- **Engine:** PostgreSQL
- **Migrations:** Liquibase (src/main/resources/db/changelog/)
- **Master file:** db.changelog-master.yaml (inclui todas as mudanças de changelog/changes/)
- **Política:** Sem `ddl-auto: create` em produção — migrações são manuais

### Adicionar Nova Migração
1. Criar arquivo YAML em `src/main/resources/db/changelog/changes/` (ex: `0005-add-budget-table.yaml`)
2. Referenciar em `db.changelog-master.yaml`
3. Liquibase aplica automaticamente no próximo `bootRun`

## Segurança

- **CSRF:** Desabilitado (único cookie é SameSite=Lax + escopado a `/api/auth`; endpoints protegidos exigem Bearer token)
- **CORS:** Configurável por environment
- **Passwords:** BCrypt + salt
- **Refresh Tokens:** Opacos (256 bits), hashados com SHA-256, armazenados com revogação
- **Access Tokens:** JWT assinado (HS256), sem dados sensíveis

## Profiles (Ambientes)

- **dev** (padrão) — Sem HTTPS, CORS flexível, logging verbose
- **hml** — Staging, HTTPS exigido
- **prd** — Produção, todas as validações ativas

Configure via `SPRING_PROFILES_ACTIVE`.

## API Endpoints

### Auth
- `POST /api/auth/register` — Registro
- `POST /api/auth/login` — Login
- `POST /api/auth/refresh` — Renovar tokens
- `POST /api/auth/logout` — Logout (revoga refresh token)

### Users
- `GET /api/users/me` — Dados do usuário autenticado (requer JWT)

### Entries (Orçamentos)
- `POST /api/entries` — Criar lançamento
- `GET /api/entries` — Listar (com filtros opcionais)
- `PATCH /api/entries/{id}` — Atualizar
- `DELETE /api/entries/{id}` — Deletar

### Categories (Taxonomia)
- `GET /api/categories` — Listar categorias do usuário
- `POST /api/categories/provision` — Provisionar taxonomia padrão (idempotente)

### Lookups (Contas & Formas de Pagamento)
- `GET /api/lookups` — Listar contas e formas de pagamento do usuário

## Estrutura de Recursos

```
src/main/
├── java/com/lwv/budgetflow/    # Código-fonte
├── resources/
│   ├── application.yaml         # Config base
│   ├── application-dev.yaml     # Dev overrides
│   ├── application-hml.yaml     # Staging overrides
│   ├── application-prd.yaml     # Prod overrides
│   ├── db/changelog/            # Migrações Liquibase
│   └── taxonomy/                # Templates de categorias (CSVs)
└── test/java/com/lwv/budgetflow/  # Testes

src/test/
├── java/com/lwv/budgetflow/    # Testes unitários
└── resources/                   # Fixtures de teste
```

## Convenções de Código

- **Nomes:** CamelCase para classes, snake_case para colunas/tabelas
- **DTOs:** Sufixo `Request` para entrada, `Response` para saída, nomes de features são prefixos (ex: `LoginRequest`, `EntryResponse`)
- **Entities:** Sufixo `Entity`, repositórios usam Spring Data (sem sufixo)
- **Services:** Sufixo `Service`, lógica de negócio centralizada
- **Exceções:** Personalizadas, herdam de `RuntimeException`, tratadas centralmente em `GlobalExceptionHandler`

## Contribuindo

1. Crie uma branch (`git checkout -b feat/feature-name`)
2. Faça commit das mudanças (`git commit -m "feat: descrição"`)
3. Push para a branch (`git push origin feat/feature-name`)
4. Abra um Pull Request

### Commits
Use conventional commits:
- `feat:` nova feature
- `fix:` correção de bug
- `refactor:` reorganização sem mudança de comportamento
- `docs:` documentação
- `test:` testes
