# Segurança FATEC ZL — Sistema de Segurança Comunitária

Sistema web desenvolvido como Trabalho de Conclusão de Curso (TCC) para fortalecer a
segurança dos estudantes no entorno da **FATEC Zona Leste**. Reúne alertas de ocorrências
em tempo real, caronas solidárias entre alunos (com chat e rastreamento), acompanhamento
de ônibus da SPTrans e um painel administrativo de moderação.

---

## Sumário

- [Tecnologias](#tecnologias)
- [Funcionalidades](#funcionalidades)
- [Arquitetura](#arquitetura)
- [Como executar](#como-executar)
- [Configuração](#configuração)
- [Usuários padrão](#usuários-padrão)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Tecnologias

### Backend
- **Java 17** + **Spring Boot 3.2.5**
- **Spring Web** (MVC) e **Spring WebSocket** (STOMP) — chat e rastreamento em tempo real
- **Spring Security** — autenticação por sessão, CSRF, papéis `USER` / `MODERATOR` / `ADMIN`
- **Spring Data JPA** (Hibernate) — persistência
- **Spring Validation**, **Cache**, **Actuator** e **Mail**
- **Lombok** e **MapStruct** — redução de boilerplate
- Schema gerado automaticamente pelo Hibernate (`ddl-auto=update`) + *stored procedures* via `db/procedures.sql`

### Frontend
- **Thymeleaf** (renderização server-side) + **Thymeleaf Extras Spring Security**
- **Bootstrap 5.3** e **Bootstrap Icons** (via CDN)
- **JavaScript puro (vanilla)** consumindo a API REST com `fetch`
- **Leaflet.js 1.9.4** + **OpenStreetMap** — mapas de alertas e de ônibus

### Banco de dados e integrações
- **MySQL 8** (produção) / **H2** (testes)
- **API SPTrans Olho Vivo** — posição dos ônibus em tempo real
- Geocodificação de endereços (autocomplete no cadastro de caronas)

### Testes
- JUnit 5, Spring Security Test e **Testcontainers** (MySQL)

---

## Funcionalidades

### 🚨 Alertas de Segurança
- Publicação de ocorrências (furto, roubo, atividade suspeita, falta de iluminação, outro)
  com localização descritiva e ponto no mapa
- Filtros por tipo, data e "meus alertas"
- Reações 👍 / 👎 (like/dislike)
- Listagem em duas seções — **recentes (≤ 72h)** e **anteriores** — ordenadas por reações
  líquidas (alertas com mais curtidas aparecem primeiro)
- Denúncia de alerta **com categoria e justificativa**, enviada à moderação

### 🚗 Caronas Solidárias
- Oferta e busca de caronas, com solicitação e aceite de passageiros pelo motorista
- **Chat em tempo real** entre motorista e passageiros confirmados (com mensagens
  automáticas do sistema, ex.: avisos de entrada na carona)
- Lista de participantes restrita aos integrantes confirmados
- **Rastreamento e geofencing** (WebSocket): fechamento automático ao passar do horário e
  finalização ao chegar ao destino
- Avaliação do motorista por estrelas (1–5) e denúncias de ocorridos
- Regras de visibilidade: caronas em aberto são públicas; iniciadas/fechadas/finalizadas
  ficam visíveis só aos integrantes e somem após avaliação (janela de 72h)

### 🚌 Ônibus em Tempo Real
- Mapa com a posição dos ônibus via integração com a API **SPTrans Olho Vivo**

### 📊 Estatísticas
- Gráficos de ocorrências por tipo, horário e volume

### 👤 Usuários
- Cadastro com validação de **e-mail institucional** (`@fatec.sp.gov.br`) e matrícula
- Classificação em **aluno / docente / funcionário**
- Alunos têm acesso por até **5 anos** (contados do ano de ingresso na matrícula);
  após o prazo, são desativados automaticamente
- Perfil com edição de dados, troca de senha e **histórico paginado** (alertas, caronas
  como motorista e como passageiro) com filtro por período

### 🛡️ Administração (papel ADMIN)
- Gestão de usuários (listagem paginada, filtros e exclusão)
- Moderação de **denúncias**, separadas em duas categorias: **caronas** e **alertas**
- Consulta via *stored procedure* (`sp_total_usuarios`)

---

## Arquitetura

Aplicação **monolítica híbrida MVC + REST**:

- `@Controller` (Thymeleaf) entrega as páginas HTML.
- `@RestController` sob `/api/**` fornece os dados, consumidos via `fetch` no navegador.
- WebSocket/STOMP (`/ws/**`) para chat e rastreamento ao vivo.

```
br.com.fatec.tcc
├── config           # Security, WebSocket, CORS, inicialização, tratamento de exceções
├── controller       # Páginas Thymeleaf (MVC)
├── rest.controller  # API REST (/api/**)
├── websocket        # Rastreamento em tempo real
├── service          # Regras de negócio
├── repository       # Spring Data JPA
├── model            # Entidades JPA
├── dto / record     # Objetos de transporte
├── scheduler        # Tarefas agendadas (fechar caronas, desativar alunos expirados)
└── specification    # Consultas dinâmicas (filtros)
```

---

## Como executar

### Pré-requisitos
- **JDK 17+**
- **MySQL 8** em execução
- **Maven 3.6+** (o projeto não inclui o wrapper `mvnw`; use o Maven instalado ou a IDE)

### Passos

```bash
# 1. Clone o repositório
git clone https://github.com/mrossine/tcc-seguranca.git
cd tcc-seguranca

# 2. Ajuste as credenciais do banco em src/main/resources/application.properties
#    (veja a seção Configuração abaixo)

# 3. Execute
mvn spring-boot:run
```

O banco `tcc_seguranca` é criado automaticamente (`createDatabaseIfNotExist=true`), o schema
é gerado pelo Hibernate e as *stored procedures* são aplicadas no startup.

Acesse: **http://localhost:8080**

> **IDE (Eclipse/IntelliJ):** o projeto também roda diretamente pela classe
> `br.com.fatec.tcc.TccApplication`.

---

## Configuração

As configurações ficam em `src/main/resources/application.properties`. Os principais pontos:

```properties
# Banco de dados — ajuste usuário e senha para o seu ambiente
spring.datasource.url=jdbc:mysql://localhost:3306/tcc_seguranca?...&createDatabaseIfNotExist=true
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA

# Schema gerido pelo Hibernate
spring.jpa.hibernate.ddl-auto=update

# Token da API SPTrans Olho Vivo (cadastre o seu em sptrans.com.br/desenvolvedores)
sptrans.token=SEU_TOKEN

# Porta
server.port=8080
```

> ⚠️ **Não versione credenciais reais.** Substitua usuário, senha e token pelos seus valores
> locais antes de executar.

---

## Usuários padrão

Criados automaticamente no primeiro startup ([`DataInitializer`](src/main/java/br/com/fatec/tcc/config/DataInitializer.java)):

| Papel      | E-mail                        | Senha          |
|------------|-------------------------------|----------------|
| ADMIN      | `admin@fatec.sp.gov.br`       | `admin123`     |
| MODERATOR  | `moderador@fatec.sp.gov.br`   | `moderador123` |

Demais usuários são criados pela tela de cadastro (`/cadastro`).

---

## Estrutura do projeto

```
tcc-seguranca/
├── src/main/java/br/com/fatec/tcc/   # Código-fonte (ver Arquitetura)
├── src/main/resources/
│   ├── templates/                    # Páginas Thymeleaf
│   ├── static/                       # CSS, JS e assets
│   ├── db/procedures.sql             # Stored procedures
│   └── application.properties        # Configuração
├── src/test/java/                    # Testes
└── pom.xml
```

---

*Projeto acadêmico (TCC) — FATEC Zona Leste. O sistema apenas conecta a comunidade;
cada usuário é responsável pela própria segurança. Em emergências, ligue 190.*
