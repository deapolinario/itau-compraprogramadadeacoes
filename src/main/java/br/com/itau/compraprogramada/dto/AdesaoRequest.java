package br.com.itau.compraprogramada.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdesaoRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos numéricos")
    private String cpf;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotNull(message = "Valor mensal é obrigatório")
    @DecimalMin(value = "100.00", message = "Valor mensal mínimo é R$ 100,00")
    private BigDecimal valorMensal;
}
