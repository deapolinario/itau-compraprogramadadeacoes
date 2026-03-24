package br.com.itau.compraprogramada.domain.cesta;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_cesta", uniqueConstraints = @UniqueConstraint(columnNames = {"cesta_id", "ticker"}))
@Getter
@Setter
@NoArgsConstructor
public class ItemCesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cesta_id", nullable = false)
    private CestaRecomendacao cesta;

    @Column(nullable = false, length = 12)
    private String ticker;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual;

    public ItemCesta(CestaRecomendacao cesta, String ticker, BigDecimal percentual) {
        this.cesta = cesta;
        this.ticker = ticker;
        this.percentual = percentual;
    }
}
