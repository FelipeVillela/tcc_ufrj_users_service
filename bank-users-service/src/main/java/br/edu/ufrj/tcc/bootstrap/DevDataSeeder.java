package br.edu.ufrj.tcc.bootstrap;

import java.math.BigDecimal;
import java.util.List;

import org.jboss.logging.Logger;

import br.edu.ufrj.tcc.contact.Contact;
import br.edu.ufrj.tcc.user.PixKey;
import br.edu.ufrj.tcc.user.PixKeyType;
import br.edu.ufrj.tcc.user.User;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Popula dados de exemplo no boot quando os bancos estão vazios (idempotente),
 * para permitir demonstrar o serviço sem cadastro manual.
 */
@ApplicationScoped
public class DevDataSeeder {

    private static final Logger LOG = Logger.getLogger(DevDataSeeder.class);

    void onStart(@Observes StartupEvent ev) {
        Long ownerId = seedUsuarios();
        seedContatos(ownerId);
    }

    /** Cria usuários com saldo e chaves pix. Retorna o id do usuário principal. */
    private Long seedUsuarios() {
        return QuarkusTransaction.requiringNew().call(() -> {
            User existente = User.<User>findAll().firstResult();
            if (existente != null) {
                return existente.id;
            }

            User felipe = new User(
                "Felipe Augusto", 
                "felipe@poli.ufrj.br", 
                "senha123", 
                new BigDecimal("2500.00")
            );
            felipe.persist();
            new PixKey(PixKeyType.EMAIL, "felipe@poli.ufrj.br", felipe).persist();
            new PixKey(PixKeyType.CPF, "09876543210", felipe).persist();
            new PixKey(PixKeyType.ALEATORIA, "a1b2c3d4-e5f6-7890-abcd-ef1234567890", felipe).persist();

            LOG.infof("Seed: %d usuários criados.", User.count());
            return felipe.id;
        });
    }

    /** Cria a lista de contatos recentes do usuário principal no MongoDB. */
    private void seedContatos(Long ownerId) {
        if (Contact.count() > 0) {
            return;
        }
        criarContato(ownerId, "Carla Pereira", "carla.pereira@gmail.com", "Banco do Brasil", List.of("mãe"));
        criarContato(ownerId, "Fernando Costa", "+5521988887777", "Banco do Brasil", List.of("pai"));
        criarContato(ownerId, "João Silva", "11122233344", "Nubank", List.of("irmão"));
        criarContato(ownerId, "Ana Souza", "ana.souza@outlook.com", "Itaú", List.of("prima"));
        criarContato(ownerId, "Gabriel Lima", "+5511977776666", "Caixa", List.of("tio", "padrinho"));
        criarContato(ownerId, "Thiago Almeida", "thiago.almeida@empresa.com", "Bradesco",
                List.of("colega de trabalho", "chefe"));
        criarContato(ownerId, "Sofia Castro", "55667788900", "Inter", List.of("vizinha"));
        criarContato(ownerId, "Rafaela Fernandes", "rafa@gmail.com", "Nubank", List.of("irmã", "rafa"));
        LOG.infof("Seed: %d contatos criados para o usuário %d.", Contact.count(), ownerId);
    }

    private void criarContato(Long ownerId, String nome, String chavePix, String banco, List<String> nomesAlternativos) {
        Contact c = new Contact();
        c.ownerUserId = ownerId;
        c.nome = nome;
        c.chavePix = chavePix;
        c.banco = banco;
        c.nomesAlternativos = new java.util.ArrayList<>(nomesAlternativos);
        c.persist();
    }
}
