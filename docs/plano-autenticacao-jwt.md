# Plano: Spring Security — Autenticação Email/Senha + JWT

## Contexto

O projeto `budgetflow` já tem `UserEntity`/`UserRepository` (com `findByEmail`) e as
dependências `spring-boot-starter-security` + `jjwt-api/impl/jackson:0.12.7` no
`build.gradle`, mas nenhum código de segurança foi escrito ainda — o pacote
`domain.service` existe e está vazio. O objetivo é implementar autenticação
stateless completa: registro de usuário, login, emissão de access token (JWT
curto) + refresh token (opaco, persistido com hash, rotacionado), e proteção
das rotas via filtro JWT, aproveitando o campo `status` já existente em
`UserEntity` para bloquear usuários suspensos/deletados.

Decisões confirmadas:
- Incluir endpoint de registro (`POST /api/auth/register`).
- Estratégia access token + refresh token (rotação, revogável).
- Sem campo `role` por enquanto — toda autenticação recebe authority fixa `ROLE_USER`.

## Estrutura de pacotes (novos)

```
com.lwv.budgetflow
├── domain
│   ├── entity/RefreshTokenEntity.java
│   ├── repository/RefreshTokenRepository.java
│   └── service/AuthService.java
├── security
│   ├── config/SecurityConfig.java
│   ├── jwt/
│   │   ├── JwtProperties.java
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtAuthenticationEntryPoint.java
│   └── userdetails/
│       ├── UserPrincipal.java
│       └── CustomUserDetailsService.java
└── web
    ├── controller/AuthController.java
    ├── dto/{RegisterRequest,LoginRequest,RefreshRequest,AuthResponse}.java
    └── exception/{GlobalExceptionHandler,EmailAlreadyExistsException}.java
```

`security` e `web` ficam como pacotes irmãos de `domain`, seguindo a convenção
já usada (infra separada de domínio).

## 1. Banco de dados — refresh tokens

Novo changeset `src/main/resources/db/changelog/changes/003-create-refresh-tokens-table.sql`
(mesmo estilo do `002-create-users-table.sql`, liquibase formatted sql):

```sql
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES users(id),
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
```

Guarda-se o **hash SHA-256** do refresh token, nunca o valor bruto — mesmo
princípio do `password_hash`, para que um vazamento do banco não exponha
tokens utilizáveis.

`RefreshTokenEntity` segue o padrão de `UserEntity`/`OrganizationEntity`
(Lombok `@Getter @Setter @SuperBuilder`, `@CreationTimestamp`).
`RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID>` com
`findByTokenHashAndRevokedAtIsNull(String hash)`.

## 2. Configuração (`application*.yaml`)

Em `application.yaml` (base, mesmo estilo do bloco `datasource` existente):

```yaml
security:
  jwt:
    secret: ${JWT_SECRET:dev-only-change-me-...-base64-32bytes}
    issuer: budgetflow
    access-token-ttl: 15m
    refresh-token-ttl: 7d
```

`JwtProperties` com `@ConfigurationProperties("security.jwt")` (+
`@EnableConfigurationProperties` ou `@Component`), expondo `secret`, `issuer`,
`accessTokenTtl`, `refreshTokenTtl` como `Duration`.

**Importante**: o default do `secret` só é aceitável em dev. Documentar (não
sobrescrever automaticamente) que `application-hml.yaml`/`application-prd.yaml`
devem ter `JWT_SECRET` fornecido via variável de ambiente real (`openssl rand
-base64 32`), sem fallback embutido — nenhum segredo real deve ir para um yaml
versionado.

## 3. UserDetails / autenticação

- `UserPrincipal implements UserDetails`: wrapper sobre `UserEntity`.
  - `getAuthorities()` → `List.of(new SimpleGrantedAuthority("ROLE_USER"))` (fixo, conforme decidido).
  - `isEnabled()` / `isAccountNonLocked()` → derivam de `UserStatus`: `ACTIVE` → true; `SUSPENDED`/`DELETED` → false. Isso dá uso real ao enum que já existe.
  - `getUsername()` → `email`, `getPassword()` → `passwordHash`.
- `CustomUserDetailsService implements UserDetailsService`: usa
  `UserRepository.findByEmail` (já existe) e lança `UsernameNotFoundException`
  se ausente.
- `PasswordEncoder` bean: `BCryptPasswordEncoder` em `SecurityConfig`.
- `AuthenticationManager` bean: obtido de `AuthenticationConfiguration`
  (Spring Boot conecta `CustomUserDetailsService` + `PasswordEncoder`
  automaticamente via `DaoAuthenticationProvider`).

## 4. JWT (`security.jwt`)

`JwtService` usando a API do jjwt 0.12.7 já presente:
- `generateAccessToken(UserPrincipal)`: claims `sub`=userId, `email`, `iss`, `iat`, `exp` (agora + `accessTokenTtl`), assinado com `Jwts.SIG.HS256` usando `Keys.hmacShaKeyFor(secretBytes)`.
- `parseAndValidate(String token)`: `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`, captura `ExpiredJwtException`/`JwtException` e retorna `Optional`/lança exceção específica.
- Refresh token: string aleatória opaca (`SecureRandom`/`UUID.randomUUID()` + `UUID.randomUUID()` ou `Base64` de 32 bytes) — **não** é um JWT, é gerado, hasheado (SHA-256) e persistido em `refresh_tokens`. Mantém o design simples e revogável (se fosse JWT também, não daria para revogar sem blacklist).

