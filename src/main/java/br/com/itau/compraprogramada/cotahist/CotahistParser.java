package br.com.itau.compraprogramada.cotahist;

import br.com.itau.compraprogramada.exception.NegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Parser do arquivo COTAHIST da B3.
 * Layout posicional fixo de 245 caracteres por linha.
 * Posições baseadas em índice 1 (conforme documentação B3), convertidas para 0-based.
 */
@Slf4j
@Component
public class CotahistParser {

    // Posições (0-based, tamanho)
    private static final int POS_TIPREG = 0;
    private static final int TAM_TIPREG = 2;
    private static final int POS_DATPRE = 2;
    private static final int TAM_DATPRE = 8;
    private static final int POS_CODBDI = 10;
    private static final int TAM_CODBDI = 2;
    private static final int POS_CODNEG = 12;
    private static final int TAM_CODNEG = 12;
    private static final int POS_TPMERC = 24;
    private static final int TAM_TPMERC = 3;
    private static final int POS_PREULT = 108;
    private static final int TAM_PREULT = 13;

    private static final String TIPO_DETALHE = "01";
    private static final String CODBDI_LOTE_PADRAO = "02";
    private static final String CODBDI_FRACIONARIO = "96";
    private static final String TPMERC_VISTA = "010";
    private static final String TPMERC_FRACIONARIO = "020";

    @Value("${cotahist.diretorio:./cotacoes}")
    private String diretorio;

    /**
     * Retorna mapa ticker → preço de fechamento para os tickers solicitados.
     * Usa o registro mais recente disponível (suporte a arquivo acumulativo).
     */
    public Map<String, BigDecimal> buscarCotacoes(List<String> tickers) {
        Path dir = Paths.get(diretorio);
        if (!Files.exists(dir)) {
            throw new NegocioException(
                    "Diretório COTAHIST não encontrado: " + dir.toAbsolutePath(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        Map<String, BigDecimal> resultado = new HashMap<>();
        Map<String, String> maiorData = new HashMap<>(); // ticker → datpre

        try (Stream<Path> arquivos = Files.walk(dir, 1)) {
            List<Path> cotahistFiles = arquivos
                    .filter(p -> p.getFileName().toString().matches("COTAHIST.*\\.TXT"))
                    .sorted()
                    .toList();

            if (cotahistFiles.isEmpty()) {
                throw new NegocioException(
                        "Nenhum arquivo COTAHIST encontrado em: " + dir.toAbsolutePath(),
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            for (Path arquivo : cotahistFiles) {
                processarArquivo(arquivo, tickers, resultado, maiorData);
            }
        } catch (IOException e) {
            throw new NegocioException("Erro ao ler diretório COTAHIST: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Verificar se todos os tickers foram encontrados
        for (String ticker : tickers) {
            if (!resultado.containsKey(ticker)) {
                throw new NegocioException(
                        "Cotação não encontrada para o ativo: " + ticker,
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        }

        return resultado;
    }

    private void processarArquivo(Path arquivo, List<String> tickers,
                                   Map<String, BigDecimal> resultado,
                                   Map<String, String> maiorData) {
        try (Stream<String> linhas = Files.lines(arquivo, Charset.forName("ISO-8859-1"))) {
            linhas.forEach(linha -> processarLinha(linha, tickers, resultado, maiorData));
        } catch (IOException e) {
            log.warn("Erro ao processar arquivo COTAHIST {}: {}", arquivo, e.getMessage());
        }
    }

    private void processarLinha(String linha, List<String> tickers,
                                 Map<String, BigDecimal> resultado,
                                 Map<String, String> maiorData) {
        if (linha.length() < 121) return; // PREULT termina na posição 121

        String tipreg = linha.substring(POS_TIPREG, POS_TIPREG + TAM_TIPREG);
        if (!TIPO_DETALHE.equals(tipreg)) return;

        String codbdi = linha.substring(POS_CODBDI, POS_CODBDI + TAM_CODBDI).trim();
        if (!CODBDI_LOTE_PADRAO.equals(codbdi) && !CODBDI_FRACIONARIO.equals(codbdi)) return;

        String tpmerc = linha.substring(POS_TPMERC, POS_TPMERC + TAM_TPMERC).trim();
        if (!TPMERC_VISTA.equals(tpmerc) && !TPMERC_FRACIONARIO.equals(tpmerc)) return;

        String ticker = linha.substring(POS_CODNEG, POS_CODNEG + TAM_CODNEG).trim();
        if (!tickers.contains(ticker)) return;

        String datpre = linha.substring(POS_DATPRE, POS_DATPRE + TAM_DATPRE);
        String dataAtual = maiorData.getOrDefault(ticker, "");

        if (datpre.compareTo(dataAtual) >= 0) {
            BigDecimal precoFechamento = parsePreco(linha.substring(POS_PREULT, POS_PREULT + TAM_PREULT));
            resultado.put(ticker, precoFechamento);
            maiorData.put(ticker, datpre);
        }
    }

    /**
     * Converte campo numérico do COTAHIST (inteiro com 2 casas implícitas) para BigDecimal.
     * Ex: "0000000003580" → 35.80
     */
    BigDecimal parsePreco(String valorBruto) {
        try {
            long valor = Long.parseLong(valorBruto.trim());
            return BigDecimal.valueOf(valor, 2);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
