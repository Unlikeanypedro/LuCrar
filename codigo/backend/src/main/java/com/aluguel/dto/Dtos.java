package com.aluguel.dto;

import com.aluguel.model.Automovel;
import com.aluguel.model.Papel;
import com.aluguel.model.Rendimento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTOs de entrada da API. */
public final class Dtos {

    private Dtos() {}

    public record ClienteDTO(
        @NotBlank String nome,
        String rg,
        @NotBlank String cpf,
        @NotBlank String endereco,
        @NotBlank String profissao,
        @Size(max = 3) List<@Valid Rendimento> rendimentos
    ) {}

    public record AgenteDTO(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Pattern(regexp = "[0-9]{14}", message = "CNPJ deve conter 14 números") String cnpj
    ) {}

    /** Cadastro: bloco cliente ou agente é obrigatório conforme o tipo da conta. */
    public record RegistroDTO(
        @NotBlank @Size(max = 120) String login,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @NotNull Papel papel,
        @Valid ClienteDTO cliente,
        @Size(max = 256) String codigoAgente,
        @Valid AgenteDTO agente
    ) {}

    public record PedidoDTO(
        @NotNull Long automovelId,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim
    ) {}

    public record AvaliacaoDTO(
        @NotNull Boolean aprovado,
        @DecimalMin("0.01") BigDecimal valorCredito,
        @NotBlank @Size(max = 1000) String parecerFinanceiro
    ) {}

    public record AutomovelDTO(
        @NotNull @Min(1886) Integer ano,
        @NotBlank String marca,
        @NotBlank String modelo,
        @NotBlank String placa,
        @NotNull @DecimalMin("0.01") BigDecimal valorDiaria,
        Automovel.Proprietario proprietario,
        Long proprietarioId
    ) {}

    public record AutomovelRespostaDTO(
        Long id,
        String matricula,
        Integer ano,
        String marca,
        String modelo,
        String placa,
        BigDecimal valorDiaria,
        Automovel.Proprietario proprietario,
        Long proprietarioId
    ) {}
}
