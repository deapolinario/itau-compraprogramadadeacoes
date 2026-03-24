package br.com.itau.compraprogramada.domain.cesta;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cestas_recomendacao")
@Getter
@Setter
@NoArgsConstructor
public class CestaRecomendacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_desativacao")
    private LocalDateTime dataDesativacao;

    @Column(nullable = false)
    private Boolean ativo = true;

    @OneToMany(mappedBy = "cesta", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ItemCesta> itens = new ArrayList<>();

    public void desativar() {
        this.ativo = false;
        this.dataDesativacao = LocalDateTime.now();
    }
}
