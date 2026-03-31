package controle.api.back_end.exception;

public class EntidadeJaExisteException extends RuntimeException {
    public EntidadeJaExisteException(String message) {
        super(message);
    }
}
