package com.nikiforov.aichatbot.exceptionhandler.exception;

import com.nikiforov.aichatbot.exceptionhandler.ErrorCodes;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Getter
@Setter
public class CodedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 182214224795326875L;

    private final int httpStatus;
    private final String errorCode;

    public CodedException(ErrorCodes errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = errorCode.getHttpStatus().value();
        this.errorCode = errorCode.getCode();
    }
}
