package controle.api.back_end.model.usuario;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Pronome de tratamento preferido pelo usuário, usado para personalizar
 * e-mails automatizados e demais comunicações. Quando o valor for
 * {@link #PERSONALIZADO}, o texto livre deve ser informado em
 * {@code Usuario.pronomePersonalizado}.
 * <p>
 * Assim como {@link GeneroUsuario}, os nomes das constantes são ASCII;
 * a descrição acentuada é exposta via {@link #getDescricao()}.
 */
public enum Pronome {
    ELA_DELA("Ela / Dela"),
    ELE_DELE("Ele / Dele"),
    ELU_DELU("Elu / Delu"),
    APENAS_NOME("Usar apenas meu nome"),
    PREFIRO_NAO_INFORMAR("Prefiro não informar"),
    PERSONALIZADO("Personalizado");

    private final String descricao;

    Pronome(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static Pronome fromValor(String valor) {
        if (valor == null) {
            return null;
        }
        for (Pronome pronome : values()) {
            if (pronome.name().equalsIgnoreCase(valor) || pronome.descricao.equalsIgnoreCase(valor)) {
                return pronome;
            }
        }
        throw new IllegalArgumentException("Pronome inválido: " + valor);
    }
}

