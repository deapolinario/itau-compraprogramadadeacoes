package br.com.itau.compraprogramada.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CestaRequest {

    @NotEmpty(message = "A cesta deve conter ativos")
    @Size(min = 5, max = 5, message = "A cesta deve conter exatamente 5 ações")
    @Valid
    private List<ItemCestaRequest> itens;

    @Data
    public static class ItemCestaRequest {
        @jakarta.validation.constraints.NotBlank(message = "Ticker é obrigatório")
        private String ticker;

        @jakarta.validation.constraints.NotNull(message = "Percentual é obrigatório")
        @jakarta.validation.constraints.DecimalMin(value = "0.01", message = "Percentual deve ser maior que 0")
        private java.math.BigDecimal percentual;
    }
}
