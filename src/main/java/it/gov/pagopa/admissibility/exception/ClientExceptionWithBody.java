package it.gov.pagopa.admissibility.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ClientExceptionWithBody extends ClientException{
    private final String code;

    public ClientExceptionWithBody(HttpStatus httpStatus, String code, String message){
        this(httpStatus, code, message, true, null);
    }

    public ClientExceptionWithBody(HttpStatus httpStatus, String code, String message, Throwable ex){
        this(httpStatus, code, message, true, ex);
    }

    public ClientExceptionWithBody(HttpStatus httpStatus, String code, String message, boolean printStackTrace, Throwable ex){
        super(httpStatus, message, printStackTrace, ex);
        this.code = code;
    }
}
