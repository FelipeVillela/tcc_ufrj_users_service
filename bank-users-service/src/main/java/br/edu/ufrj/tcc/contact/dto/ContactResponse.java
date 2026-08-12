package br.edu.ufrj.tcc.contact.dto;

import java.time.Instant;
import java.util.List;

import br.edu.ufrj.tcc.contact.Contact;

public record ContactResponse(
        String id,
        String nome,
        String chavePix,
        String banco,
        List<String> nomesAlternativos,
        Instant criadoEm,
        Instant usadoPorUltimoEm) {

    public static ContactResponse from(Contact c) {
        return new ContactResponse(
                c.id == null ? null : c.id.toHexString(),
                c.nome,
                c.chavePix,
                c.banco,
                c.nomesAlternativos,
                c.criadoEm,
                c.usadoPorUltimoEm);
    }
}
