package br.com.itau.compraprogramada.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlterarValorMensalRequest {

    @NotNull(message = "Valor mensal é obrigatório")
    @DecimalMin(value = "100.00", message = "Valor mensal mínimo é R$ 100,00")
    private BigDecimal valorMensal;
}
