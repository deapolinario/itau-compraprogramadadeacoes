package br.com.itau.compraprogramada.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CarteiraPosicaoResponse {

    private Long clienteId;
    private BigDecimal valorInvestidoTotal;
    private BigDecimal valorAtualTotal;
    private BigDecimal plTotal;
    private BigDecimal rentabilidadePercentual;
    private List<AtivoResponse> ativos;

    @Data
    @Builder
    public static class AtivoResponse {
        private String ticker;
        private Long quantidade;
        private BigDecimal precoMedio;
        private BigDecimal cotacaoAtual;
        private BigDecimal valorAtual;
        private BigDecimal pl;
        private BigDecimal composicaoPercentual;
    }
}
