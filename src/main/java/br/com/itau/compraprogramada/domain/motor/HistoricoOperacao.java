package br.com.itau.compraprogramada.domain.motor;

import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_operacoes")
@Getter
@Setter
@NoArgsConstructor
public class HistoricoOperacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id", nullable = false)
    private ContaGrafica conta;

    @Column(nullable = false, length = 12)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoOperacao tipo;

    @Column(nullable = false)
    private Long quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 18, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "valor_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_operacao", nullable = false)
    private LocalDateTime dataOperacao = LocalDateTime.now();

    public enum TipoOperacao {
        COMPRA, VENDA
    }

    public HistoricoOperacao(Cliente cliente, ContaGrafica conta, String ticker,
                              TipoOperacao tipo, Long quantidade, BigDecimal precoUnitario) {
        this.cliente = cliente;
        this.conta = conta;
        this.ticker = ticker;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.valorTotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
