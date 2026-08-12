package br.edu.ufrj.tcc.user;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import br.edu.ufrj.tcc.user.dto.AddPixKeyRequest;
import br.edu.ufrj.tcc.user.dto.CreateUserRequest;
import br.edu.ufrj.tcc.user.dto.PixKeyResponse;
import br.edu.ufrj.tcc.user.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserService service;

    public UserResource(UserService service) {
        this.service = service;
    }

    @GET
    public List<UserResponse> listar() {
        return service.listarTodos().stream().map(UserResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public UserResponse buscar(@PathParam("id") Long id) {
        return UserResponse.from(service.buscarPorId(id));
    }

    @GET
    @Path("/{id}/saldo")
    public Map<String, BigDecimal> saldo(@PathParam("id") Long id) {
        return Map.of("saldo", service.saldoDe(id));
    }

    @POST
    public Response criar(@Valid CreateUserRequest req) {
        User user = service.criar(req);
        return Response.status(Response.Status.CREATED).entity(UserResponse.from(user)).build();
    }

    @GET
    @Path("/{id}/pix-keys")
    public List<PixKeyResponse> chaves(@PathParam("id") Long id) {
        return service.chavesDe(id).stream().map(PixKeyResponse::from).toList();
    }

    @POST
    @Path("/{id}/pix-keys")
    public Response adicionarChave(@PathParam("id") Long id, @Valid AddPixKeyRequest req) {
        PixKey chave = service.adicionarChavePix(id, req);
        return Response.status(Response.Status.CREATED).entity(PixKeyResponse.from(chave)).build();
    }

    @DELETE
    @Path("/{id}/pix-keys/{chaveId}")
    public Response removerChave(@PathParam("id") Long id, @PathParam("chaveId") Long chaveId) {
        service.removerChavePix(id, chaveId);
        return Response.noContent().build();
    }
}
