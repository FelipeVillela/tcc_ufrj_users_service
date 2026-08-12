package br.edu.ufrj.tcc.contact;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

/**
 * Contato recente de um usuário, guardado no MongoDB.
 *
 * Diferente dos {@link br.edu.ufrj.tcc.user.User}, um contato NÃO tem
 * relação (FK) com nenhum usuário do banco: é um destinatário Pix arbitrário,
 * que pode inclusive estar em outra instituição. Por isso guardamos um
 * snapshot dos dados (nome, chave, banco) no momento do envio.
 *
 * {@code nomesAlternativos} são palavras-chave livres usadas para localizar o
 * contato por linguagem natural — ex.: "mãe", "irmão", "amigo",
 * "colega de trabalho", "chefe", "vizinho", ou um apelido. É a informação
 * que o chat/IA usa para resolver "manda um pix pra minha mãe".
 */
@MongoEntity(collection = "contacts")
public class Contact extends PanacheMongoEntity {

    /** Id do usuário do banco (login) dono desta lista de contatos. */
    public Long ownerUserId;

    public String nome;

    public String chavePix;

    /** Instituição de destino (permite Pix para outros bancos). */
    public String banco;

    public List<String> nomesAlternativos = new ArrayList<>();

    public Instant criadoEm = Instant.now();

    public Instant usadoPorUltimoEm = Instant.now();
}
