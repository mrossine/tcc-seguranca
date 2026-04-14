 # Segurança Fatec ZL - Sistema de Segurança Comunitária

## Descrição do Projeto

Sistema web desenvolvido como Trabalho de Conclusão de Curso (TCC) com o objetivo de fortalecer a segurança dos estudantes no entorno da FATEC Zona Leste. A aplicação permite o compartilhamento de alertas em tempo real, organização de caronas solidárias e monitoramento de ocorrências na região.

## Tecnologias Utilizadas

- **Backend**: Java 17, Spring Boot 3.1.5, Spring Security, Spring Data JPA
- **Frontend**: HTML5, CSS3, Bootstrap 5, Thymeleaf, JavaScript
- **Banco de Dados**: MySQL 8.0
- **Build Tool**: Maven
- **Servidor**: Embedded Tomcat

## Funcionalidades Principais

1. **Alertas de Segurança**
   - Publicação de alertas em tempo real
   - Filtros por tipo, data e localização
   - Sistema de confirmação e denúncia

2. **Carona Solidária**
   - Oferecimento e busca de caronas
   - Sistema de solicitação e confirmação de vagas
   - Filtros por horário, origem e destino

3. **Painel Estatístico**
   - Gráficos de ocorrências por tipo e horário
   - Métricas de uso do sistema
   - Identificação de padrões de segurança

4. **Gestão de Usuários**
   - Cadastro com validação de e-mail institucional
   - Perfil com informações acadêmicas
   - Histórico de participação

## Pré-requisitos

- Java 17 ou superior
- MySQL 8.0
- Maven 3.6+
- Navegador web moderno

## Instalação e Execução

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/tcc-seguranca.git
cd tcc-seguranca
