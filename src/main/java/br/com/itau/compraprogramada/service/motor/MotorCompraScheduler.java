package br.com.itau.compraprogramada.service.motor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Scheduler do motor de compra.
 * Executa às 9h nos dias 5, 15 e 25 de cada mês.
 * Se a data cair em sábado ou domingo, executa na segunda-feira seguinte.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MotorCompraScheduler {

    private final MotorCompraService motorCompraService;

    @Scheduled(cron = "${motor.compra.cron:0 0 9 5,15,25 * ?}")
    public void executar() {
        LocalDate dataExecucao = LocalDate.now();
        LocalDate dataReferencia = ajustarParaDiaUtil(dataExecucao);

        log.info("Scheduler disparado. Data execução: {}, Data referência: {}", dataExecucao, dataReferencia);
        motorCompraService.executar(dataReferencia);
    }

    /**
     * Retorna a data de referência para o motor.
     * Se a data cair em sábado/domingo, ajusta para a segunda-feira seguinte.
     * A dataReferencia usada para idempotência é a data planejada (5, 15 ou 25), não a real.
     */
    LocalDate ajustarParaDiaUtil(LocalDate data) {
        int dia = data.getDayOfMonth();
        int diaPlanejado = encontrarDiaPlanejado(dia);

        LocalDate dataPlanejada = data.withDayOfMonth(diaPlanejado);
        DayOfWeek diaSemana = dataPlanejada.getDayOfWeek();

        if (diaSemana == DayOfWeek.SATURDAY) {
            return dataPlanejada.plusDays(2); // segunda-feira
        } else if (diaSemana == DayOfWeek.SUNDAY) {
            return dataPlanejada.plusDays(1); // segunda-feira
        }

        return dataPlanejada;
    }

    private int encontrarDiaPlanejado(int diaAtual) {
        // O dia atual pode ser o dia planejado ou a segunda-feira subsequente
        if (diaAtual <= 7) return 5;
        if (diaAtual <= 17) return 15;
        return 25;
    }
}
