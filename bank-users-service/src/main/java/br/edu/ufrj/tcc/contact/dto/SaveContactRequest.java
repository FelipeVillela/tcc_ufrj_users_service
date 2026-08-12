package br.edu.ufrj.tcc.contact.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/**
 * Enviado quando o usuário confirma o envio de um Pix e autoriza salvar o
 * destinatário na sua lista de contatos ("salvar contato com permissão").
 * {@code nomesAlternativos} é opcional.
 */
public record SaveContactRequest(
        @NotBlank String nome,
        @NotBlank String chavePix,
        String banco,
        List<String> nomesAlternativos) {
}
