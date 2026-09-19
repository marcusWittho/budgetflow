# BudgetFlow Backend - Guia para Claude

Orientações específicas para trabalhar com o backend Spring Boot do BudgetFlow.

## Stack Técnico

- **Framework:** Spring Boot 4.1.0
- **Linguagem:** Java 21
- **Build:** Gradle 9.5+
- **Database:** PostgreSQL + Liquibase
- **Segurança:** Spring Security + JWT (JJWT 0.12.7)
- **ORM:** Spring Data JPA + Hibernate
- **Validação:** Jakarta Bean Validation + Spring Validation

## Arquitetura

O backend segue **Feature-First Modular Architecture** com 6 features principais:

```
src/main/java/com/lwv/budgetflow/
├── config/              # Configuração global (JWT, CORS, Security)
├── shared/              # Cross-cutting concerns e exceções
├── auth/                # Autenticação e usuários
├── ledger/              # Orçamentos e lançamentos
├── taxonomy/            # Categorias e taxonomia
└── accounts/            # Contas e formas de pagamento
```

Cada feature tem sua própria camada:
- **Entity** — Modelo JPA
- **Repository** — Spring Data JPA
- **Service** — Lógica de negócio
- **Controller** — REST endpoints
- **DTO** — Request/Response

## Suite de Testes

### Cobertura Completa

```
56 testes unitários | JUnit 5 + Mockito | 100% sucesso | ~7 segundos
```

#### Testes por Feature

| Feature | Testes | Arquivo |
|---------|--------|---------|
| AuthService | 8 | `auth/service/AuthServiceTest.java` |
| JwtService | 9 | `auth/security/JwtServiceTest.java` |
| EntryService | 16 | `ledger/service/EntryServiceTest.java` |
| EntryScheduleService | 14 | `ledger/service/EntryScheduleServiceTest.java` |
| TaxonomyProvisioningService | 3 | `taxonomy/service/TaxonomyProvisioningServiceTest.java` |
| LookupService | 5 | `accounts/service/LookupServiceTest.java` |

### Executar Testes

```bash
# Todos os testes
./gradlew test

# Testes de uma classe específica
./gradlew test --tests AuthServiceTest

# Um teste específico
./gradlew test --tests AuthServiceTest.testRegisterSuccess

# Com output detalhado
./gradlew test --info

# Forçar reexecução (pulando cache)
./gradlew test --rerun-tasks

# Ver relatório HTML
open build/reports/tests/test/index.html
```

### Padrão de Testes

Todos os testes seguem **AAA Pattern** (Arrange, Act, Assert):

```java
@Test
@DisplayName("deve registrar novo usuário com sucesso")
void testRegisterSuccess() {
    // Arrange - Preparar dados e mocks
    RegisterRequest request = new RegisterRequest("email@example.com", "senha123", "Nome");
    when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
    
    // Act - Executar a ação
    AuthResult result = authService.register(request);
    
    // Assert - Verificar resultado
    assertNotNull(result);
    assertEquals("email@example.com", result.response().email());
    verify(userRepository).save(any(UserEntity.class));
}
```

### Boas Práticas para Testes

1. **Use `@DisplayName`** para documentação clara do teste
2. **Mock apenas dependências externas** (repositories, services)
3. **Use `lenient()`** para mocks que podem não ser usados
4. **Verifique comportamento**, não implementação
5. **Use `ArgumentCaptor`** para validações complexas
6. **Mantenha testes independentes** — sem dependências entre testes

### Dependências de Teste

```gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.security:spring-security-test'
testImplementation 'com.fasterxml.jackson.core:jackson-databind'
testImplementation 'org.mockito:mockito-core:5.11.0'
testImplementation 'org.mockito:mockito-junit-jupiter:5.11.0'
```

## Fluxos Principais

### 1. Autenticação (AuthService)

```java
// Teste: testRegisterSuccess
1. User.register(email, password, fullName)
2. AuthService valida email único
3. Senha é hasheada com BCrypt
4. UserEntity é criado e salvo
5. TaxonomyProvisioningService provisiona categorias padrão
6. JWT tokens são gerados (access + refresh)
7. Response com accessToken (15m TTL)
```

### 2. Gerenciamento de Entradas (EntryService)

```java
// Teste: testCriarEntradaUnica
1. User cria lançamento com EntryRequest
2. EntryService valida campos obrigatórios
3. Categoria é validada (ownership)
4. EntryEntity é criado e salvo
5. Se parcelado: EntryScheduleService gera parcelas
6. Se recorrente: EntryScheduleService gera mensalidades
7. List<EntryEntity> retorna todas as entradas geradas
```

### 3. Provisão de Taxonomia (TaxonomyProvisioningService)

