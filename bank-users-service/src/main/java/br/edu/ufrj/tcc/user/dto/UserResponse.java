package br.edu.ufrj.tcc.user.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.edu.ufrj.tcc.user.User;

public record UserResponse(
        Long id,
        String nome,
        String email,
        BigDecimal saldo,
        Instant criadoEm,
        List<PixKeyResponse> chavesPix) {

    public static UserResponse from(User user) {
        List<PixKeyResponse> chaves = user.chavesPix.stream()
                .map(PixKeyResponse::from)
                .toList();
        return new UserResponse(user.id, user.nome, user.email, user.saldo, user.criadoEm, chaves);
    }
}
