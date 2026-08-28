package controle.api.back_end.model.usuario;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gênero do usuário. Os nomes das constantes são mantidos em ASCII (sem
 * acentos) para evitar problemas de codificação de caracteres na
 * serialização/desserialização JSON e no banco de dados (é isso que persiste
 * via {@code @Enumerated(EnumType.STRING)}). O texto de exibição (com
 * acentuação correta) fica em {@link #getDescricao()}, usado tanto na
 * resposta JSON (via {@link JsonValue}) quanto para leitura de entrada
 * (via {@link JsonCreator}, aceitando tanto o nome da constante quanto a
 * descrição, sem diferenciar maiúsculas/minúsculas).
 */
public enum GeneroUsuario {
    HOMEM_CIS("Homem cis"),
    HOMEM_TRANS("Homem trans"),
    MULHER_CIS("Mulher cis"),
    MULHER_TRANS("Mulher trans"),
    NAO_BINARIO("Não Binário"),
    GENDERQUEER("Genderqueer"),
    AGENERO("Agênero"),
    PREFIRO_NAO_INFORMAR("Prefiro não informar"),
    OUTRO("Outro");

    private final String descricao;

    GeneroUsuario(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static GeneroUsuario fromValor(String valor) {
        if (valor == null) {
            return null;
        }
        for (GeneroUsuario genero : values()) {
            if (genero.name().equalsIgnoreCase(valor) || genero.descricao.equalsIgnoreCase(valor)) {
                return genero;
            }
        }
        throw new IllegalArgumentException("Gênero inválido: " + valor);
    }
}

