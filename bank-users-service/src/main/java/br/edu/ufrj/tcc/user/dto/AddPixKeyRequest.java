package br.edu.ufrj.tcc.user.dto;

import br.edu.ufrj.tcc.user.PixKeyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddPixKeyRequest(
        @NotNull PixKeyType tipo,
        @NotBlank String valor) {
}