```java
// Teste: testProvisionarNovoUsuario
1. User registra
2. TaxonomyProvisioningService.provisionar(userId) é chamado
3. Carrega templates (CSVs em resources/taxonomy/)
4. Cria 22 categorias + 85 subcategorias
5. Cria 5 contas + 6 formas de pagamento
6. Idempotente: chamadas subsequentes não duplicam
```

## Convenções de Código

### Nomes de Classes

```java
// Services
AuthService              // Lógica de autenticação
EntryService           // Lógica de lançamentos
TaxonomyProvisioningService  // Lógica de provisão

// Entities
UserEntity             // Usuário
EntryEntity            // Lançamento
CategoryEntity         // Categoria
RefreshTokenEntity     // Token de refresh

// DTOs
RegisterRequest        // Entrada do registro
AuthResponse          // Resposta de autenticação
EntryResponse         // Resposta de lançamento

// Testes
AuthServiceTest       // Testes de AuthService
AuthServiceTest.java  // Arquivo de testes
```

### Padrão de Método

```java
// Services
public List<EntryEntity> criar(UUID userId, EntryRequest pedido)
public List<EntryEntity> listarMes(UUID userId, YearMonth periodo)
public EntryEntity marcarPago(UUID userId, UUID id)

// Testes
void testCriarEntradaUnica()
void testListarMes()
void testMarcarPago()
void testFalhaComDadosInvalidos()
```

## Debugging

### Backend

```bash
# Run com debug
./gradlew bootRun --debug

# Ver logs da aplicação
tail -f build/logs/spring.log

# Executar teste específico com debug
./gradlew test --tests AuthServiceTest.testRegisterSuccess --debug
```

### Testes

```bash
# Ver falhas no console
./gradlew test --info 2>&1 | grep -i "FAILED\|error"

# Gerar relatório HTML com detalhes
./gradlew test --rerun-tasks
open build/reports/tests/test/index.html

# Ver assertion errors
./gradlew test --tests AuthServiceTest --info | grep -A 10 "AssertionError"
```

### Database

```bash
# Verificar status de migrações
./gradlew liquibaseStatus

# Executar migrações manualmente
./gradlew liquibaseUpdate

# Rollback
./gradlew liquibaseRollback
```

## Ambiente de Desenvolvimento

### Variáveis Obrigatórias

```bash
export URL_DB=localhost:5432/budgetflow_db
export USERNAME_DB=postgres
export PASSWORD_DB=postgres
export JWT_SECRET=sua-chave-super-secreta-com-minimo-32-caracteres
export CORS_ALLOWED_ORIGINS=http://localhost:5173
export SPRING_PROFILES_ACTIVE=dev
```

### Iniciar Tudo

```bash
# Terminal 1: Backend
./gradlew bootRun

# Terminal 2: Testes (contínuos)
./gradlew test --continuous

# Terminal 3: Monitorar logs
tail -f build/logs/spring.log
```

## Principais Features Implementadas

### ✅ AuthService (8 testes)
- Registro de novo usuário
- Login com validação de credenciais
- Refresh de tokens JWT
- Logout e revogação de tokens
- Tratamento de exceções (email duplicado, token inválido)

### ✅ JwtService (9 testes)
- Geração de access token com claims
- Parsing e validação de tokens
- Geração de refresh token opaco (256 bits)
- TTL configurável
- Rejeição de tokens inválidos/expirados

### ✅ EntryService (16 testes)
- Criar entradas únicas
- Criar parcelamentos (12x, 24x, etc)
- Criar recorrências (6 meses, 12 meses)
- Listar por mês ou período
- Marcar como pago
- Validação de campos
- Scoping por usuário

### ✅ EntryScheduleService (14 testes)
- Criar parcelas com número de ordem
- Criar mensalidades com datas futuras
- Estender recorrências
- Cancelar futuras
- Validações de parâmetros

### ✅ TaxonomyProvisioningService (3 testes)
- Provisionar categorias padrão na criação de usuário
- Carregar de templates (CSVs)
- Idempotência (não duplica)

### ✅ LookupService (5 testes)
- Listar contas do usuário
- Listar formas de pagamento
- Filtrar arquivadas
- Ordenar por nome

## Extensões Futuras

### Testes de Integração
```bash
# Adicionar TestContainers para testes com BD real
testImplementation 'org.testcontainers:testcontainers:1.19.0'
testImplementation 'org.testcontainers:postgresql:1.19.0'
```

### Testes de Controller
```bash
# Usar @WebMvcTest para testar endpoints
@WebMvcTest(AuthController.class)
void testRegisterEndpoint()
```

### Coverage Report
```bash
# Adicionar Jacoco
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Referências

- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Security Testing](https://spring.io/guides/topicals/spring-security-architecture)

---

**Última atualização:** 13 de setembro de 2026  
**Branch:** refatoracao/padroniza_arquitetura