`JwtAuthenticationFilter extends OncePerRequestFilter`:
- Extrai `Authorization: Bearer <token>`.
- Se válido, monta `UserPrincipal` a partir das claims do token (sem ida ao banco a cada request — approach stateless) e popula o `SecurityContextHolder`.
- Se ausente/inválido, segue a cadeia sem autenticar (deixa o `AuthenticationEntryPoint` responder 401 nas rotas protegidas).

`JwtAuthenticationEntryPoint implements AuthenticationEntryPoint`: responde
401 com corpo JSON padronizado (reaproveitado pelo `GlobalExceptionHandler`).

## 5. `SecurityConfig`

`SecurityFilterChain` bean:
- `csrf(disable)` — API stateless, sem cookies de sessão.
- `sessionManagement(STATELESS)`.
- `authorizeHttpRequests`: `permitAll()` em `/api/auth/**` e `/actuator/health`; `authenticated()` no resto.
- `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`.
- `exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)`.

## 6. `AuthService` (domain/service)

- `register(RegisterRequest)`: valida e-mail não duplicado (`UserRepository.findByEmail`) → lança `EmailAlreadyExistsException` (409) se existir; senão `passwordEncoder.encode(...)`, salva `UserEntity` (`status=ACTIVE`), gera par de tokens (auto-login pós-registro) e retorna `AuthResponse`.
- `login(LoginRequest)`: `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, senha))` — deixa o Spring Security validar credenciais e status (`isEnabled`/`isAccountNonLocked`); em sucesso, atualiza `lastLoginAt`, emite tokens.
- `refresh(String refreshToken)`: hash do token recebido → busca em `RefreshTokenRepository` não revogado e não expirado → revoga o antigo (`revokedAt = now`) → emite novo par (rotação) → se o token não for encontrado/expirado, lança exceção → 401.
- `logout(String refreshToken)`: marca o refresh token como revogado. (Access token continua válido até expirar naturalmente — limitação esperada de JWT stateless, TTL curto de 15 min mitiga.)

## 7. `AuthController` (web/controller) + DTOs

Endpoints REST:
- `POST /api/auth/register` → `RegisterRequest{email, password, fullName}` (Bean Validation: `@Email`, `@Size(min=8)`) → `201` + `AuthResponse`.
- `POST /api/auth/login` → `LoginRequest{email, password}` → `200` + `AuthResponse`.
- `POST /api/auth/refresh` → `RefreshRequest{refreshToken}` → `200` + `AuthResponse`.
- `POST /api/auth/logout` → `RefreshRequest{refreshToken}` → `204`.

`AuthResponse{accessToken, refreshToken, tokenType="Bearer", expiresIn}`.

`GlobalExceptionHandler` (`@RestControllerAdvice`): mapeia
`BadCredentialsException`/`DisabledException` → 401 mensagem genérica (evita
enumeração de usuários), `EmailAlreadyExistsException` → 409,
`MethodArgumentNotValidException` → 400 com detalhes de campo.

## 8. Endpoint de verificação (mínimo, para provar que a proteção funciona)

Adicionar `GET /api/users/me` simples (retorna id/email/fullName do usuário
autenticado via `@AuthenticationPrincipal UserPrincipal`) — não protegido por
`permitAll`, então serve como prova end-to-end de que o filtro JWT protege
rotas normais.

## Arquivos principais

- `build.gradle` — nenhuma dependência nova necessária (security + jjwt já presentes).
- `src/main/resources/db/changelog/changes/003-create-refresh-tokens-table.sql` (novo)
- `src/main/resources/application.yaml` (+ bloco `security.jwt`)
- `domain/entity/RefreshTokenEntity.java`, `domain/repository/RefreshTokenRepository.java`
- `domain/service/AuthService.java`
- `security/config/SecurityConfig.java`
- `security/jwt/{JwtProperties,JwtService,JwtAuthenticationFilter,JwtAuthenticationEntryPoint}.java`
- `security/userdetails/{UserPrincipal,CustomUserDetailsService}.java`
- `web/controller/{AuthController,UserController}.java`
- `web/dto/{RegisterRequest,LoginRequest,RefreshRequest,AuthResponse}.java`
- `web/exception/{GlobalExceptionHandler,EmailAlreadyExistsException}.java`

## Verificação

1. `./gradlew build` — compila e roda `BudgetflowApplicationTests`.
2. Testes unitários novos (com mocks, sem precisar de Postgres):
   - `JwtServiceTest`: gera token, valida claims, testa expiração/assinatura inválida.
   - `AuthServiceTest`: mocka `UserRepository`/`RefreshTokenRepository`/`PasswordEncoder`, cobre registro duplicado, login inválido, refresh expirado/revogado.
   - `AuthControllerTest` (`@WebMvcTest` + `AuthService` mockado): valida contratos HTTP (status codes, validação de payload).
3. Teste manual local (requer Postgres rodando, perfil `dev`):
   - `curl -X POST localhost:8080/api/auth/register -d '{"email":"a@a.com","password":"12345678","fullName":"A"}'`
   - `curl -X POST localhost:8080/api/auth/login -d '{"email":"a@a.com","password":"12345678"}'`
   - `curl localhost:8080/api/users/me -H "Authorization: Bearer <accessToken>"` → 200
   - Sem header → 401 (valida `JwtAuthenticationEntryPoint`)
   - `curl -X POST localhost:8080/api/auth/refresh -d '{"refreshToken":"..."}'` → novo par de tokens; token antigo reutilizado → 401 (confirma rotação/revogação)
