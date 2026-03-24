package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.cotahist.CotahistParser;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import br.com.itau.compraprogramada.dto.CarteiraPosicaoResponse;
import br.com.itau.compraprogramada.dto.CarteiraPosicaoResponse.AtivoResponse;
import br.com.itau.compraprogramada.exception.NegocioException;
import br.com.itau.compraprogramada.repository.ClienteRepository;
import br.com.itau.compraprogramada.repository.ContaGraficaRepository;
import br.com.itau.compraprogramada.repository.CustodiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentabilidadeService {

    private final ClienteRepository clienteRepository;
    private final ContaGraficaRepository contaGraficaRepository;
    private final CustodiaRepository custodiaRepository;
    private final CotahistParser cotahistParser;

    @Transactional(readOnly = true)
    public CarteiraPosicaoResponse consultar(Long clienteId) {
        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new NegocioException("Cliente não encontrado", HttpStatus.NOT_FOUND));

        ContaGrafica contaFilhote = contaGraficaRepository
                .findByClienteIdAndTipo(clienteId, ContaGrafica.TipoConta.FILHOTE)
                .orElseThrow(() -> new NegocioException("Conta filhote não encontrada", HttpStatus.NOT_FOUND));

        List<Custodia> posicoes = custodiaRepository
                .findAllByContaIdAndQuantidadeGreaterThan(contaFilhote.getId(), 0L);

        if (posicoes.isEmpty()) {
            return CarteiraPosicaoResponse.builder()
                    .clienteId(clienteId)
                    .valorInvestidoTotal(BigDecimal.ZERO)
                    .valorAtualTotal(BigDecimal.ZERO)
                    .plTotal(BigDecimal.ZERO)
                    .rentabilidadePercentual(BigDecimal.ZERO)
                    .ativos(List.of())
                    .build();
        }

        List<String> tickers = posicoes.stream().map(Custodia::getTicker).toList();
        Map<String, BigDecimal> cotacoes = cotahistParser.buscarCotacoes(tickers);

        BigDecimal valorAtualTotal = BigDecimal.ZERO;
        BigDecimal valorInvestidoTotal = BigDecimal.ZERO;

        // Primeiro passo: calcula totais
        for (Custodia pos : posicoes) {
            BigDecimal cotacao = cotacoes.getOrDefault(pos.getTicker(), BigDecimal.ZERO);
            valorAtualTotal = valorAtualTotal.add(cotacao.multiply(BigDecimal.valueOf(pos.getQuantidade())));
            valorInvestidoTotal = valorInvestidoTotal.add(
                    pos.getPrecoMedio().multiply(BigDecimal.valueOf(pos.getQuantidade())));
        }

        BigDecimal plTotal = valorAtualTotal.subtract(valorInvestidoTotal);
        BigDecimal rentabilidade = valorInvestidoTotal.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : plTotal.divide(valorInvestidoTotal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        final BigDecimal valorAtualFinal = valorAtualTotal;

        List<AtivoResponse> ativos = posicoes.stream().map(pos -> {
            BigDecimal cotacao = cotacoes.getOrDefault(pos.getTicker(), BigDecimal.ZERO);
            BigDecimal valorAtualAtivo = cotacao.multiply(BigDecimal.valueOf(pos.getQuantidade()));
            BigDecimal pl = cotacao.subtract(pos.getPrecoMedio().setScale(2, RoundingMode.HALF_UP))
                    .multiply(BigDecimal.valueOf(pos.getQuantidade()));
            BigDecimal composicao = valorAtualFinal.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : valorAtualAtivo.divide(valorAtualFinal, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

            return AtivoResponse.builder()
                    .ticker(pos.getTicker())
                    .quantidade(pos.getQuantidade())
                    .precoMedio(pos.getPrecoMedio().setScale(2, RoundingMode.HALF_UP))
                    .cotacaoAtual(cotacao)
                    .valorAtual(valorAtualAtivo.setScale(2, RoundingMode.HALF_UP))
                    .pl(pl.setScale(2, RoundingMode.HALF_UP))
                    .composicaoPercentual(composicao)
                    .build();
        }).collect(Collectors.toList());

        return CarteiraPosicaoResponse.builder()
                .clienteId(clienteId)
                .valorInvestidoTotal(valorInvestidoTotal.setScale(2, RoundingMode.HALF_UP))
                .valorAtualTotal(valorAtualTotal.setScale(2, RoundingMode.HALF_UP))
                .plTotal(plTotal.setScale(2, RoundingMode.HALF_UP))
                .rentabilidadePercentual(rentabilidade)
                .ativos(ativos)
                .build();
    }
}
