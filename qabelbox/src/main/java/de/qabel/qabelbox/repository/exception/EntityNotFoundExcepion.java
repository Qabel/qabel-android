package de.qabel.qabelbox.repository.exception;

public class EntityNotFoundExcepion extends Exception {
    public EntityNotFoundExcepion(String message) {
        super(message);
    }

    public EntityNotFoundExcepion(String message, Throwable cause) {
        super(message, cause);
    }
}
