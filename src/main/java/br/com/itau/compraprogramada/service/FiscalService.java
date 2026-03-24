package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.fiscal.EventoKafka;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao.TipoOperacao;
import br.com.itau.compraprogramada.repository.EventoKafkaRepository;
import br.com.itau.compraprogramada.repository.HistoricoOperacaoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FiscalService {

    private static final BigDecimal ALIQUOTA_DEDO_DURO = new BigDecimal("0.00005");
    private static final BigDecimal ALIQUOTA_IR_VENDA = new BigDecimal("0.20");
    private static final BigDecimal LIMITE_ISENCAO = new BigDecimal("20000.00");

    @Value("${kafka.topic.ir:ir-eventos}")
    private String topicIr;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventoKafkaRepository eventoKafkaRepository;
    private final HistoricoOperacaoRepository historicoRepository;
    private final ObjectMapper objectMapper;

    /**
     * Calcula e publica IR dedo-duro (0,005%) sobre compra distribuída ao cliente.
     */
    public void publicarIRDedoDuro(Cliente cliente, String ticker, long quantidade, BigDecimal precoUnitario) {
        BigDecimal valorOperacao = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        BigDecimal valorIR = valorOperacao.multiply(ALIQUOTA_DEDO_DURO).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> mensagem = new HashMap<>();
        mensagem.put("tipo", "IR_DEDO_DURO");
        mensagem.put("clienteId", cliente.getId());
        mensagem.put("cpf", cliente.getCpf());
        mensagem.put("ticker", ticker);
        mensagem.put("tipoOperacao", "COMPRA");
        mensagem.put("quantidade", quantidade);
        mensagem.put("precoUnitario", precoUnitario);
        mensagem.put("valorOperacao", valorOperacao);
        mensagem.put("aliquota", ALIQUOTA_DEDO_DURO);
        mensagem.put("valorIR", valorIR);
        mensagem.put("dataOperacao", LocalDateTime.now().toString());

        publicar(mensagem);
    }

    /**
     * Calcula e publica IR sobre vendas do mês (regra R$ 20.000).
     * Chamado após rebalanceamento.
     */
    public void calcularEPublicarIRVenda(Cliente cliente, List<HistoricoOperacao> vendas, BigDecimal lucroLiquido) {
        if (vendas.isEmpty()) return;

        BigDecimal totalVendas = vendas.stream()
                .map(HistoricoOperacao::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalVendas.compareTo(LIMITE_ISENCAO) <= 0) {
            log.debug("Cliente {} isento de IR (total vendas: R$ {})", cliente.getId(), totalVendas);
            return;
        }

        // Publica mesmo com IR zero: a notificação é obrigatória para o sistema fiscal
        // sempre que as vendas superam o limite de isenção, independentemente de haver lucro.
        BigDecimal valorIR = lucroLiquido.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : lucroLiquido.multiply(ALIQUOTA_IR_VENDA).setScale(2, RoundingMode.HALF_UP);

        YearMonth mesRef = YearMonth.from(LocalDate.now());

        Map<String, Object> mensagem = new HashMap<>();
        mensagem.put("tipo", "IR_VENDA");
        mensagem.put("clienteId", cliente.getId());
        mensagem.put("cpf", cliente.getCpf());
        mensagem.put("mesReferencia", mesRef.toString());
        mensagem.put("totalVendasMes", totalVendas);
        mensagem.put("lucroLiquido", lucroLiquido);
        mensagem.put("aliquota", ALIQUOTA_IR_VENDA);
        mensagem.put("valorIR", valorIR);
        mensagem.put("dataCalculo", LocalDateTime.now().toString());

        publicar(mensagem);
    }

    /**
     * Calcula IR venda usando preço médio da custódia.
     */
    public BigDecimal calcularIRVenda(BigDecimal totalVendas, BigDecimal lucroLiquido) {
        if (totalVendas.compareTo(LIMITE_ISENCAO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (lucroLiquido.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return lucroLiquido.multiply(ALIQUOTA_IR_VENDA).setScale(2, RoundingMode.HALF_UP);
    }

    private void publicar(Map<String, Object> mensagem) {
        try {
            String payload = objectMapper.writeValueAsString(mensagem);
            kafkaTemplate.send(topicIr, payload);
            salvarEventoKafka(mensagem.get("tipo").toString(), payload, EventoKafka.StatusEvento.ENVIADO);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento Kafka: {}. Salvando para retry.", e.getMessage());
            try {
                String payload = objectMapper.writeValueAsString(mensagem);
                salvarEventoKafka(mensagem.get("tipo").toString(), payload, EventoKafka.StatusEvento.PENDENTE);
            } catch (JsonProcessingException ex) {
                log.error("Erro ao serializar evento Kafka", ex);
            }
        }
    }

    private void salvarEventoKafka(String tipo, String payload, EventoKafka.StatusEvento status) {
        EventoKafka evento = new EventoKafka(tipo, payload);
        evento.setStatus(status);
        if (status == EventoKafka.StatusEvento.ENVIADO) {
            evento.setEnviadoEm(LocalDateTime.now());
        }
        eventoKafkaRepository.save(evento);
    }
}
