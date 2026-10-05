package com.hospitalflow.beds.application.port.out;

public interface ProcessedMessageStore {
    /** @return true si el mensaje no se había procesado y queda marcado; false si es un duplicado. */
    boolean markProcessed(String messageId);
}
