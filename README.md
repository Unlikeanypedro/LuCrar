# 🚗 LUCRAR

> [!NOTE]
> Sistema web para **efetuar, modificar, consultar e cancelar pedidos de aluguel de automóveis**, com análise financeira por agentes (empresas e bancos) e execução de contrato. Laboratório 02 — DIAW, Engenharia de Software, PUC Minas.

![Java](https://img.shields.io/badge/Java-25-007ec6?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.x-007ec6?style=for-the-badge&logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-007ec6?style=for-the-badge&logo=apachemaven&logoColor=white)
![React](https://img.shields.io/badge/React-19-007ec6?style=for-the-badge&logo=react&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-7-007ec6?style=for-the-badge&logo=vite&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-PostgreSQL-007ec6?style=for-the-badge&logo=supabase&logoColor=white)

---

## 📚 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades Principais](#-funcionalidades-principais)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Arquitetura](#-arquitetura)
- [Instalação e Execução](#-instalação-e-execução)
- [Estrutura de Pastas](#-estrutura-de-pastas)
- [API](#-api)
- [Demonstração](#-demonstração)
- [Testes](#-testes)
- [Documentações utilizadas](#-documentações-utilizadas)
- [Autores](#-autores)
- [Agradecimentos](#-agradecimentos)
- [Licença](#-licença)

---

## 📝 Sobre o Projeto

Empresas e bancos precisam de um canal único para receber pedidos de aluguel, analisá-los financeiramente e formalizar contratos. Este sistema centraliza o fluxo:

1. O **cliente** se cadastra, faz login e introduz um pedido de aluguel (automóvel + período).
2. Um **agente** (empresa ou banco) analisa o pedido do ponto de vista financeiro e, se o parecer for positivo, o coloca para execução do contrato. **Bancos** podem associar um contrato de crédito ao aluguel.
3. O cliente acompanha o **status** do pedido a qualquer momento.

Contexto: trabalho acadêmico (Laboratório 02, 15 pontos), com modelagem UML alinhada ao código.

---

## ✨ Funcionalidades Principais

- 🔐 **Autenticação:** cadastro e login obrigatórios (usuário e senha, BCrypt); papéis `CLIENTE`, `EMPRESA`, `BANCO`.
- 👤 **CRUD de cliente:** RG, CPF, nome, endereço, profissão e até 3 rendimentos (empregador + valor).
- 🚘 **Catálogo de automóveis:** matrícula, ano, marca, modelo, placa, valor da diária e proprietário (cliente, empresa ou banco). Clientes podem cadastrar veículos próprios; agentes podem cadastrar veículos vinculados a outras contas.
- 📄 **Pedidos de aluguel:** criar, modificar, consultar e cancelar (cliente); avaliar e executar contrato (agente).
- 💳 **Contrato de crédito:** concedido por um banco agente ao aprovar o pedido.

---

## 🛠 Tecnologias Utilizadas

| Camada         | Tecnologia                                                  |
| -------------- | ----------------------------------------------------------- |
| Linguagem      | Java — JDK 25                                               |
| Back-end       | Spring Boot 4.0.x (Web MVC, Data JPA, Security, Validation) |
| Build          | Apache Maven                                                |
| Front-end      | React + Vite                                                |
| Banco de dados | Supabase (PostgreSQL)                                       |
| Autenticação   | Login obrigatório (usuário e senha) com Spring Security     |

---

## 🏗 Arquitetura

**MVC em camadas** no back-end e SPA no front-end, divididos em dois subsistemas: _gestão de pedidos e contratos_ (Spring Boot) e _construção dinâmica das páginas web_ (React + Vite).

- **Model:** entidades JPA (`model`) e repositórios (`repository`).
- **Controller:** endpoints REST (`controller`), entrada via DTOs (`dto`).
- **Service:** regras de negócio e autorização (`service`).
- **View:** React + Vite consumindo a API REST.
- **Segurança:** HTTP Basic sobre sessão _stateless_, senhas com BCrypt (`config/SecurityConfig`).

Diagramas (casos de uso, classes, pacotes) e mapeamento modelo ↔ código: [`docs/modelagem.md`](./docs/modelagem.md). Histórias de usuário: [`docs/historias-usuario.md`](./docs/historias-usuario.md).

---

## 🔧 Instalação e Execução

### Pré-requisitos

- **JDK 25** e **Maven 3.9+**
- **Node.js** LTS (v18+) e npm
- Um projeto no **Supabase** (PostgreSQL)

### 🔑 Variáveis de Ambiente

Veja [`codigo/.env.example`](./codigo/.env.example). Back-end (variáveis de ambiente do sistema):

| Variável                 | Descrição                      | Exemplo                                                              |
| ------------------------ | ------------------------------ | -------------------------------------------------------------------- |
| `DB_URL`                 | URL JDBC do Supabase           | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require` |
| `DB_USER`                | Usuário do banco               | `postgres`                                                           |
| `DB_PASSWORD`            | Senha do banco                 | `***`                                                                |
| `SERVER_PORT`            | Porta do back-end (opcional)   | `8080`                                                               |
| `AGENT_REGISTRATION_KEY` | Chave para cadastro de agentes | `sua_chave_secreta`                                                  |
| `DOMAIN`                 | Domínio para HTTPS remoto      | `seu-dominio.example`                                                |

> As tabelas são criadas automaticamente na primeira execução (`ddl-auto=update`) e 3 automóveis de exemplo são inseridos.

### ⚡ Como Executar (sem Docker)

**Terminal 1 — Back-end**

```bash
cd codigo/backend
mvn spring-boot:run
```

🚀 http://localhost:8080

**Terminal 2 — Front-end**

```bash
cd codigo/frontend
npm install
npm run dev
```

🎨 http://localhost:5173

### 🐳 Docker

O front-end React é servido pelo Nginx, que encaminha `/api` ao Spring Boot. O banco continua no Supabase; as credenciais ficam somente no container do back-end.

Configure Docker Engine com Docker Compose v2 e um projeto Supabase PostgreSQL. Na pasta `codigo`, copie `.env.example` para `.env` e preencha `DB_URL`, `DB_USER`, `DB_PASSWORD` e `AGENT_REGISTRATION_KEY`.

**Acesso local — porta 3000:**

```bash
cd codigo
cp .env.example .env
docker compose up -d --build
docker compose ps
```

Abra `http://localhost:3000`; a saúde da API pode ser conferida em `http://localhost:3000/api/health`.

**Execução remota com HTTPS:** defina `DOMAIN` no `.env` com um domínio cujo DNS aponte para o servidor. Libere TCP 80/443 e UDP 443. Caddy solicita e renova o certificado TLS automaticamente.

```bash
cd codigo
docker compose --profile remote up -d --build
docker compose ps
```

Acesse `https://seu-dominio.example`. O back-end não é publicado diretamente; no acesso remoto, somente o proxy HTTPS fica exposto. Para acompanhar os serviços e encerrar:

```bash
docker compose logs -f
docker compose down
```

Não versione o arquivo `.env`. Para atualizar após publicar alterações, execute novamente o comando `docker compose up -d --build` correspondente ao tipo de execução.

### 🚀 Build de Produção

```bash
cd codigo/backend && mvn clean package      # gera target/aluguel-carros-1.0.0.jar
cd codigo/frontend && npm run build         # gera dist/
```

---

## 📂 Estrutura de Pastas

```
.
├── README.md
├── codigo/
│   ├── .env.example
│   ├── .gitignore
│   ├── Caddyfile
│   ├── compose.yaml              # Produção
│   ├── compose.dev.yaml          # Desenvolvimento (hot reload)
│   ├── backend/                  # Spring Boot (MVC)
│   │   ├── Dockerfile            # Produção (JAR)
│   │   ├── Dockerfile.dev        # Desenvolvimento (Maven)
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/java/com/aluguel/
│   │       │   ├── controller/     # Endpoints REST
│   │       │   ├── service/        # Regras de negócio
│   │       │   ├── repository/     # Spring Data JPA
│   │       │   ├── model/          # Entidades e enums
│   │       │   ├── dto/            # Objetos de entrada
│   │       │   └── config/         # Segurança e dados iniciais
│   │       ├── main/resources/
│   │       │   ├── application.properties
│   │       │   └── schema.sql
│   │       └── test/               # Teste do fluxo completo (H2)
│   └── frontend/                 # React + Vite
│       ├── Dockerfile            # Produção (Nginx)
│       ├── Dockerfile.dev        # Desenvolvimento (Vite)
│       ├── nginx.conf            # Proxy /api para o backend
│       ├── package.json
│       └── src/
├── docs/
│   ├── modelagem.md            # Casos de uso, classes e pacotes (Mermaid)
│   └── historias-usuario.md
```

---

## 🔌 API

Todas as rotas exigem `Authorization: Basic ...`, exceto o cadastro.

| Método         | Rota                          | Descrição                                                         | Quem                        |
| -------------- | ----------------------------- | ----------------------------------------------------------------- | --------------------------- |
| POST           | `/api/auth/register`          | Cadastro (cliente ou agente)                                      | público                     |
| GET            | `/api/auth/me`                | Valida login                                                      | autenticado                 |
| GET/PUT/DELETE | `/api/clientes[/{id}]`        | CRUD de cliente                                                   | cliente (próprio) / agentes |
| GET/POST       | `/api/automoveis`             | Lista / cadastra automóveis                                       | todos / agentes             |
| GET/POST       | `/api/pedidos`                | Lista / introduz pedido                                           | todos / cliente             |
| GET/PUT/DELETE | `/api/pedidos/{id}`           | Consulta / modifica / cancela                                     | cliente (próprio) / agentes |
| POST           | `/api/pedidos/{id}/avaliacao` | Aprova ou rejeita (`{aprovado, valorCredito, parecerFinanceiro}`) | agentes                     |
| POST           | `/api/pedidos/{id}/contrato`  | Executa contrato                                                  | agentes                     |

---

## 🎥 Demonstração

_Adicione aqui prints/GIFs das telas: Login, Cadastro, Pedidos (cliente), Pedidos (agente), Clientes, Catálogo._

---

## 🧪 Testes

```bash
cd codigo/backend
mvn test
```

`FluxoAluguelTest` cobre: acesso sem login (401), cadastro, login, criação de pedido, bloqueio de avaliação por cliente, aprovação com crédito, execução de contrato e não exposição de senha. Usa H2 em memória (apenas no escopo de teste).

---

## 🔗 Documentações utilizadas

- [Spring Boot](https://docs.spring.io/spring-boot/) · [Spring Security](https://docs.spring.io/spring-security/reference/)
- [React](https://react.dev/) · [Vite](https://vitejs.dev/)
- [Supabase](https://supabase.com/docs)

---

## 👥 Autores

| Nome             | GitHub                                               | E-mail                      |
| ---------------- | ---------------------------------------------------- | --------------------------- |
| _Pedro Oliveira_ | [@Unlikeanypedro](https://github.com/Unlikeanypedro) | phsoliveira@sga.pucminas.br |

---

## 🙏 Agradecimentos

- [**Engenharia de Software PUC Minas**](https://www.instagram.com/engsoftwarepucminas/)
- [**Prof. Dr. João Paulo Aramuni**](https://github.com/joaopauloaramuni)

---

## 📄 Licença

Distribuído sob a **Licença MIT**.
