package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import br.com.itau.compraprogramada.domain.cesta.ItemCesta;
import br.com.itau.compraprogramada.dto.CestaRequest;
import br.com.itau.compraprogramada.dto.CestaResponse;
import br.com.itau.compraprogramada.exception.NegocioException;
import br.com.itau.compraprogramada.repository.CestaRecomendacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CestaServiceTest {

    @Mock
    private CestaRecomendacaoRepository cestaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CestaService cestaService;

    private CestaRequest requestValida;

    @BeforeEach
    void setup() {
        requestValida = new CestaRequest();
        requestValida.setItens(List.of(
                item("PETR4", "30.00"),
                item("VALE3", "25.00"),
                item("ITUB4", "20.00"),
                item("BBDC4", "15.00"),
                item("WEGE3", "10.00")
        ));
    }

    @Test
    void criar_cestaValida_retornaResponse() {
        when(cestaRepository.findByAtivoTrue()).thenReturn(Optional.empty());
        when(cestaRepository.save(any())).thenAnswer(inv -> {
            CestaRecomendacao c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CestaResponse response = cestaService.criar(requestValida);

        assertThat(response.getAtivo()).isTrue();
        assertThat(response.getItens()).hasSize(5);
    }

    @Test
    void criar_somaDiferenteDe100_lancaBadRequest() {
        CestaRequest req = new CestaRequest();
        req.setItens(List.of(
                item("PETR4", "30.00"),
                item("VALE3", "25.00"),
                item("ITUB4", "20.00"),
                item("BBDC4", "15.00"),
                item("WEGE3", "5.00") // soma = 95, não 100
        ));

        NegocioException ex = catchThrowableOfType(
                () -> cestaService.criar(req),
                NegocioException.class
        );

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("100");
    }

    @Test
    void criar_comCestaAnterior_desativaAnteriorEDispararEvento() {
        CestaRecomendacao antiga = new CestaRecomendacao();
        antiga.setId(1L);
        antiga.setAtivo(true);
        when(cestaRepository.findByAtivoTrue()).thenReturn(Optional.of(antiga));
        when(cestaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cestaService.criar(requestValida);

        assertThat(antiga.getAtivo()).isFalse();
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void buscarAtiva_semCesta_lancaNotFound() {
        when(cestaRepository.findByAtivoTrue()).thenReturn(Optional.empty());

        NegocioException ex = catchThrowableOfType(
                () -> cestaService.buscarAtiva(),
                NegocioException.class
        );

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private CestaRequest.ItemCestaRequest item(String ticker, String percentual) {
        CestaRequest.ItemCestaRequest item = new CestaRequest.ItemCestaRequest();
        item.setTicker(ticker);
        item.setPercentual(new BigDecimal(percentual));
        return item;
    }
}
