package br.com.itau.compraprogramada.domain.custodia;

import br.com.itau.compraprogramada.domain.cliente.Cliente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contas_graficas")
@Getter
@Setter
@NoArgsConstructor
public class ContaGrafica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "numero_conta", nullable = false, unique = true, length = 20)
    private String numeroConta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoConta tipo;

    public enum TipoConta {
        MASTER, FILHOTE
    }

    public ContaGrafica(Cliente cliente, String numeroConta, TipoConta tipo) {
        this.cliente = cliente;
        this.numeroConta = numeroConta;
        this.tipo = tipo;
    }

    public ContaGrafica(String numeroConta, TipoConta tipo) {
        this.numeroConta = numeroConta;
        this.tipo = tipo;
    }
}
