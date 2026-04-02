package controle.api.back_end.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MenorDeIdadeException extends RuntimeException {
    public MenorDeIdadeException(String message) {
        super(message);
    }
}
