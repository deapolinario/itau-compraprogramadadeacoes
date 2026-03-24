package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.cotahist.CotahistParser;
import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import br.com.itau.compraprogramada.domain.cesta.ItemCesta;
import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import br.com.itau.compraprogramada.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RebalanceamentoServiceTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private ContaGraficaRepository contaGraficaRepository;
    @Mock private CustodiaRepository custodiaRepository;
    @Mock private HistoricoOperacaoRepository historicoRepository;
    @Mock private CotahistParser cotahistParser;
    @Mock private FiscalService fiscalService;

    @InjectMocks
    private RebalanceamentoService rebalanceamentoService;

    private Cliente cliente;
    private ContaGrafica contaFilhote;

    @BeforeEach
    void setup() {
        cliente = new Cliente("João", "12345678901", "j@e.com", BigDecimal.valueOf(3000));
        cliente.setId(1L);

        contaFilhote = new ContaGrafica(cliente, "FILHOTE-001", ContaGrafica.TipoConta.FILHOTE);
        contaFilhote.setId(2L);

        lenient().when(clienteRepository.findAllByAtivoTrue()).thenReturn(List.of(cliente));
        lenient().when(contaGraficaRepository.findByClienteIdAndTipo(1L, ContaGrafica.TipoConta.FILHOTE))
                .thenReturn(Optional.of(contaFilhote));
        lenient().when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(custodiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(custodiaRepository.findAllByContaId(2L)).thenReturn(List.of());
    }

    @Test
    void executar_ativoRemovido_vendeTodasAsAcoes() {
        CestaRecomendacao antiga = criarCesta("BBDC4", "15.00");
        CestaRecomendacao nova = criarCesta("ABEV3", "100.00");

        Custodia posBBDC4 = new Custodia(contaFilhote, "BBDC4");
        posBBDC4.setQuantidade(10L);
        posBBDC4.setPrecoMedio(new BigDecimal("14.00"));

        when(cotahistParser.buscarCotacoes(anyList()))
                .thenReturn(Map.of("BBDC4", new BigDecimal("15.00"), "ABEV3", new BigDecimal("14.00")));
        when(custodiaRepository.findByContaIdAndTicker(2L, "BBDC4")).thenReturn(Optional.of(posBBDC4));
        when(custodiaRepository.findByContaIdAndTicker(2L, "ABEV3")).thenReturn(Optional.empty());

        rebalanceamentoService.executar(antiga, nova);

        assertThat(posBBDC4.getQuantidade()).isEqualTo(0L);
        verify(historicoRepository, atLeast(1)).save(any()); // venda registrada
    }

    @Test
    void executar_ativoMantidoSemAlteracao_naoOperacoes() {
        CestaRecomendacao antiga = criarCesta("ITUB4", "20.00");
        CestaRecomendacao nova = criarCesta("ITUB4", "20.00");

        when(cotahistParser.buscarCotacoes(anyList())).thenReturn(Map.of("ITUB4", new BigDecimal("30.00")));

        rebalanceamentoService.executar(antiga, nova);

        // Sem alterados, sem removidos, sem adicionados → nenhuma operação
        verify(historicoRepository, never()).save(any());
    }

    @Test
    void executar_semClientesAtivos_naoFazNada() {
        when(clienteRepository.findAllByAtivoTrue()).thenReturn(List.of());

        rebalanceamentoService.executar(criarCesta("PETR4", "100.00"), criarCesta("VALE3", "100.00"));

        verify(cotahistParser, never()).buscarCotacoes(any());
    }

    @Test
    void executar_ativoRemovido_passaLucroLiquidoCorretoParaFiscal() {
        // PM = R$ 14,00, cotação = R$ 16,00, qtd = 10 → lucro = (16 - 14) * 10 = R$ 20,00
        CestaRecomendacao antiga = criarCesta("BBDC4", "100.00");
        CestaRecomendacao nova = criarCesta("VALE3", "100.00");

        Custodia posBBDC4 = new Custodia(contaFilhote, "BBDC4");
        posBBDC4.setQuantidade(10L);
        posBBDC4.setPrecoMedio(new BigDecimal("14.00"));

        when(cotahistParser.buscarCotacoes(anyList()))
                .thenReturn(Map.of("BBDC4", new BigDecimal("16.00"), "VALE3", new BigDecimal("10.00")));
        when(custodiaRepository.findByContaIdAndTicker(2L, "BBDC4")).thenReturn(Optional.of(posBBDC4));
        when(custodiaRepository.findByContaIdAndTicker(2L, "VALE3")).thenReturn(Optional.empty());

        rebalanceamentoService.executar(antiga, nova);

        ArgumentCaptor<BigDecimal> lucroCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(fiscalService).calcularEPublicarIRVenda(eq(cliente), anyList(), lucroCaptor.capture());
        assertThat(lucroCaptor.getValue()).isEqualByComparingTo("20.00");
    }

    private CestaRecomendacao criarCesta(String ticker, String percentual) {
        CestaRecomendacao cesta = new CestaRecomendacao();
        cesta.setId((long) (Math.random() * 1000));
        ItemCesta item = new ItemCesta(cesta, ticker, new BigDecimal(percentual));
        cesta.getItens().add(item);
        return cesta;
    }
}
