package br.com.fatec.tcc.dto;

/**
 * Payload para registrar uma denúncia.
 *
 * - categoria  : nome do enum CategoriaDenuncia (ex.: "DIRECAO_PERIGOSA")
 * - descricao  : texto do ocorrido
 * - alvoEmail  : (só motorista) e-mail do passageiro denunciado; ignorado se todaCarona = true
 * - todaCarona : (só motorista) true = gera denúncia para todos os passageiros confirmados
 *
 * Quando quem denuncia é passageiro, alvoEmail/todaCarona são ignorados — o alvo
 * é sempre o motorista da carona.
 */
public record DenunciaRequestDTO(
    String categoria,
    String descricao,
    String alvoEmail,
    Boolean todaCarona
) {}
