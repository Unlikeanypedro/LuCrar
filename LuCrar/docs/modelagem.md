# Modelagem do Sistema — Aluguel de Carros

Diagramas em Mermaid (renderizados automaticamente pelo GitHub). Versão **v1.0**; atualizar este arquivo a cada revisão do modelo.

## 1. Diagrama de Casos de Uso

```mermaid
flowchart LR
    Cliente([Cliente])
    Agente([Agente: Empresa / Banco])

    subgraph Sistema["Sistema de Aluguel de Carros"]
        UC1(Cadastrar-se)
        UC2(Autenticar - login)
        UC3(Gerenciar cliente - CRUD)
        UC4(Introduzir pedido)
        UC5(Modificar pedido)
        UC6(Consultar pedidos e status)
        UC7(Cancelar pedido)
        UC8(Avaliar pedido financeiramente)
        UC9(Conceder crédito)
        UC10(Executar contrato)
        UC11(Gerenciar automóveis)
    end

    Cliente --- UC1 & UC3 & UC4 & UC5 & UC6 & UC7
    Agente --- UC1 & UC5 & UC6 & UC8 & UC10 & UC11
    UC3 -. include .-> UC2
    UC4 -. include .-> UC2
    UC8 -. extend .-> UC9
    UC10 -. include .-> UC8
```

> `UC9 Conceder crédito` é exclusivo de agentes do tipo Banco.

## 2. Diagrama de Classes

```mermaid
classDiagram
    class Usuario {
        Long id
        String login
        String senha
        Papel papel
    }
    class Cliente {
        Long id
        String rg
        String cpf
        String nome
        String endereco
        String profissao
    }
    class Rendimento {
        String entidadeEmpregadora
        BigDecimal valor
    }
    class Automovel {
        Long id
        String matricula
        Integer ano
        String marca
        String modelo
        String placa
        Proprietario proprietario
    }
    class PedidoAluguel {
        Long id
        LocalDate dataInicio
        LocalDate dataFim
        StatusPedido status
    }
    class ContratoCredito {
        Long id
        BigDecimal valor
    }
    class Papel {
        <<enumeration>>
        CLIENTE
        EMPRESA
        BANCO
    }
    class StatusPedido {
        <<enumeration>>
        PENDENTE
        APROVADO
        REJEITADO
        CONTRATADO
        CANCELADO
    }
    class Proprietario {
        <<enumeration>>
        CLIENTE
        EMPRESA
        BANCO
    }

    Usuario "1" -- "0..1" Cliente : é
    Cliente "1" *-- "0..3" Rendimento
    Cliente "1" -- "0..*" PedidoAluguel : introduz
    PedidoAluguel "0..*" --> "1" Automovel : aluga
    PedidoAluguel "1" o-- "0..1" ContratoCredito
    ContratoCredito "0..*" --> "1" Usuario : banco concedente
    Usuario --> Papel
    PedidoAluguel --> StatusPedido
    Automovel --> Proprietario
```

Ciclo de vida do pedido: `PENDENTE → APROVADO → CONTRATADO`; `PENDENTE → REJEITADO`; `CANCELADO` (cliente, antes do contrato).

## 3. Diagrama de Pacotes (Visão Lógica — MVC)

Dois subsistemas: **gestão de pedidos e contratos** (back-end Spring Boot) e **construção dinâmica das páginas web** (front-end React + Vite).

```mermaid
flowchart TB
    subgraph FE["Subsistema Web - frontend (React + Vite)"]
        pages["pages / componentes (View)"]
        api["api.js (cliente HTTP)"]
        pages --> api
    end

    subgraph BE["Subsistema de Pedidos e Contratos - backend (Spring Boot)"]
        controller["controller (C)"]
        dto["dto"]
        service["service"]
        repository["repository"]
        model["model (M)"]
        config["config (segurança)"]
        controller --> dto
        controller --> service
        service --> repository
        service --> model
        repository --> model
        config -.-> controller
    end

    DB[("Supabase - PostgreSQL")]
    api -- "REST/JSON + Basic Auth" --> controller
    repository -- JPA --> DB
```

## 4. Mapeamento modelo ↔ código

| Modelo | Código (`backend/src/main/java/com/aluguel`) |
|---|---|
| Classes de domínio | `model/*` |
| Pacotes | `controller`, `service`, `repository`, `dto`, `model`, `config` |
| UC1/UC2 | `AuthController`, `UsuarioService`, `SecurityConfig` |
| UC3 | `ClienteController`, `ClienteService` |
| UC4–UC7 | `PedidoController`, `PedidoService` |
| UC8–UC10 | `PedidoService.avaliar` / `contratar` |
| UC11 | `AutomovelController` |
