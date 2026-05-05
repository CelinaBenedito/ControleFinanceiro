package controle.api.back_end.exception;

public class SenhasNaoCoincidemException extends RuntimeException {
    public SenhasNaoCoincidemException() {
        super("As senhas não conincidem");
    }
}
