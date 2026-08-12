package br.edu.ufrj.tcc.user;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Chave Pix pertencente a um {@link User}. Um usuário pode ter várias;
 * o valor da chave é globalmente único no arranjo Pix.
 */
@Entity
@Table(name = "pix_keys")
public class PixKey extends PanacheEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public PixKeyType tipo;

    @Column(nullable = false, unique = true)
    public String valor;

    @Column(nullable = false)
    public Instant criadoEm = Instant.now();

    @ManyToOne(optional = false)
    @JsonIgnore
    public User user;

    public PixKey() {
    }

    public PixKey(PixKeyType tipo, String valor, User user) {
        this.tipo = tipo;
        this.valor = valor;
        this.user = user;
    }

    public static PixKey findByValor(String valor) {
        return find("valor", valor).firstResult();
    }
}
