package br.com.itau.compraprogramada.domain.fiscal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_kafka")
@Getter
@Setter
@NoArgsConstructor
public class EventoKafka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusEvento status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    public enum StatusEvento {
        PENDENTE, ENVIADO, ERRO
    }

    public EventoKafka(String tipo, String payload) {
        this.tipo = tipo;
        this.payload = payload;
        this.status = StatusEvento.PENDENTE;
    }
}
