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
    @Autowired MockMvc mvc;

    private void registrar(String json) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void fluxoCompleto() throws Exception {
        // sem login -> 401
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());

        registrar("""
            {"login":"ana","senha":"123","papel":"CLIENTE","cliente":{"nome":"Ana","rg":"1","cpf":"2",
             "rendimentos":[{"entidadeEmpregadora":"ACME","valor":5000}]}}""");
        registrar("{\"login\":\"banco\",\"senha\":\"123\",\"papel\":\"BANCO\"}");

        mvc.perform(get("/api/auth/me").with(httpBasic("ana", "123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.papel").value("CLIENTE"));

        // cliente cria pedido
        mvc.perform(post("/api/pedidos").with(httpBasic("ana", "123")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"automovelId\":1,\"dataInicio\":\"2026-12-01\",\"dataFim\":\"2026-12-10\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"));

        // cliente não avalia
        mvc.perform(post("/api/pedidos/1/avaliacao").with(httpBasic("ana", "123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aprovado\":true}"))
                .andExpect(status().isForbidden());

        // banco avalia com crédito e contrata
        mvc.perform(post("/api/pedidos/1/avaliacao").with(httpBasic("banco", "123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aprovado\":true,\"valorCredito\":9000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.credito.valor").value(9000));
        mvc.perform(post("/api/pedidos/1/contrato").with(httpBasic("banco", "123")))
                .andExpect(jsonPath("$.status").value("CONTRATADO"));

        // senha nunca é exposta
        mvc.perform(get("/api/pedidos/1").with(httpBasic("ana", "123")))
                .andExpect(jsonPath("$.credito.banco.senha").doesNotExist());
    }
}
