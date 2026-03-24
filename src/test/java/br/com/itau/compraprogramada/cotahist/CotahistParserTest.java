package br.com.itau.compraprogramada.cotahist;

import br.com.itau.compraprogramada.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CotahistParserTest {

    private CotahistParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        parser = new CotahistParser();
        // Copia o arquivo de teste para o diretório temporário
        Path origem = Path.of("src/test/resources/COTAHIST_D20260225.TXT");
        Files.copy(origem, tempDir.resolve("COTAHIST_D20260225.TXT"));
        ReflectionTestUtils.setField(parser, "diretorio", tempDir.toString());
    }

    @Test
    void parsePreco_converteCorretamente() {
        assertThat(parser.parsePreco("0000000003580")).isEqualByComparingTo("35.80");
        assertThat(parser.parsePreco("0000000001500")).isEqualByComparingTo("15.00");
        assertThat(parser.parsePreco("0000000012345")).isEqualByComparingTo("123.45");
    }

    @Test
    void buscarCotacoes_retornaPrecosFechamento() {
        Map<String, BigDecimal> cotacoes = parser.buscarCotacoes(
                List.of("PETR4", "VALE3", "ITUB4", "BBDC4", "WEGE3")
        );

        assertThat(cotacoes).containsKeys("PETR4", "VALE3", "ITUB4", "BBDC4", "WEGE3");
        assertThat(cotacoes.get("PETR4")).isEqualByComparingTo("35.80");
        assertThat(cotacoes.get("VALE3")).isEqualByComparingTo("62.00");
        assertThat(cotacoes.get("ITUB4")).isEqualByComparingTo("30.00");
        assertThat(cotacoes.get("BBDC4")).isEqualByComparingTo("15.00");
        assertThat(cotacoes.get("WEGE3")).isEqualByComparingTo("40.00");
    }

    @Test
    void buscarCotacoes_tickerNaoEncontrado_lancaExcecao() {
        assertThatThrownBy(() -> parser.buscarCotacoes(List.of("XXXX3")))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("XXXX3");
    }

    @Test
    void buscarCotacoes_diretorioInexistente_lancaExcecao() {
        ReflectionTestUtils.setField(parser, "diretorio", "/caminho/inexistente");

        assertThatThrownBy(() -> parser.buscarCotacoes(List.of("PETR4")))
                .isInstanceOf(NegocioException.class);
    }
}
