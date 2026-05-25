# Segurança FATEC ZL — Sistema de Segurança Comunitária

Sistema web desenvolvido como TCC para fortalecer a segurança dos estudantes no entorno da FATEC Zona Leste.

---

## Tecnologias

- **Backend**: Java 17, Spring Boot 3.2.5, Spring Security, Spring Data JPA
- **Frontend**: HTML5, Bootstrap 5, Thymeleaf, JavaScript
- **Banco de Dados**: MySQL 8.0
- **Build**: Maven 3.6+
- **Migrations**: Flyway

---

## Como Executar

### Pré-requisitos
- Java 17+
- MySQL 8.0
- Maven 3.6+

### 1. Configure variáveis de ambiente

```bash
# Nunca coloque credenciais no application.properties!
export DB_URL=jdbc:mysql://localhost:3306/tcc_seguranca
export DB_USERNAME=seu_usuario
export DB_PASSWORD=sua_senha
export MAIL_USERNAME=seu@email.com
export MAIL_PASSWORD=sua_senha_email
```

### 2. Crie o banco de dados

```sql
CREATE DATABASE tcc_seguranca CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Execute

```bash
mvn spring-boot:run
```

O Flyway criará o schema automaticamente na primeira execução.

Acesse: http://localhost:8080

---

## Funcionalidades

- **Alertas de Segurança** — Publicação, filtros, confirmações e denúncias
- **Carona Solidária** — Oferta e busca de caronas entre estudantes
- **Painel Estatístico** — Gráficos por tipo, horário e volume de ocorrências
- **Gestão de Usuários** — Cadastro com validação de email institucional (@fatec.sp.gov.br)

---

## Melhorias Implementadas (vs. versão original)

### Segurança
| Problema Original | Solução Implementada |
|---|---|
| CSRF possivelmente desabilitado | `CookieCsrfTokenRepository` + token em todas as forms POST |
| Logout via GET | Logout via POST com CSRF token |
| Credenciais hardcoded | Variáveis de ambiente `${DB_PASSWORD}` etc. |
| Senha BCrypt strength padrão (10) | BCrypt strength 12 |
| Conta sem proteção brute force | Bloqueio temporário após 5 tentativas |
| Email não verificado | Verificação via token com expiração em 24h |
| Stack traces expostos | `GlobalExceptionHandler` com páginas amigáveis |
| Headers HTTP sem segurança | HSTS, CSP, X-Frame-Options configurados |
| Sessões múltiplas por usuário | `maximumSessions(1)` |

### Performance
| Problema Original | Solução Implementada |
|---|---|
| Possível N+1 queries | `JOIN FETCH` nas queries do repositório |
| `open-in-view=true` (padrão) | `open-in-view=false` |
| Sem paginação | `Pageable` em todas as listagens |
| Sem cache | Cache nas estatísticas do painel (Caffeine, 10min) |
| ddl-auto=create/update em prod | Flyway para migrations versionadas |
| Sem connection pool configurado | HikariCP com pool size definido |

### Qualidade de Código
| Problema Original | Solução Implementada |
|---|---|
| Entidades expostas diretamente | DTOs (`AlertaResponseDTO`, `AlertaRequestDTO`) |
| Lógica no controller | Service layer com responsabilidade única |
| Sem testes | Testes unitários com Mockito |
| Sem tratamento de exceções | `GlobalExceptionHandler` centralizado |
| Entidades sem auditoria | `criadoEm`/`atualizadoEm` automáticos com `@PrePersist` |
| Spring Boot 3.1.5 | Atualizado para 3.2.5 |
| `target/` no Git | Adicionar ao `.gitignore` |

### Arquitetura
```
src/
├── main/
│   ├── java/br/com/fatec/seguranca/
│   │   ├── config/       # SecurityConfig, CacheConfig
│   │   ├── controller/   # Controllers MVC (thin)
│   │   ├── dto/          # Request/Response DTOs
│   │   ├── entity/       # Entidades JPA
│   │   ├── enums/        # Enums de domínio
│   │   ├── exception/    # Exceções customizadas + GlobalExceptionHandler
│   │   ├── repository/   # JPA Repositories
│   │   └── service/      # Interfaces + Implementações
│   └── resources/
│       ├── db/migration/ # Scripts Flyway (V1__, V2__...)
│       ├── static/       # CSS, JS
│       └── templates/    # Templates Thymeleaf
└── test/                 # Testes unitários e de integração
```

---

## .gitignore Recomendado

```gitignore
target/
.settings/
.classpath
.project
.factorypath
*.class
application-prod.properties
.env
```

> **ATENÇÃO**: O repositório original contém `target/` e arquivos de IDE (`.settings/`, `.classpath`, `.project`, `.factorypath`) commitados. Isso expõe bytecode compilado e configurações locais — adicione ao `.gitignore`.