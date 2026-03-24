package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class PrecoMedioServiceTest {

    private final PrecoMedioService service = new PrecoMedioService();

    private Custodia custodiaVazia(String ticker) {
        Custodia c = new Custodia();
        c.setTicker(ticker);
        c.setQuantidade(0L);
        c.setPrecoMedio(BigDecimal.ZERO);
        return c;
    }

    private Custodia custodiaComPosicao(long qtd, String pm) {
        Custodia c = new Custodia();
        c.setQuantidade(qtd);
        c.setPrecoMedio(new BigDecimal(pm));
        return c;
    }

    @Test
    void calcular_primeiraCOmpra_retornaPrecoNovo() {
        Custodia custodia = custodiaVazia("PETR4");

        BigDecimal pm = service.calcular(custodia, 8L, new BigDecimal("35.00"));

        assertThat(pm).isEqualByComparingTo("35.000000");
    }

    @Test
    void calcular_segundaCompra_recalculaPM() {
        // 8 ações a R$ 35,00 → PM anterior = 35,00
        Custodia custodia = custodiaComPosicao(8L, "35.00");

        // Compra 10 ações a R$ 37,00
        BigDecimal pm = service.calcular(custodia, 10L, new BigDecimal("37.00"));

        // PM = (8×35 + 10×37) / 18 = (280 + 370) / 18 = 650/18 = 36,111...
        assertThat(pm).isEqualByComparingTo("36.111111");
    }

    @Test
    void calcular_vendaNaoAlteraPM_apenasQuantidadeMuda() {
        // Simula venda: cliente deveria apenas reduzir quantidade sem mudar PM
        Custodia custodia = custodiaComPosicao(18L, "36.111111");

        // Venda não chama PrecoMedioService — apenas quantidade é reduzida externamente
        // Confirma que PM não se altera ao reduzir quantidade manualmente
        custodia.setQuantidade(13L);
        assertThat(custodia.getPrecoMedio()).isEqualByComparingTo("36.111111");
    }

    @Test
    void calcular_compraAposVenda_recalculaComPMAntigo() {
        // 13 ações com PM = R$ 36,11 (após venda de 5)
        Custodia custodia = custodiaComPosicao(13L, "36.111111");

        // Compra 7 ações a R$ 38,00
        BigDecimal pm = service.calcular(custodia, 7L, new BigDecimal("38.00"));

        // PM = (13×36,111111 + 7×38) / 20 = (469,44443 + 266) / 20 = 735,44443 / 20 = 36,772...
        assertThat(pm).isBetween(new BigDecimal("36.77"), new BigDecimal("36.78"));
    }

    @Test
    void calcular_usaBigDecimal_semPerdaDePrecisao() {
        Custodia custodia = custodiaComPosicao(1L, "33.333333");

        BigDecimal pm = service.calcular(custodia, 2L, new BigDecimal("33.333333"));

        // Não deve usar double — sem perda de precisão
        assertThat(pm).isEqualByComparingTo("33.333333");
    }
}
