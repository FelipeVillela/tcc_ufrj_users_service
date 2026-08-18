package br.edu.ufrj.tcc.pixdirectory.dto;

/**
 * Titular de uma chave Pix, como o app precisa exibir antes de um envio:
 * o nome de quem vai receber e a instituição de destino.
 *
 * No arranjo Pix real esses dados viriam do DICT (diretório do Banco Central);
 * aqui eles são resolvidos por um mock ({@link
 * br.edu.ufrj.tcc.pixdirectory.PixDirectoryService}).
 */
public record PixOwnerResponse(String chavePix, String nome, String banco) {
}
