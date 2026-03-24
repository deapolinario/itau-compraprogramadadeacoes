package br.com.itau.compraprogramada.domain.cliente;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, length = 11, unique = true)
    private String cpf;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(name = "valor_mensal", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorMensal;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_adesao", nullable = false)
    private LocalDateTime dataAdesao = LocalDateTime.now();

    public Cliente(String nome, String cpf, String email, BigDecimal valorMensal) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.valorMensal = valorMensal;
    }
}
