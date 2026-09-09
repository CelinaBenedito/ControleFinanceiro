package controle.api.back_end.dto.instituicao.out;

import controle.api.back_end.model.usuario.GeneroUsuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class InstituicaoUsuarioResponseDTO {

    private Integer id;
    private UsuarioInstituicaoDTO usuario;
    private InstituicaoUsuarioDTO intituicao;
    private Boolean isAtivo;
    private LocalDateTime ultimaAtualizacao;

    public static class UsuarioInstituicaoDTO{
        private UUID id;
        private String nome;
        private String sobrenome;
        private LocalDate dataNascimento;
        private GeneroUsuario genero;
        private String email;

        public UUID getId() {
            return id;

        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getSobrenome() {
            return sobrenome;
        }

        public void setSobrenome(String sobrenome) {
            this.sobrenome = sobrenome;
        }

        public LocalDate getDataNascimento() {
            return dataNascimento;
        }

        public void setDataNascimento(LocalDate dataNascimento) {
            this.dataNascimento = dataNascimento;
        }

        public GeneroUsuario getGenero() {
            return genero;
        }

        public void setGenero(GeneroUsuario genero) {
            this.genero = genero;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class InstituicaoUsuarioDTO{
        private Integer id;
        private String nome;
        private Boolean isVoucher;
        private java.math.BigDecimal limiteCredito;
        private Double taxaJuros;
        private java.util.Set<controle.api.back_end.model.eventoFinanceiro.TipoMovimento> tiposAceitos;
        private Integer diaVencimentoFatura;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public Boolean getIsVoucher() {
            return isVoucher;
        }

        public void setIsVoucher(Boolean isVoucher) {
            this.isVoucher = isVoucher;
        }

        public java.math.BigDecimal getLimiteCredito() {
            return limiteCredito;
        }

        public void setLimiteCredito(java.math.BigDecimal limiteCredito) {
            this.limiteCredito = limiteCredito;
        }

        public Double getTaxaJuros() {
            return taxaJuros;
        }

        public void setTaxaJuros(Double taxaJuros) {
            this.taxaJuros = taxaJuros;
        }

        public java.util.Set<controle.api.back_end.model.eventoFinanceiro.TipoMovimento> getTiposAceitos() {
            return tiposAceitos;
        }

        public void setTiposAceitos(java.util.Set<controle.api.back_end.model.eventoFinanceiro.TipoMovimento> tiposAceitos) {
            this.tiposAceitos = tiposAceitos;
        }

        public Integer getDiaVencimentoFatura() {
            return diaVencimentoFatura;
        }

        public void setDiaVencimentoFatura(Integer diaVencimentoFatura) {
            this.diaVencimentoFatura = diaVencimentoFatura;
        }
    }

    public InstituicaoUsuarioResponseDTO(Integer id, UsuarioInstituicaoDTO usuario, InstituicaoUsuarioDTO intituicao) {
        this.id = id;
        this.usuario = usuario;
        this.intituicao = intituicao;
    }

    public InstituicaoUsuarioResponseDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UsuarioInstituicaoDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioInstituicaoDTO usuario) {
        this.usuario = usuario;
    }

    public InstituicaoUsuarioDTO getIntituicao() {
        return intituicao;
    }

    public void setIntituicao(InstituicaoUsuarioDTO intituicao) {
        this.intituicao = intituicao;
    }

    public Boolean getAtivo() {
        return isAtivo;
    }

    public void setAtivo(Boolean ativo) {
        isAtivo = ativo;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }
}
