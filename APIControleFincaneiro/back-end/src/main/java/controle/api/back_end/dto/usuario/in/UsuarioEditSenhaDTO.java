package controle.api.back_end.dto.usuario.in;

public class UsuarioEditSenhaDTO {
    private String novaSenha;
    private String antigaSenha;

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getAntigaSenha() {
        return antigaSenha;
    }

    public void setAntigaSenha(String antigaSenha) {
        this.antigaSenha = antigaSenha;
    }
}
