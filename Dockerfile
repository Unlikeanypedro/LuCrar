# syntax=docker/dockerfile:1
# Imagem única (front-end + API) para hospedagens que constroem a partir de um
# Dockerfile e não aceitam docker compose (ex.: Render, Railway, Fly.io).
#
# Este arquivo é feito para contexto = RAIZ do repositório
# (Root Directory vazio e Dockerfile Path = Dockerfile).
# codigo/Dockerfile é o equivalente para Root Directory = codigo.

# ---- 1. Front-end (React + Vite) ----
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY codigo/frontend/package.json codigo/frontend/package-lock.json ./
RUN npm ci
COPY codigo/frontend/ ./
RUN npm run build

# ---- 2. Back-end (Spring Boot) ----
FROM maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /workspace
COPY codigo/backend/pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline
COPY codigo/backend/src ./src
RUN mvn -B -DskipTests package

# ---- 3. Runtime: Nginx (estáticos + proxy /api) e JRE (API) ----
FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx curl \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /etc/nginx/sites-enabled/default

WORKDIR /app
COPY --from=backend /workspace/target/aluguel-carros-1.0.0.jar /app/app.jar
COPY --from=frontend /frontend/dist /usr/share/nginx/html
COPY codigo/deploy/nginx.conf /etc/nginx/conf.d/lucrar.conf
COPY codigo/deploy/start.sh /app/start.sh
RUN chmod +x /app/start.sh

# A API escuta em 8080 (interno) e o Nginx publica em 10000.
ENV SERVER_PORT=8080
EXPOSE 10000

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl --fail --silent http://localhost:10000/api/health || exit 1

CMD ["/app/start.sh"]
