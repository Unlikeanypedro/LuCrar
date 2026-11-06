# 🐳 LuCrar — Código e Docker

Este diretório contém o código-fonte e a configuração de contêineres do LuCrar.

## Estrutura

```
codigo/
├── compose.yaml              # Produção (Nginx + Caddy opcional)
├── compose.dev.yaml          # Desenvolvimento (hot reload)
├── Caddyfile                 # Proxy HTTPS para execução remota
├── .env.example              # Modelo de variáveis de ambiente
├── .gitignore
├── backend/                  # Spring Boot (MVC)
│   ├── Dockerfile            # Imagem de produção (JAR)
│   ├── Dockerfile.dev        # Imagem de desenvolvimento (Maven + hot reload)
│   ├── pom.xml
│   └── src/
├── frontend/                 # React + Vite
│   ├── Dockerfile            # Imagem de produção (Nginx + build estático)
│   ├── Dockerfile.dev        # Imagem de desenvolvimento (Vite + HMR)
│   ├── nginx.conf            # Proxy /api para o backend
│   ├── package.json
│   └── src/
```

## Pré-requisitos

- Docker Engine com Docker Compose v2
- Um projeto Supabase PostgreSQL

## Variáveis de Ambiente

Copie `.env.example` para `.env` e preencha os valores reais:

```bash
cp .env.example .env
```

| Variável                 | Descrição                                      | Obrigatório |
| ------------------------ | ---------------------------------------------- | ----------- |
| `DB_URL`                 | URL JDBC do Supabase                           | Sim         |
| `DB_USER`                | Usuário do banco                               | Sim         |
| `DB_PASSWORD`            | Senha do banco                                 | Sim         |
| `DB_POOL_MAX_SIZE`       | Tamanho do pool de conexões (padrão 5)         | Não         |
| `SERVER_PORT`            | Porta do back-end (padrão 8080)                | Não         |
| `AGENT_REGISTRATION_KEY` | Chave para cadastro de agentes (EMPRESA/BANCO) | Sim         |
| `DOMAIN`                 | Domínio para HTTPS remoto (padrão localhost)   | Não         |

## Execução Local (produção)

Abra `http://localhost:3000`. O front-end é servido pelo Nginx e encaminha `/api` ao Spring Boot.

```bash
docker compose up -d --build
docker compose ps
```

A saúde da API pode ser conferida em `http://localhost:3000/api/health`.

Para encerrar:

```bash
docker compose down
```

## Execução Remota com HTTPS

Defina `DOMAIN` no `.env` com um domínio cujo DNS aponte para o servidor. Libere as portas TCP 80/443 e UDP 443. O Caddy solicita e renova o certificado TLS automaticamente.

```bash
docker compose --profile remote up -d --build
docker compose ps
```

Acesse `https://seu-dominio.example`. O back-end não é publicado diretamente; somente o proxy HTTPS fica exposto.

## Desenvolvimento com Hot Reload

Use o Compose de desenvolvimento para trabalhar sem rebuild a cada alteração:

```bash
docker compose -f compose.dev.yaml up --build --watch
```

Abra `http://localhost:5173`. Alterações em `frontend/src` entram via HMR do Vite; alterações no back-end sincronizam e reiniciam apenas o back-end.

Para encerrar:

```bash
docker compose -f compose.dev.yaml down
```

No modo de desenvolvimento, contas de demonstração são criadas automaticamente:

| Perfil  | Usuário         | Senha     |
| ------- | --------------- | --------- |
| Empresa | `empresa-teste` | `EMPRESA` |
| Banco   | `banco-teste`   | `BANCO`   |

## Build de Produção (sem Docker)

```bash
cd backend && mvn clean package      # gera target/aluguel-carros-1.0.0.jar
cd frontend && npm run build         # gera dist/
```

## Testes

```bash
cd backend
mvn test
```

`FluxoAluguelTest` cobre: acesso sem login (401), cadastro, login, criação de pedido, bloqueio de avaliação por cliente, aprovação com crédito, execução de contrato e não exposição de senha. Usa H2 em memória (apenas no escopo de teste).
