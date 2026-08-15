package br.edu.ufrj.tcc.common;

import jakarta.ws.rs.core.Response;

/**
 * Erro de regra de negócio que vira uma resposta HTTP com status específico.
 * Mapeada para JSON por {@link BusinessExceptionMapper}.
 */
public class BusinessException extends RuntimeException {

    private final Response.Status status;

    public BusinessException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(Response.Status.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(Response.Status.CONFLICT, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(Response.Status.UNAUTHORIZED, message);
    }

    public Response.Status getStatus() {
        return status;
    }
}
