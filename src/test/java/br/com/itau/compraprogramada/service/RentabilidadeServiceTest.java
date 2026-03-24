package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.cotahist.CotahistParser;
import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import br.com.itau.compraprogramada.dto.CarteiraPosicaoResponse;
import br.com.itau.compraprogramada.exception.NegocioException;
import br.com.itau.compraprogramada.repository.ClienteRepository;
import br.com.itau.compraprogramada.repository.ContaGraficaRepository;
import br.com.itau.compraprogramada.repository.CustodiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentabilidadeServiceTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private ContaGraficaRepository contaGraficaRepository;
    @Mock private CustodiaRepository custodiaRepository;
    @Mock private CotahistParser cotahistParser;

    @InjectMocks
    private RentabilidadeService rentabilidadeService;

    private Cliente cliente;
    private ContaGrafica contaFilhote;

    @BeforeEach
    void setup() {
        cliente = new Cliente("João", "12345678901", "j@e.com", BigDecimal.valueOf(3000));
        cliente.setId(1L);
        contaFilhote = new ContaGrafica(cliente, "FILHOTE-001", ContaGrafica.TipoConta.FILHOTE);
        contaFilhote.setId(2L);

        lenient().when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        lenient().when(contaGraficaRepository.findByClienteIdAndTipo(1L, ContaGrafica.TipoConta.FILHOTE))
                .thenReturn(Optional.of(contaFilhote));
    }

    @Test
    void consultar_carteiraComLucro_retornaRentabilidadePositiva() {
        Custodia petr4 = new Custodia(contaFilhote, "PETR4");
        petr4.setQuantidade(24L);
        petr4.setPrecoMedio(new BigDecimal("35.500000"));

        when(custodiaRepository.findAllByContaIdAndQuantidadeGreaterThan(2L, 0L))
                .thenReturn(List.of(petr4));
        when(cotahistParser.buscarCotacoes(any()))
                .thenReturn(Map.of("PETR4", new BigDecimal("37.00")));

        CarteiraPosicaoResponse response = rentabilidadeService.consultar(1L);

        // Valor investido: 24 × 35,50 = 852,00
        // Valor atual: 24 × 37,00 = 888,00
        // P/L: +36,00
        assertThat(response.getValorInvestidoTotal()).isEqualByComparingTo("852.00");
        assertThat(response.getValorAtualTotal()).isEqualByComparingTo("888.00");
        assertThat(response.getPlTotal()).isEqualByComparingTo("36.00");
        assertThat(response.getRentabilidadePercentual()).isPositive();
    }

    @Test
    void consultar_carteiraComPrejuizo_retornaRentabilidadeNegativa() {
        Custodia petr4 = new Custodia(contaFilhote, "PETR4");
        petr4.setQuantidade(10L);
        petr4.setPrecoMedio(new BigDecimal("40.000000"));

        when(custodiaRepository.findAllByContaIdAndQuantidadeGreaterThan(2L, 0L))
                .thenReturn(List.of(petr4));
        when(cotahistParser.buscarCotacoes(any()))
                .thenReturn(Map.of("PETR4", new BigDecimal("35.00")));

        CarteiraPosicaoResponse response = rentabilidadeService.consultar(1L);

        assertThat(response.getPlTotal()).isNegative();
        assertThat(response.getRentabilidadePercentual()).isNegative();
    }

    @Test
    void consultar_carteiraVazia_retornaZeros() {
        when(custodiaRepository.findAllByContaIdAndQuantidadeGreaterThan(2L, 0L))
                .thenReturn(List.of());

        CarteiraPosicaoResponse response = rentabilidadeService.consultar(1L);

        assertThat(response.getValorAtualTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getAtivos()).isEmpty();
    }

    @Test
    void consultar_clienteInexistente_lancaNotFound() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        NegocioException ex = catchThrowableOfType(
                () -> rentabilidadeService.consultar(99L),
                NegocioException.class
        );
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
