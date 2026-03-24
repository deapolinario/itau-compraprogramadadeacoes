package br.com.itau.compraprogramada.domain.motor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "execucoes_motor")
@Getter
@Setter
@NoArgsConstructor
public class ExecucaoMotor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_referencia", nullable = false, unique = true)
    private LocalDate dataReferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusExecucao status;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    public enum StatusExecucao {
        PENDENTE, EM_EXECUCAO, CONCLUIDO, ERRO
    }

    public ExecucaoMotor(LocalDate dataReferencia) {
        this.dataReferencia = dataReferencia;
        this.status = StatusExecucao.PENDENTE;
        this.iniciadoEm = LocalDateTime.now();
    }
}
