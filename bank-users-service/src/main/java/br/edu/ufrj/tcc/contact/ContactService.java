package br.edu.ufrj.tcc.contact;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bson.Document;

import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.contact.dto.SaveContactRequest;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ContactService {

    /** Campo de busca aceito, espelhando consulta_banco_de_dados do worker Python. */
    public enum Campo {
        NOME("nome"),
        NOME_ALTERNATIVO("nomesAlternativos");

        final String mongoField;

        Campo(String mongoField) {
            this.mongoField = mongoField;
        }

        static Campo of(String valor) {
            if (valor == null) {
                return NOME;
            }
            return switch (valor.toLowerCase()) {
                case "nome" -> NOME;
                case "nomealternativo", "nomesalternativos" -> NOME_ALTERNATIVO;
                default -> throw new BusinessException(jakarta.ws.rs.core.Response.Status.BAD_REQUEST,
                        "Parâmetro de busca inválido. Use 'nome' ou 'nomeAlternativo'.");
            };
        }
    }

    /** Contatos recentes do usuário, do mais recente para o mais antigo. */
    public List<Contact> listarRecentes(Long ownerUserId) {
        return Contact.find("ownerUserId = ?1", Sort.by("usadoPorUltimoEm", Sort.Direction.Descending), ownerUserId)
                .list();
    }

    /**
     * Busca por substring (case-insensitive) em 'nome' ou em qualquer palavra
     * de 'nomesAlternativos', dentro dos contatos do próprio usuário. Equivale ao
     * consulta_banco_de_dados(termo, campo) do worker, agora por usuário.
     */
    public List<Contact> buscar(Long ownerUserId, String termo, String campoStr) {
        Campo campo = Campo.of(campoStr);
        if (termo == null || termo.isBlank()) {
            return listarRecentes(ownerUserId);
        }
        Document regex = new Document("$regex", escapeRegex(termo)).append("$options", "i");
        Document filtro = new Document("ownerUserId", ownerUserId).append(campo.mongoField, regex);
        return Contact.find(filtro).list();
    }

    /**
     * Salva o destinatário na lista de contatos ao enviar um Pix (com a
     * permissão do usuário). A chave Pix é normalizada (trim + caixa baixa)
     * para deduplicação. Se já existir um contato com a mesma chave, atualiza o
     * "usado por último", o nome/banco e agrega novas palavras-chave.
     */
    public Contact salvarAoEnviar(Long ownerUserId, SaveContactRequest req) {
        String chavePix = normalizarChave(req.chavePix());
        Contact existente = Contact
                .find("ownerUserId = ?1 and chavePix = ?2", ownerUserId, chavePix)
                .firstResult();

        if (existente != null) {
            existente.nome = req.nome();
            if (req.banco() != null) {
                existente.banco = req.banco();
            }
            existente.nomesAlternativos = mesclarNomesAlternativos(existente.nomesAlternativos, req.nomesAlternativos());
            existente.usadoPorUltimoEm = Instant.now();
            existente.update();
            return existente;
        }

        Contact novo = new Contact();
        novo.ownerUserId = ownerUserId;
        novo.nome = req.nome();
        novo.chavePix = chavePix;
        novo.banco = req.banco();
        novo.nomesAlternativos = req.nomesAlternativos() == null ? new ArrayList<>() : new ArrayList<>(req.nomesAlternativos());
        novo.persist();
        return novo;
    }

    public void remover(Long ownerUserId, String contactId) {
        long removidos = Contact.delete("ownerUserId = ?1 and _id = ?2", ownerUserId, new org.bson.types.ObjectId(contactId));
        if (removidos == 0) {
            throw BusinessException.notFound("Contato " + contactId + " não encontrado para o usuário " + ownerUserId + ".");
        }
    }

    /**
     * Normaliza a chave Pix para a deduplicação: remove espaços nas bordas e
     * aplica caixa baixa, de modo que o mesmo contato não seja duplicado quando
     * a chave é digitada com espaços ou em caixa diferente (ex.: e-mails).
     * Chaves numéricas (CPF, telefone) não são afetadas pela caixa; a
     * padronização de formato de telefone (com/sem +55) fica fora de escopo por
     * risco de mesclagem indevida.
     */
    private static String normalizarChave(String chavePix) {
        return chavePix == null ? null : chavePix.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> mesclarNomesAlternativos(List<String> atuais, List<String> novos) {
        Set<String> merged = new LinkedHashSet<>();
        if (atuais != null) {
            merged.addAll(atuais);
        }
        if (novos != null) {
            merged.addAll(novos);
        }
        return new ArrayList<>(merged);
    }

    /** Escapa metacaracteres de regex para tratar o termo como texto literal. */
    private static String escapeRegex(String termo) {
        return termo.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
    }
}
