package com.nikiforov.aichatbot.exceptionhandler.exception;

import com.nikiforov.aichatbot.exceptionhandler.ErrorCodes;

import java.io.Serial;

public class RestException extends CodedException {

    @Serial
    private static final long serialVersionUID = 7587934933756030867L;

    public RestException(ErrorCodes errorCode) {
        super(errorCode);
    }
}
