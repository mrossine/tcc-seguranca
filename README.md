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