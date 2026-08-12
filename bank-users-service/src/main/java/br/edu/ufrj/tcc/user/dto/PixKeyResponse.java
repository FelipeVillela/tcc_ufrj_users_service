package br.edu.ufrj.tcc.user.dto;

import java.time.Instant;

import br.edu.ufrj.tcc.user.PixKey;
import br.edu.ufrj.tcc.user.PixKeyType;

public record PixKeyResponse(Long id, PixKeyType tipo, String valor, Instant criadoEm) {

    public static PixKeyResponse from(PixKey key) {
        return new PixKeyResponse(key.id, key.tipo, key.valor, key.criadoEm);
    }
}
