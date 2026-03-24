package br.com.itau.compraprogramada.dto;

import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import br.com.itau.compraprogramada.domain.cesta.ItemCesta;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CestaResponse {

    private Long id;
    private LocalDateTime dataCriacao;
    private Boolean ativo;
    private List<ItemResponse> itens;

    public static CestaResponse of(CestaRecomendacao cesta) {
        CestaResponse r = new CestaResponse();
        r.id = cesta.getId();
        r.dataCriacao = cesta.getDataCriacao();
        r.ativo = cesta.getAtivo();
        r.itens = cesta.getItens().stream()
                .map(ItemResponse::of)
                .toList();
        return r;
    }

    @Data
    public static class ItemResponse {
        private String ticker;
        private BigDecimal percentual;

        public static ItemResponse of(ItemCesta item) {
            ItemResponse r = new ItemResponse();
            r.ticker = item.getTicker();
            r.percentual = item.getPercentual();
            return r;
        }
    }
}
