package br.edu.ufrj.tcc.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Usuário do banco (login + saldo). É a entidade "de login" da arquitetura:
 * os contatos da lista de contatos NÃO se relacionam com esta tabela.
 * Um usuário pode ter várias chaves Pix ({@link PixKey}).
 */
@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @Column(nullable = false)
    public String nome;

    /** Usado como login. Único no banco. */
    @Column(nullable = false, unique = true)
    public String email;

    /** Guardado apenas para autenticação; hashing/segurança fora do escopo deste serviço. */
    @Column(nullable = false)
    public String senha;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal saldo = BigDecimal.ZERO;

    @Column(nullable = false)
    public Instant criadoEm = Instant.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<PixKey> chavesPix = new ArrayList<>();

    public User() {
    }

    public User(String nome, String email, String senha, BigDecimal saldo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.saldo = saldo == null ? BigDecimal.ZERO : saldo;
    }

    public static User findByEmail(String email) {
        return find("email", email).firstResult();
    }
}
