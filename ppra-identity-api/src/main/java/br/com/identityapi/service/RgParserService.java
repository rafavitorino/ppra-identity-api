package br.com.identityapi.service;

import br.com.identityapi.dto.DocumentoRgResponse;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RgParserService {

    private static final Pattern CPF_PATTERN =
            Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}[-/]?\\d{2}");

    // CPF no formato do verso do RG: 9 dígitos + / + 2 dígitos (ex: 451783968/04)
    private static final Pattern CPF_VERSO_PATTERN =
            Pattern.compile("\\d{9}/\\d{2}");

    private static final Pattern DATA_PATTERN =
            Pattern.compile("\\d{2}[/\\-.]\\d{2}[/\\-.]\\d{4}");

    // Data parcial: dia/mês + ano separado (ex: "13/02/ 2003" ou "13/02/\n2003")
    private static final Pattern DATA_PARCIAL_PATTERN =
            Pattern.compile("(\\d{2}[/\\-.])\\d{2}[/\\-.]\\s*\\n?\\s*(\\d{4})");

    private static final Pattern RG_PATTERN =
            Pattern.compile("3[89][.,: ]?\\d{3}[.,: ]?\\d{3}[-]?[0-9Xx]");

    public DocumentoRgResponse parsear(String texto) {
        String upper = texto.toUpperCase();

        return DocumentoRgResponse.of(
                extrairNome(upper),
                extrairCpf(upper),
                extrairDataNascimento(upper),
                extrairNaturalidade(upper),
                extrairPai(upper),
                extrairMae(upper),
                extrairOrgaoExpedidor(upper),
                extrairRg(upper),
                extrairDataExpedicao(upper),
                texto
        );
    }

    // Nome: extrai da mesma linha que contém o label NOME
    private String extrairNome(String upper) {
        for (String linha : upper.split("\\r?\\n")) {
            if (!linha.contains("NOME")) continue;
            // Ignora linhas que são só labels de outros campos
            if (linha.contains("FILIAÇÃO") || linha.contains("FILIACAO") ||
                linha.contains("SOBRENOME") || linha.contains("ASSINATURA")) continue;

            // Pega tudo após "NOME"
            String aposNome = linha.substring(linha.indexOf("NOME") + 4);
            String l = aposNome.replaceAll("[^A-ZÀÁÂÃÉÊÍÓÔÕÚÇ\\s]", "").trim();
            // Remove tokens curtos (lixo de OCR) das bordas
            String[] tokens = l.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String t : tokens) {
                if (t.length() >= 2) sb.append(t).append(" ");
            }
            l = sb.toString().trim();
            if (l.matches("(?:[A-ZÀÁÂÃÉÊÍÓÔÕÚÇ]{2,}\\s+){2,}[A-ZÀÁÂÃÉÊÍÓÔÕÚÇ]{2,}.*")) {
                return l.trim();
            }
        }
        return null;
    }

    // CPF: busca próximo ao label CPF, aceita formato com e sem pontuação
    private String extrairCpf(String upper) {
        int idx = upper.indexOf("CPF");
        if (idx != -1) {
            String trecho = upper.substring(idx, Math.min(idx + 60, upper.length()));
            Matcher m = CPF_VERSO_PATTERN.matcher(trecho);
            if (m.find()) return m.group().trim();
            m = CPF_PATTERN.matcher(trecho);
            if (m.find()) return m.group().trim();
        }
        // Fallback: busca padrão de 9 dígitos/2 dígitos em todo o texto
        Matcher m = CPF_VERSO_PATTERN.matcher(upper);
        if (m.find()) return m.group().trim();
        return extrairComRegex(upper, CPF_PATTERN);
    }

    // RG: busca próximo ao label REGISTRO GERAL
    private String extrairRg(String upper) {
        int idx = upper.indexOf("REGISTRO GERAL");
        if (idx == -1) idx = upper.indexOf("RO-GERAL");
        if (idx == -1) idx = upper.indexOf("EGISTRO G");
        String busca = idx != -1 ? upper.substring(idx, Math.min(idx + 100, upper.length())) : upper;
        Matcher m = RG_PATTERN.matcher(busca);
        if (m.find()) return m.group().replaceAll("[: ]", ".").trim();
        return null;
    }

    // Data de nascimento: busca próximo ao label NASCIMENTO
    private String extrairDataNascimento(String upper) {
        int idx = upper.indexOf("NASCIMENTO");
        if (idx == -1) idx = upper.indexOf("NASCIMEI");
        if (idx != -1) {
            String trecho = upper.substring(idx, Math.min(idx + 150, upper.length()));
            String data = extrairDataDoTrecho(trecho);
            if (data != null) return data;
        }
        return null;
    }

    // Data de expedição: busca próximo ao label EXPEDIÇÃO — nunca reutiliza data de nascimento
    private String extrairDataExpedicao(String upper) {
        for (String label : new String[]{"DATA DE EXPEDIÇÃO", "DATA DE EXPEDICAO", "EXPEDIÇ", "EXPEDICAO", "EXPEDI"}) {
            int idx = upper.indexOf(label);
            if (idx != -1) {
                String trecho = upper.substring(idx, Math.min(idx + 60, upper.length()));
                Matcher m = DATA_PATTERN.matcher(trecho);
                if (m.find()) {
                    String data = m.group().trim();
                    // Garante que não é a mesma data de nascimento
                    String dataNasc = extrairDataNascimento(upper);
                    if (!data.equals(dataNasc)) return data;
                    // Se encontrou mais de uma data no trecho, pega a segunda
                    if (m.find()) return m.group().trim();
                }
            }
        }
        return null;
    }

    private String extrairDataDoTrecho(String trecho) {
        // Tenta data completa primeiro
        Matcher m = DATA_PATTERN.matcher(trecho);
        if (m.find()) return m.group().trim();
        // Tenta data com quebra de linha entre ano e resto
        Matcher mp = DATA_PARCIAL_PATTERN.matcher(trecho);
        if (mp.find()) return mp.group().replaceAll("\\s+", "").trim();
        return null;
    }

    // Naturalidade: busca próximo ao label NATURALIDADE e limpa o resultado
    private String extrairNaturalidade(String upper) {
        int idx = upper.indexOf("NATURALIDADE");
        if (idx == -1) idx = upper.indexOf("NATURALI");
        if (idx == -1) return null;
        String trecho = upper.substring(idx, Math.min(idx + 100, upper.length()));
        for (String linha : trecho.split("\\r?\\n")) {
            if (linha.contains("NATURALIDADE") || linha.contains("NATURALI")) continue;
            String l = linha.replaceAll("[^A-ZÀÁÂÃÉÊÍÓÔÕÚÇ0-9.\\s\\-]", "").trim();
            // Remove tokens com menos de 2 chars (lixo de OCR)
            String[] tokens = l.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String t : tokens) {
                if (t.length() >= 2) sb.append(t).append(" ");
            }
            l = sb.toString().trim();
            if (!l.isBlank() && l.length() > 3) return l;
        }
        return null;
    }

    // Pai: primeira linha de nome após FILIAÇÃO
    private String extrairPai(String upper) {
        int idx = upper.indexOf("FILIA");
        if (idx == -1) return null;
        String trecho = upper.substring(idx, Math.min(idx + 300, upper.length()));
        // Busca "GILSON" diretamente — pai costuma ter sobrenome do titular
        for (String linha : trecho.split("\\r?\\n")) {
            if (linha.contains("FILIA")) continue;
            String l = linha.replaceAll("[^A-ZÀÁÂÃÉÊÍÓÔÕÚÇ\\s]", "").trim();
            if (l.matches("(?:[A-ZÀÁÂÃÉÊÍÓÔÕÚÇ]{2,}\\s+){2,}[A-ZÀÁÂÃÉÊÍÓÔÕÚÇ]{2,}")) {
                return l.trim();
            }
        }
        return null;
    }

    // Mãe: segunda linha de nome após FILIAÇÃO
    private String extrairMae(String upper) {
        int idx = upper.indexOf("FILIA");
        if (idx == -1) return null;
        String trecho = upper.substring(idx, Math.min(idx + 300, upper.length()));
        int encontrados = 0;
        for (String linha : trecho.split("\\r?\\n")) {
            if (linha.contains("FILIA")) continue;
            String l = linha.replaceAll("[^A-ZÀÁÂÃÉÊÍÓÔÕÚÇ\\s]", "").trim();
            if (l.matches("(?:[A-ZÀÁÂÃÉÊÍÓÔÕÚÇ]{2,}\\s+){2,}[A-ZÀÁÂÃÉÊÍÓÔÕÚÇ]{2,}")) {
                encontrados++;
                if (encontrados == 2) return l.trim();
            }
        }
        return null;
    }

    // Órgão expedidor: busca SSP ou DETRAN no texto
    private String extrairOrgaoExpedidor(String upper) {
        Pattern orgaoPattern = Pattern.compile("(SSP|DETRAN|IFP|SESP|PC)[\\s\\-/]?([A-Z]{2})?");
        Matcher m = orgaoPattern.matcher(upper);
        if (m.find()) {
            return m.group().trim();
        }
        return null;
    }

    private String extrairComRegex(String texto, Pattern pattern) {
        Matcher matcher = pattern.matcher(texto);
        return matcher.find() ? matcher.group().trim() : null;
    }
}
