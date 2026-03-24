package br.com.itau.compraprogramada.domain.custodia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "custodias", uniqueConstraints = @UniqueConstraint(columnNames = {"conta_id", "ticker"}))
@Getter
@Setter
@NoArgsConstructor
public class Custodia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id", nullable = false)
    private ContaGrafica conta;

    @Column(nullable = false, length = 12)
    private String ticker;

    @Column(nullable = false)
    private Long quantidade = 0L;

    @Column(name = "preco_medio", nullable = false, precision = 18, scale = 6)
    private BigDecimal precoMedio = BigDecimal.ZERO;

    public Custodia(ContaGrafica conta, String ticker) {
        this.conta = conta;
        this.ticker = ticker;
    }
}
