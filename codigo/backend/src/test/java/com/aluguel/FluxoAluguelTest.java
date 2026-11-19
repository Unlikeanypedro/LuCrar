package com.aluguel;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fluxo ponta a ponta (HU01–HU12) com H2 em memória.
 *
 * O contexto e o banco são compartilhados entre os testes, então cada teste usa
 * o próprio período e o id devolvido pela API em vez de valores fixos.
 */
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
                    {"login":"%s","senha":"%s","papel":"%s","codigoAgente":"test-key",
                     "agente":{"nome":"Agente Teste","cnpj":"12345678000190"}}
                    """.formatted(login, senha, papel)
                )
        ).andExpect(status().isCreated());
    }

    /** Cria um pedido para o automóvel semeado e devolve o id gerado. */
    private long criarPedido(
        String login,
        String senha,
        String dataInicio,
        String dataFim
    ) throws Exception {
        var resposta = mvc.perform(
            post("/api/pedidos")
                .with(httpBasic(login, senha))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"automovelId\":1,\"dataInicio\":\"%s\",\"dataFim\":\"%s\"}".formatted(
                        dataInicio,
                        dataFim
                    )
                )
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDENTE"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String id = JsonPath.read(resposta, "$.id").toString();
        return Long.parseLong(id);
    }

    private void avaliar(
        String login,
        String senha,
        long pedidoId,
        String corpo
    ) throws Exception {
        mvc.perform(
            post("/api/pedidos/" + pedidoId + "/avaliacao")
                .with(httpBasic(login, senha))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo)
        ).andExpect(status().isOk());
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

        // o catálogo semeado tem diária e permite o pedido (HU04)
        long pedidoId = criarPedido(
            "ana",
            "senha123",
            "2027-01-05",
            "2027-01-10"
        );

        // cliente não avalia
        mvc.perform(
            post("/api/pedidos/" + pedidoId + "/avaliacao")
                .with(httpBasic("ana", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\":true,\"parecerFinanceiro\":\"Renda compatível.\"}")
        ).andExpect(status().isForbidden());

        // banco avalia com crédito e contrata
        avaliar(
            "banco",
            "senha123",
            pedidoId,
            "{\"aprovado\":true,\"valorCredito\":9000,\"parecerFinanceiro\":\"Renda compatível.\"}"
        );
        mvc.perform(
            get("/api/pedidos/" + pedidoId).with(httpBasic("banco", "senha123"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APROVADO"))
            .andExpect(jsonPath("$.valorCreditoAprovado").value(9000));

        mvc.perform(
            post("/api/pedidos/" + pedidoId + "/contrato").with(
                httpBasic("banco", "senha123")
            )
        )
            .andExpect(jsonPath("$.status").value("CONTRATADO"))
            .andExpect(jsonPath("$.credito.valorFinanciado").value(9000))
            .andExpect(jsonPath("$.contrato.status").value("ATIVO"));

        // senha nunca é exposta
        mvc.perform(
            get("/api/pedidos/" + pedidoId).with(httpBasic("ana", "senha123"))
        ).andExpect(jsonPath("$.credito.banco.usuario.senha").doesNotExist());
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

        long pedidoId = criarPedido(
            "cli",
            "senha123",
            "2027-02-01",
            "2027-02-05"
        );

        avaliar(
            "banco2",
            "senha123",
            pedidoId,
            "{\"aprovado\":true,\"valorCredito\":1000,\"parecerFinanceiro\":\"Renda compatível.\"}"
        );
        mvc.perform(
            post("/api/pedidos/" + pedidoId + "/contrato").with(
                httpBasic("banco2", "senha123")
            )
        ).andExpect(status().isOk());

        mvc.perform(
            delete("/api/pedidos/" + pedidoId).with(httpBasic("cli", "senha123"))
        ).andExpect(status().isConflict());
    }

    @Test
    void healthEndpointPublico() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void rgEhOpcionalNoCadastroDoCliente() throws Exception {
        mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"login":"sem-rg","senha":"senha123","papel":"CLIENTE",
                     "cliente":{"nome":"Teste","cpf":"456",
                                  "endereco":"Rua A","profissao":"Dev"}}
                    """
                )
        ).andExpect(status().isCreated());
        mvc.perform(get("/api/auth/me").with(httpBasic("sem-rg", "senha123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.papel").value("CLIENTE"));
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
                    "{\"automovelId\":1,\"dataInicio\":\"2027-06-10\",\"dataFim\":\"2027-06-01\"}"
                )
        ).andExpect(status().isBadRequest());
    }

    @Test
    void agenteNaoClienteModificaPedidoAprovado() throws Exception {
        registrarCliente("cli4", "senha123");
        registrarAgente("emp", "senha123", "EMPRESA");

        long pedidoId = criarPedido(
            "cli4",
            "senha123",
            "2027-03-02",
            "2027-03-08"
        );

        avaliar(
            "emp",
            "senha123",
            pedidoId,
            "{\"aprovado\":true,\"parecerFinanceiro\":\"Renda compatível.\"}"
        );

        // agente pode modificar pedido aprovado (volta a PENDENTE)
        mvc.perform(
            put("/api/pedidos/" + pedidoId)
                .with(httpBasic("emp", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"automovelId\":1,\"dataInicio\":\"2027-03-10\",\"dataFim\":\"2027-03-15\"}"
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void creditoSomenteBancoAoAprovar() throws Exception {
        registrarCliente("cli5", "senha123");
        registrarAgente("emp2", "senha123", "EMPRESA");

        long pedidoId = criarPedido(
            "cli5",
            "senha123",
            "2027-04-01",
            "2027-04-05"
        );

        // empresa tentando dar crédito ao aprovar -> 400
        mvc.perform(
            post("/api/pedidos/" + pedidoId + "/avaliacao")
                .with(httpBasic("emp2", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"aprovado\":true,\"valorCredito\":5000,\"parecerFinanceiro\":\"Renda compatível.\"}"
                )
        ).andExpect(status().isBadRequest());
    }

    @Test
    void parecerNegativoRejeitaPedido() throws Exception {
        registrarCliente("cli6", "senha123");
        registrarAgente("emp3", "senha123", "EMPRESA");

        long pedidoId = criarPedido(
            "cli6",
            "senha123",
            "2027-05-01",
            "2027-05-05"
        );

        mvc.perform(
            post("/api/pedidos/" + pedidoId + "/avaliacao")
                .with(httpBasic("emp3", "senha123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"aprovado\":false,\"parecerFinanceiro\":\"Renda insuficiente.\"}"
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJEITADO"));
    }

    @Test
    void agenteConsultaPerfilFinanceiroDoCliente() throws Exception {
        registrarCliente("cli7", "senha123");
        registrarAgente("emp4", "senha123", "EMPRESA");

        mvc.perform(
            get("/api/clientes").with(httpBasic("emp4", "senha123"))
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$[?(@.nome == 'Teste')].rendimentos[0].entidadeEmpregadora")
                    .value(org.hamcrest.Matchers.hasItem("ACME"))
            );
    }
}
