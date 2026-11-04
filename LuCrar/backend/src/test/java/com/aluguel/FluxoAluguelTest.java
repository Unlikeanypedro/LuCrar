package com.aluguel;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FluxoAluguelTest {

    @Autowired
    MockMvc mvc;

    private void registrarCliente(String login, String senha) throws Exception {
        mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"login":"%s","senha":"%s","papel":"CLIENTE",
                     "cliente":{"nome":"Teste","rg":"123","cpf":"456",
                                  "endereco":"Rua A","profissao":"Dev",
                                  "rendimentos":[{"entidadeEmpregadora":"ACME","valor":5000}]}}
                    """.formatted(login, senha)
                )
        ).andExpect(status().isCreated());
    }

    private void registrarAgente(String login, String senha, String papel)
        throws Exception {
        mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"login":"%s","senha":"%s","papel":"%s","codigoAgente":"test-key"}
                    """.formatted(login, senha, papel)
                )
        ).andExpect(status().isCreated());
    }

    @Test
    void fluxoCompleto() throws Exception {
        // sem login -> 401
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());

        registrarCliente("ana", "senha123");
        registrarAgente("banco", "senha123", "BANCO");

        mvc.perform(get("/api/auth/me").with(httpBasic("ana", "senha123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.papel").value("CLIENTE"));

        // cliente cria pedido
        mvc.perform(
            post("/api/pedidos")
                .with(httpBasic("ana", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"automovelId\":1,\"dataInicio\":\"2026-12-01\",\"dataFim\":\"2026-12-10\"}"
                )
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDENTE"));

        // cliente não avalia
        mvc.perform(
            post("/api/pedidos/1/avaliacao")
                .with(httpBasic("ana", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\":true}")
        ).andExpect(status().isForbidden());

        // banco avalia com crédito e contrata
        mvc.perform(
            post("/api/pedidos/1/avaliacao")
                .with(httpBasic("banco", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\":true,\"valorCredito\":9000}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APROVADO"))
            .andExpect(jsonPath("$.credito.valor").value(9000));
        mvc.perform(
            post("/api/pedidos/1/contrato").with(httpBasic("banco", "senha123"))
        ).andExpect(jsonPath("$.status").value("CONTRATADO"));

        // senha nunca é exposta
        mvc.perform(
            get("/api/pedidos/1").with(httpBasic("ana", "senha123"))
        ).andExpect(jsonPath("$.credito.banco.senha").doesNotExist());
    }

    @Test
    void agenteSemChaveNaoRegistra() throws Exception {
        mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"login\":\"empresa\",\"senha\":\"senha123\",\"papel\":\"EMPRESA\"}"
                )
        ).andExpect(status().isForbidden());
    }

    @Test
    void clienteNaoCancelaContratado() throws Exception {
        registrarCliente("cli", "senha123");
        registrarAgente("banco2", "senha123", "BANCO");

        mvc.perform(
            post("/api/pedidos")
                .with(httpBasic("cli", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"automovelId\":1,\"dataInicio\":\"2026-12-01\",\"dataFim\":\"2026-12-10\"}"
                )
        ).andExpect(status().isCreated());

        mvc.perform(
            post("/api/pedidos/1/avaliacao")
                .with(httpBasic("banco2", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\":true,\"valorCredito\":1000}")
        ).andExpect(status().isOk());
        mvc.perform(
            post("/api/pedidos/1/contrato").with(
                httpBasic("banco2", "senha123")
            )
        ).andExpect(status().isOk());

        mvc.perform(
            delete("/api/pedidos/1").with(httpBasic("cli", "senha123"))
        ).andExpect(status().isConflict());
    }

    @Test
    void healthEndpointPublico() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void rendimentoLimiteTres() throws Exception {
        mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"login":"cli2","senha":"senha123","papel":"CLIENTE",
                     "cliente":{"nome":"Teste","rg":"1","cpf":"2",
                                  "endereco":"Rua","profissao":"Dev",
                                  "rendimentos":[
                                      {"entidadeEmpregadora":"A","valor":1000},
                                      {"entidadeEmpregadora":"B","valor":2000},
                                      {"entidadeEmpregadora":"C","valor":3000},
                                      {"entidadeEmpregadora":"D","valor":4000}
                                  ]}}
                    """
                )
        ).andExpect(status().isBadRequest());
    }

    @Test
    void dataFimDeveSerPosteriorAInicio() throws Exception {
        registrarCliente("cli3", "senha123");
        mvc.perform(
            post("/api/pedidos")
                .with(httpBasic("cli3", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"automovelId\":1,\"dataInicio\":\"2026-12-10\",\"dataFim\":\"2026-12-01\"}"
                )
        ).andExpect(status().isBadRequest());
    }

    @Test
    void agenteNaoClienteModificaPedidoAprovado() throws Exception {
        registrarCliente("cli4", "senha123");
        registrarAgente("emp", "senha123", "EMPRESA");

        mvc.perform(
            post("/api/pedidos")
                .with(httpBasic("cli4", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"automovelId\":1,\"dataInicio\":\"2026-12-01\",\"dataFim\":\"2026-12-10\"}"
                )
        ).andExpect(status().isCreated());

        mvc.perform(
            post("/api/pedidos/1/avaliacao")
                .with(httpBasic("emp", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\":true}")
        ).andExpect(status().isOk());

        // agente pode modificar pedido aprovado (volta a PENDENTE)
        mvc.perform(
            put("/api/pedidos/1")
                .with(httpBasic("emp", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"automovelId\":1,\"dataInicio\":\"2026-12-05\",\"dataFim\":\"2026-12-15\"}"
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void creditoSomenteBancoAoAprovar() throws Exception {
        registrarCliente("cli5", "senha123");
        registrarAgente("emp2", "senha123", "EMPRESA");

        mvc.perform(
            post("/api/pedidos")
                .with(httpBasic("cli5", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"automovelId\":1,\"dataInicio\":\"2026-12-01\",\"dataFim\":\"2026-12-10\"}"
                )
        ).andExpect(status().isCreated());

        // empresa tentando dar crédito ao aprovar -> 400
        mvc.perform(
            post("/api/pedidos/1/avaliacao")
                .with(httpBasic("emp2", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\":true,\"valorCredito\":5000}")
        ).andExpect(status().isBadRequest());
    }
}
