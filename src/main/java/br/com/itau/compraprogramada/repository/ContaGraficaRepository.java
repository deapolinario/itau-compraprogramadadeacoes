package br.com.itau.compraprogramada.repository;

import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica.TipoConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContaGraficaRepository extends JpaRepository<ContaGrafica, Long> {
    Optional<ContaGrafica> findByClienteIdAndTipo(Long clienteId, TipoConta tipo);
    Optional<ContaGrafica> findByTipo(TipoConta tipo);
}
