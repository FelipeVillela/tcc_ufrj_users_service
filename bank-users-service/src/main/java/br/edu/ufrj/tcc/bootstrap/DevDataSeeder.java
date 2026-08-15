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
 * Popula dados de exemplo no boot, para permitir demonstrar o serviço sem
 * cadastro manual. São dois usuários, cada um com a sua lista de contatos, o
 * que permite demonstrar o login alternando entre eles.
 */
@ApplicationScoped
public class DevDataSeeder {

    private static final Logger LOG = Logger.getLogger(DevDataSeeder.class);

    private static final String EMAIL_TESTE_1 = "teste1@email.com";
    private static final String EMAIL_TESTE_2 = "teste2@email.com";
    private static final String SENHA = "senha123";

    void onStart(@Observes StartupEvent ev) {
        List<Long> usuarios = seedUsuarios();
        seedContatos(usuarios.get(0), usuarios.get(1));
    }

    /**
     * Cria os dois usuários de demonstração com saldo e chaves pix, na ordem —
     * como o Hibernate está em {@code drop-and-create}, eles saem sempre com os
     * ids 1 e 2. Retorna os ids, do Teste 1 e do Teste 2.
     */
    private List<Long> seedUsuarios() {
        return QuarkusTransaction.requiringNew().call(() -> {

            User teste1 = User.findByEmail(EMAIL_TESTE_1);
            if (teste1 == null) {
                teste1 = criarUsuario("Usuário Teste 1", EMAIL_TESTE_1, new BigDecimal("2500.00"));
                new PixKey(PixKeyType.EMAIL, EMAIL_TESTE_1, teste1).persist();
                new PixKey(PixKeyType.CPF, "09876543210", teste1).persist();
                new PixKey(PixKeyType.ALEATORIA, "a1b2c3d4-e5f6-7890-abcd-ef1234567890", teste1).persist();
            }

            User teste2 = User.findByEmail(EMAIL_TESTE_2);
            if (teste2 == null) {
                teste2 = criarUsuario("Usuário Teste 2", EMAIL_TESTE_2, new BigDecimal("980.50"));
                new PixKey(PixKeyType.EMAIL, EMAIL_TESTE_2, teste2).persist();
                new PixKey(PixKeyType.TELEFONE, "+5521999998888", teste2).persist();
            }

            LOG.infof("Seed: %d usuários.", User.count());
            return List.of(teste1.id, teste2.id);
        });
    }

    private static User criarUsuario(String nome, String email, BigDecimal saldo) {
        User user = new User(nome, email, SENHA, saldo);
        user.persist();
        return user;
    }

    /**
     * Recria no MongoDB a lista de contatos de cada usuário.
     */
    private void seedContatos(Long teste1, Long teste2) {
        if (Contact.count() > 0) {
            Contact.deleteAll();
        }
        // Contatos do Teste 1
        criarContato(teste1, "Carla Pereira", "carla.pereira@gmail.com", "Banco Azul", List.of("mãe"));
        criarContato(teste1, "Fernando Costa", "+5521988887777", "Banco Azul", List.of("tio"));
        criarContato(teste1, "João Silva", "11122233344", "Banco Roxo", List.of("irmão"));
        criarContato(teste1, "Ana Souza", "ana.souza@outlook.com", "Banco Verde", List.of("prima"));
        criarContato(teste1, "Gabriel Lima", "+5511977776666", "Banco Amarelo", List.of("tio", "padrinho"));
        criarContato(teste1, "Thiago Almeida", "thiago.almeida@empresa.com", "Banco Vermelho",
                List.of("colega de trabalho", "chefe"));
        criarContato(teste1, "Sofia Castro", "55667788900", "Banco Laranja", List.of("vizinha"));
        criarContato(teste1, "Rafaela Silva", "rafa@gmail.com", "Banco Roxo", List.of("irmã", "rafa"));
        criarContato(teste1, "Usuário Teste 2", EMAIL_TESTE_2, "Banco Azul", List.of("amigo"));

        // Contatos do Teste 2
        criarContato(teste2, "Cesar Augusto", EMAIL_TESTE_1, "Banco Azul", List.of("pai"));
        criarContato(teste2, "Marina Duarte", "marina.duarte@gmail.com", "Banco Verde", List.of("mãe"));
        criarContato(teste2, "Rodrigo Pinto", "+5521955554444", "Banco Vermelho", List.of("irmão"));
        criarContato(teste2, "Lucas Moreira", "lucas.moreira@empresa.com", "Banco Roxo", List.of("chefe"));

        LOG.infof("Seed: %d contatos criados para os usuários %d e %d.", Contact.count(), teste1, teste2);
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
