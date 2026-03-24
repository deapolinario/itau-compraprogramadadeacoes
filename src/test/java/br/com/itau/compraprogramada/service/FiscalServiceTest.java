package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.fiscal.EventoKafka;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao;
import br.com.itau.compraprogramada.repository.EventoKafkaRepository;
import br.com.itau.compraprogramada.repository.HistoricoOperacaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiscalServiceTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private EventoKafkaRepository eventoKafkaRepository;
    @Mock private HistoricoOperacaoRepository historicoRepository;

    private FiscalService fiscalService;
    private Cliente cliente;

    @BeforeEach
    void setup() {
        fiscalService = new FiscalService(kafkaTemplate, eventoKafkaRepository, historicoRepository, new ObjectMapper());
        cliente = new Cliente("João", "12345678901", "j@e.com", BigDecimal.valueOf(3000));
        cliente.setId(1L);
        lenient().when(eventoKafkaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void calcularIRVenda_abaixoLimite_retornaZero() {
        BigDecimal ir = fiscalService.calcularIRVenda(
                new BigDecimal("230.00"),
                new BigDecimal("50.00")
        );
        assertThat(ir).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calcularIRVenda_acimaLimiteComLucro_retorna20Porcento() {
        BigDecimal ir = fiscalService.calcularIRVenda(
                new BigDecimal("21500.00"),
                new BigDecimal("3100.00")
        );
        assertThat(ir).isEqualByComparingTo("620.00");
    }

    @Test
    void calcularIRVenda_acimaLimiteComPrejuizo_retornaZero() {
        BigDecimal ir = fiscalService.calcularIRVenda(
                new BigDecimal("24400.00"),
                new BigDecimal("-600.00")
        );
        assertThat(ir).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calcularIRVenda_exatamente20k_isento() {
        BigDecimal ir = fiscalService.calcularIRVenda(
                new BigDecimal("20000.00"),
                new BigDecimal("5000.00")
        );
        assertThat(ir).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void publicarIRDedoDuro_publicaEventoKafkaCorretamente() {
        when(kafkaTemplate.send(any(), any())).thenReturn(null);

        fiscalService.publicarIRDedoDuro(cliente, "PETR4", 10L, new BigDecimal("35.00"));

        ArgumentCaptor<EventoKafka> captor = ArgumentCaptor.forClass(EventoKafka.class);
        verify(eventoKafkaRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventoKafka.StatusEvento.ENVIADO);
    }

    @Test
    void publicarIRDedoDuro_falhaKafka_salvaPendente() {
        when(kafkaTemplate.send(any(), any())).thenThrow(new RuntimeException("Kafka down"));

        fiscalService.publicarIRDedoDuro(cliente, "PETR4", 10L, new BigDecimal("35.00"));

        ArgumentCaptor<EventoKafka> captor = ArgumentCaptor.forClass(EventoKafka.class);
        verify(eventoKafkaRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventoKafka.StatusEvento.PENDENTE);
    }

    @Test
    void calcularEPublicarIRVenda_listaVazia_naoFazNada() {
        fiscalService.calcularEPublicarIRVenda(cliente, List.of(), BigDecimal.ZERO);

        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void calcularEPublicarIRVenda_totalAbaixo20k_semIR() {
        ContaGrafica conta = new ContaGrafica(cliente, "FILHOTE-001", ContaGrafica.TipoConta.FILHOTE);
        HistoricoOperacao venda = new HistoricoOperacao(cliente, conta, "PETR4",
                HistoricoOperacao.TipoOperacao.VENDA, 10L, new BigDecimal("150.00"));

        fiscalService.calcularEPublicarIRVenda(cliente, List.of(venda), new BigDecimal("50.00"));

        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void calcularEPublicarIRVenda_totalAcima20k_publicaIRComValorCorreto() throws Exception {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        when(kafkaTemplate.send(any(), payloadCaptor.capture())).thenReturn(null);

        ContaGrafica conta = new ContaGrafica(cliente, "FILHOTE-001", ContaGrafica.TipoConta.FILHOTE);
        HistoricoOperacao venda = new HistoricoOperacao(cliente, conta, "PETR4",
                HistoricoOperacao.TipoOperacao.VENDA, 100L, new BigDecimal("250.00"));

        // lucroLiquido = R$ 5.000 → IR esperado = 5000 * 20% = R$ 1.000
        fiscalService.calcularEPublicarIRVenda(cliente, List.of(venda), new BigDecimal("5000.00"));

        String payload = (String) payloadCaptor.getValue();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> mensagem = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(payload, java.util.Map.class);
        assertThat(new BigDecimal(mensagem.get("valorIR").toString()))
                .isEqualByComparingTo("1000.00");
        assertThat(mensagem.get("tipo")).isEqualTo("IR_VENDA");
    }

    @Test
    void calcularEPublicarIRVenda_totalAcima20k_prejuizo_naoPublicaIR() {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        when(kafkaTemplate.send(any(), payloadCaptor.capture())).thenReturn(null);

        ContaGrafica conta = new ContaGrafica(cliente, "FILHOTE-001", ContaGrafica.TipoConta.FILHOTE);
        HistoricoOperacao venda = new HistoricoOperacao(cliente, conta, "PETR4",
                HistoricoOperacao.TipoOperacao.VENDA, 100L, new BigDecimal("250.00"));

        // Acima de 20k em vendas com prejuízo: publica evento com IR zero
        fiscalService.calcularEPublicarIRVenda(cliente, List.of(venda), new BigDecimal("-800.00"));

        verify(kafkaTemplate, times(1)).send(any(), any());
        assertThat(payloadCaptor.getValue()).isInstanceOf(String.class);
        assertThat((String) payloadCaptor.getValue()).contains("\"tipo\":\"IR_VENDA\"");
        assertThat((String) payloadCaptor.getValue()).contains("\"valorIR\":0");
    }
}
