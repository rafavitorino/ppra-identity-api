package br.com.identityapi.dto;

public record DocumentoRgResponse(
        String status,
        String nome,
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
    public static DocumentoRgResponse of(
            String nome, String cpf, String dataNascimento, String naturalidade,
            String nomePai, String nomeMae, String orgaoExpedidor,
            String registroGeral, String dataExpedicao, String textoExtraido) {

        boolean sucesso = nome != null && cpf != null && dataNascimento != null
                && naturalidade != null && nomePai != null && nomeMae != null
                && orgaoExpedidor != null && registroGeral != null && dataExpedicao != null;

        return new DocumentoRgResponse(sucesso ? "SUCESSO" : "FALHOU",
                nome, cpf, dataNascimento, naturalidade,
                nomePai, nomeMae, orgaoExpedidor, registroGeral, dataExpedicao,
                textoExtraido);
    }
}
