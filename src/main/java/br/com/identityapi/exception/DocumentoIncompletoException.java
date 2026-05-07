package br.com.identityapi.exception;

import java.util.List;

public class DocumentoIncompletoException extends RuntimeException {

    private final List<String> camposFaltantes;

    public DocumentoIncompletoException(List<String> camposFaltantes) {
        super("Não foi possível extrair todos os dados do documento");
        this.camposFaltantes = camposFaltantes;
    }

    public List<String> getCamposFaltantes() {
        return camposFaltantes;
    }
}
