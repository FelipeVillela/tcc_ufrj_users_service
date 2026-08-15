package br.edu.ufrj.tcc.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais enviadas pela tela de login.
 *
 * O e-mail não tem validação de formato de propósito: qualquer credencial que
 * não bata devolve 401, sem diferenciar "e-mail malformado" de "senha errada".
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String senha) {
}
