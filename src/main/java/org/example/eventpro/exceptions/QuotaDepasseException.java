package org.example.eventpro.exceptions;

public class QuotaDepasseException extends BusinessException {
    public QuotaDepasseException(String message) {
        super(message);
    }
}
