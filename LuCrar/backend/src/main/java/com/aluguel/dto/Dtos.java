package com.aluguel.dto;

import com.aluguel.model.Papel;
import com.aluguel.model.Rendimento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTOs de entrada da API. */
public final class Dtos {
    private Dtos() {}

    public record ClienteDTO(@NotBlank String nome, @NotBlank String rg, @NotBlank String cpf,
                             String endereco, String profissao,
                             @Size(max = 3) List<Rendimento> rendimentos) {}

    /** Cadastro: o bloco "cliente" é obrigatório apenas para o papel CLIENTE. */
    public record RegistroDTO(@NotBlank String login, @NotBlank String senha, @NotNull Papel papel,
                              @Valid ClienteDTO cliente) {}

    public record PedidoDTO(@NotNull Long automovelId, @NotNull LocalDate dataInicio,
                            @NotNull LocalDate dataFim) {}

    public record AvaliacaoDTO(boolean aprovado, BigDecimal valorCredito) {}
}
