package br.com.itau.compraprogramada.dto;

import br.com.itau.compraprogramada.domain.cliente.Cliente;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ClienteResponse {

    private Long id;
    private String nome;
    private String cpf;
    private String email;
    private BigDecimal valorMensal;
    private Boolean ativo;
    private LocalDateTime dataAdesao;
    private String numeroConta;

    public static ClienteResponse of(Cliente cliente, String numeroConta) {
        ClienteResponse r = new ClienteResponse();
        r.id = cliente.getId();
        r.nome = cliente.getNome();
        r.cpf = mascararCpf(cliente.getCpf());
        r.email = cliente.getEmail();
        r.valorMensal = cliente.getValorMensal();
        r.ativo = cliente.getAtivo();
        r.dataAdesao = cliente.getDataAdesao();
        r.numeroConta = numeroConta;
        return r;
    }

    private static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }
}
