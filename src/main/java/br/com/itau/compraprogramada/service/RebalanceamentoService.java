package br.com.itau.compraprogramada.service;

import br.com.itau.compraprogramada.cotahist.CotahistParser;
import br.com.itau.compraprogramada.domain.cesta.CestaRecomendacao;
import br.com.itau.compraprogramada.domain.cesta.ItemCesta;
import br.com.itau.compraprogramada.domain.cliente.Cliente;
import br.com.itau.compraprogramada.domain.custodia.ContaGrafica;
import br.com.itau.compraprogramada.domain.custodia.Custodia;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao;
import br.com.itau.compraprogramada.domain.motor.HistoricoOperacao.TipoOperacao;
import br.com.itau.compraprogramada.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RebalanceamentoService {

    private final ClienteRepository clienteRepository;
    private final ContaGraficaRepository contaGraficaRepository;
    private final CustodiaRepository custodiaRepository;
    private final HistoricoOperacaoRepository historicoRepository;
    private final CotahistParser cotahistParser;
    private final FiscalService fiscalService;

    /**
     * Executa rebalanceamento para todos os clientes ativos ao mudar a cesta.
     * 9.1: Identifica ativos removidos, adicionados e com percentual alterado.
     */
    @Transactional
    public void executar(CestaRecomendacao cestaAntiga, CestaRecomendacao cestaNova) {
        List<Cliente> clientesAtivos = clienteRepository.findAllByAtivoTrue();
        if (clientesAtivos.isEmpty()) return;

        Map<String, BigDecimal> tickersAntigos = cestaAntiga == null ? Map.of() :
                cestaAntiga.getItens().stream()
                        .collect(Collectors.toMap(ItemCesta::getTicker, ItemCesta::getPercentual));

        Map<String, BigDecimal> tickersNovos = cestaNova.getItens().stream()
                .collect(Collectors.toMap(ItemCesta::getTicker, ItemCesta::getPercentual));

        Set<String> removidos = new HashSet<>(tickersAntigos.keySet());
        removidos.removeAll(tickersNovos.keySet());

        Set<String> adicionados = new HashSet<>(tickersNovos.keySet());
        adicionados.removeAll(tickersAntigos.keySet());

        Set<String> alterados = tickersNovos.keySet().stream()
                .filter(t -> tickersAntigos.containsKey(t) &&
                        tickersAntigos.get(t).compareTo(tickersNovos.get(t)) != 0)
                .collect(Collectors.toSet());

        // Busca cotações de todos os tickers envolvidos
        Set<String> todosTickers = new HashSet<>();
        todosTickers.addAll(removidos);
        todosTickers.addAll(adicionados);
        todosTickers.addAll(alterados);
        todosTickers.addAll(tickersNovos.keySet());

        if (todosTickers.isEmpty()) return;

        Map<String, BigDecimal> cotacoes = cotahistParser.buscarCotacoes(new ArrayList<>(todosTickers));

        for (Cliente cliente : clientesAtivos) {
            try {
                rebalancearCliente(cliente, removidos, adicionados, alterados,
                        tickersNovos, cotacoes, cestaNova);
            } catch (Exception e) {
                log.error("Erro ao rebalancear cliente {}: {}", cliente.getId(), e.getMessage(), e);
            }
        }
    }

    private void rebalancearCliente(Cliente cliente, Set<String> removidos, Set<String> adicionados,
                                     Set<String> alterados, Map<String, BigDecimal> tickersNovos,
                                     Map<String, BigDecimal> cotacoes, CestaRecomendacao cestaNova) {
        ContaGrafica contaFilhote = contaGraficaRepository
                .findByClienteIdAndTipo(cliente.getId(), ContaGrafica.TipoConta.FILHOTE)
                .orElse(null);
        if (contaFilhote == null) return;

        BigDecimal valorVendas = BigDecimal.ZERO;
        BigDecimal lucroLiquido = BigDecimal.ZERO;
        List<HistoricoOperacao> vendasDoMes = new ArrayList<>();

        // 9.2: Vender ativos removidos
        for (String ticker : removidos) {
            Custodia pos = custodiaRepository.findByContaIdAndTicker(contaFilhote.getId(), ticker)
                    .orElse(null);
            if (pos == null || pos.getQuantidade() == 0) continue;

            BigDecimal cotacao = cotacoes.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal valorVenda = cotacao.multiply(BigDecimal.valueOf(pos.getQuantidade()));
            BigDecimal lucro = cotacao.subtract(pos.getPrecoMedio())
                    .multiply(BigDecimal.valueOf(pos.getQuantidade()));

            valorVendas = valorVendas.add(valorVenda);
            lucroLiquido = lucroLiquido.add(lucro);

            HistoricoOperacao op = new HistoricoOperacao(cliente, contaFilhote, ticker,
                    TipoOperacao.VENDA, pos.getQuantidade(), cotacao);
            historicoRepository.save(op);
            vendasDoMes.add(op);

            pos.setQuantidade(0L);
            custodiaRepository.save(pos);
        }

        // 9.4: Ajustar ativos com percentual alterado
        BigDecimal valorTotalCarteira = calcularValorTotal(contaFilhote, cotacoes);

        for (String ticker : alterados) {
            Custodia pos = custodiaRepository.findByContaIdAndTicker(contaFilhote.getId(), ticker)
                    .orElse(null);
            if (pos == null || pos.getQuantidade() == 0) continue;

            BigDecimal cotacao = cotacoes.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal percentualNovo = tickersNovos.get(ticker).divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);
            BigDecimal valorAlvo = valorTotalCarteira.multiply(percentualNovo);
            BigDecimal valorAtual = cotacao.multiply(BigDecimal.valueOf(pos.getQuantidade()));

            if (valorAtual.compareTo(valorAlvo) > 0) {
                // Sobre-alocado: vender excesso
                BigDecimal excesso = valorAtual.subtract(valorAlvo);
                long qtdVender = excesso.divide(cotacao, 0, RoundingMode.DOWN).longValue();
                if (qtdVender > 0 && qtdVender <= pos.getQuantidade()) {
                    BigDecimal valorV = cotacao.multiply(BigDecimal.valueOf(qtdVender));
                    BigDecimal lucro = cotacao.subtract(pos.getPrecoMedio()).multiply(BigDecimal.valueOf(qtdVender));
                    valorVendas = valorVendas.add(valorV);
                    lucroLiquido = lucroLiquido.add(lucro);

                    HistoricoOperacao op = new HistoricoOperacao(cliente, contaFilhote, ticker,
                            TipoOperacao.VENDA, qtdVender, cotacao);
                    historicoRepository.save(op);
                    vendasDoMes.add(op);

                    pos.setQuantidade(pos.getQuantidade() - qtdVender);
                    custodiaRepository.save(pos);
                }
            }
        }

        // 9.3: Comprar ativos adicionados com valor obtido nas vendas
        if (valorVendas.compareTo(BigDecimal.ZERO) > 0 && !adicionados.isEmpty()) {
            BigDecimal somaPercentuaisNovos = adicionados.stream()
                    .map(tickersNovos::get)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (String ticker : adicionados) {
                BigDecimal cotacao = cotacoes.getOrDefault(ticker, BigDecimal.ZERO);
                if (cotacao.compareTo(BigDecimal.ZERO) == 0) continue;

                BigDecimal fracaoDoValor = tickersNovos.get(ticker)
                        .divide(somaPercentuaisNovos, 6, RoundingMode.DOWN);
                BigDecimal valorCompra = valorVendas.multiply(fracaoDoValor);
                long qtdComprar = valorCompra.divide(cotacao, 0, RoundingMode.DOWN).longValue();
                if (qtdComprar == 0) continue;

                Custodia pos = custodiaRepository.findByContaIdAndTicker(contaFilhote.getId(), ticker)
                        .orElse(new Custodia(contaFilhote, ticker));

                pos.setPrecoMedio(calcularNovoPrecoMedio(pos, qtdComprar, cotacao));
                pos.setQuantidade(pos.getQuantidade() + qtdComprar);
                custodiaRepository.save(pos);

                historicoRepository.save(new HistoricoOperacao(cliente, contaFilhote, ticker,
                        TipoOperacao.COMPRA, qtdComprar, cotacao));
            }
        }

        // 8.5: Publica IR venda
        if (!vendasDoMes.isEmpty()) {
            fiscalService.calcularEPublicarIRVenda(cliente, vendasDoMes);
        }
    }

    private BigDecimal calcularValorTotal(ContaGrafica conta, Map<String, BigDecimal> cotacoes) {
        return custodiaRepository.findAllByContaId(conta.getId()).stream()
                .filter(c -> c.getQuantidade() > 0)
                .map(c -> cotacoes.getOrDefault(c.getTicker(), BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(c.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularNovoPrecoMedio(Custodia pos, long qtdNova, BigDecimal preco) {
        if (pos.getQuantidade() == 0) return preco.setScale(6, RoundingMode.DOWN);
        BigDecimal valorAnt = pos.getPrecoMedio().multiply(BigDecimal.valueOf(pos.getQuantidade()));
        BigDecimal valorNovo = preco.multiply(BigDecimal.valueOf(qtdNova));
        long qtdTotal = pos.getQuantidade() + qtdNova;
        return valorAnt.add(valorNovo).divide(BigDecimal.valueOf(qtdTotal), 6, RoundingMode.DOWN);
    }
}
