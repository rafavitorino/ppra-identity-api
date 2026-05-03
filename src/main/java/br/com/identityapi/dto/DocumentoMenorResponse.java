package br.com.identityapi.dto;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public record DocumentoMenorResponse(
        String status,
        String nome,
        Integer idade,
        String cpf,
        String dataNascimento,
        String naturalidade,
        String nomePai,
        String nomeMae,
        String orgaoExpedidor,
        String registroGeral,
        String dataExpedicao,
        String textoExtraido
) {
    public static DocumentoMenorResponse of(
            String nome, String cpf, String dataNascimento, String naturalidade,
            String nomePai, String nomeMae, String orgaoExpedidor,
            String registroGeral, String dataExpedicao, String textoExtraido) {

        Integer idade = calcularIdade(dataNascimento);

        boolean sucesso = nome != null && cpf != null && dataNascimento != null
                && naturalidade != null && nomePai != null && nomeMae != null
                && orgaoExpedidor != null && registroGeral != null && dataExpedicao != null;

        return new DocumentoMenorResponse(sucesso ? "SUCESSO" : "FALHOU",
                nome, idade, cpf, dataNascimento, naturalidade,
                nomePai, nomeMae, orgaoExpedidor, registroGeral, dataExpedicao,
                textoExtraido);
    }

    private static Integer calcularIdade(String dataNascimento) {
        if (dataNascimento == null) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate nascimento = LocalDate.parse(dataNascimento, formatter);
            return Period.between(nascimento, LocalDate.now()).getYears();
        } catch (Exception e) {
            return null;
        }
    }
}
