package br.com.identityapi.exception;

public class IdadeInvalidaException extends RuntimeException {

    private final String detalhe;

    public IdadeInvalidaException(String mensagem, String detalhe) {
        super(mensagem);
        this.detalhe = detalhe;
    }

    public String getDetalhe() {
        return detalhe;
    }
}
