package br.com.itau.compraprogramada.repository;

import br.com.itau.compraprogramada.domain.fiscal.EventoKafka;
import br.com.itau.compraprogramada.domain.fiscal.EventoKafka.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoKafkaRepository extends JpaRepository<EventoKafka, Long> {
    List<EventoKafka> findAllByStatus(StatusEvento status);
}
