package br.com.itau.compraprogramada.repository;

import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao.TipoOperacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoricoOperacaoRepository extends JpaRepository<HistoricoOperacao, Long> {

    @Query("SELECT h FROM HistoricoOperacao h WHERE h.cliente.id = :clienteId " +
           "AND h.tipo = :tipo AND h.dataOperacao >= :inicio AND h.dataOperacao < :fim")
    List<HistoricoOperacao> findByClienteIdAndTipoAndPeriodo(
            Long clienteId, TipoOperacao tipo, LocalDateTime inicio, LocalDateTime fim);
}
