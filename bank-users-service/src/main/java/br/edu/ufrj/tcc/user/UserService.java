package br.edu.ufrj.tcc.user;

import java.math.BigDecimal;
import java.util.List;

import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.user.dto.AddPixKeyRequest;
import br.edu.ufrj.tcc.user.dto.CreateUserRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserService {

    public List<User> listarTodos() {
        return User.listAll();
    }

    public User buscarPorId(Long id) {
        User user = User.findById(id);
        if (user == null) {
            throw BusinessException.notFound("Usuário " + id + " não encontrado.");
        }
        return user;
    }

    public BigDecimal saldoDe(Long id) {
        return buscarPorId(id).saldo;
    }

    @Transactional
    public User criar(CreateUserRequest req) {
        if (User.findByEmail(req.email()) != null) {
            throw BusinessException.conflict("Já existe um usuário com o e-mail " + req.email() + ".");
        }
        BigDecimal saldo = req.saldoInicial() == null ? BigDecimal.ZERO : req.saldoInicial();
        User user = new User(req.nome(), req.email(), req.senha(), saldo);
        user.persist();
        return user;
    }

    @Transactional
    public PixKey adicionarChavePix(Long userId, AddPixKeyRequest req) {
        User user = buscarPorId(userId);
        if (PixKey.findByValor(req.valor()) != null) {
            throw BusinessException.conflict("A chave pix '" + req.valor() + "' já está cadastrada.");
        }
        PixKey chave = new PixKey(req.tipo(), req.valor(), user);
        user.chavesPix.add(chave);
        chave.persist();
        return chave;
    }

    public List<PixKey> chavesDe(Long userId) {
        return buscarPorId(userId).chavesPix;
    }

    @Transactional
    public void removerChavePix(Long userId, Long chaveId) {
        PixKey chave = PixKey.findById(chaveId);
        if (chave == null || chave.user == null || !chave.user.id.equals(userId)) {
            throw BusinessException.notFound("Chave pix " + chaveId + " não encontrada para o usuário " + userId + ".");
        }
        chave.delete();
    }
}
