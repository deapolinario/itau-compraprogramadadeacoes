package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.domain.custodia.Custodia;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class PrecoMedioService {

    private static final int ESCALA_PM = 6;
    private static final RoundingMode ARREDONDAMENTO = RoundingMode.DOWN;

    /**
     * Recalcula o preço médio após uma compra.
     * PM = (Qtd_Ant × PM_Ant + Qtd_Nova × Preco_Nova) / (Qtd_Ant + Qtd_Nova)
     */
    public BigDecimal calcular(Custodia custodia, long qtdNova, BigDecimal precoNovo) {
        long qtdAnterior = custodia.getQuantidade();
        BigDecimal pmAnterior = custodia.getPrecoMedio();

        if (qtdAnterior == 0) {
            return precoNovo.setScale(ESCALA_PM, ARREDONDAMENTO);
        }

        BigDecimal valorAnterior = pmAnterior.multiply(BigDecimal.valueOf(qtdAnterior));
        BigDecimal valorNovo = precoNovo.multiply(BigDecimal.valueOf(qtdNova));
        long qtdTotal = qtdAnterior + qtdNova;

        return valorAnterior.add(valorNovo)
                .divide(BigDecimal.valueOf(qtdTotal), ESCALA_PM, ARREDONDAMENTO);
    }
}
