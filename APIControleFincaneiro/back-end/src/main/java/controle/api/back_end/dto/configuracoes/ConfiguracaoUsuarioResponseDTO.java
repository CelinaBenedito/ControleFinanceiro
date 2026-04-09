package controle.api.back_end.dto.configuracoes;

import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.usuario.UsuarioSexo;

import java.time.LocalDate;
import java.util.UUID;

//DTO que retorna as configurações em conjunto com o usuário e informações mais detalhadas.
public class ConfiguracaoUsuarioResponseDTO {
    private UUID id;
    private ConfiguracaoUsuarioDTO usuario;
    private Integer inicioMesFiscal;
    private LocalDate ultimaAtualizacao;
    private Double limiteDesejadoMensal;
    private LimiteInstituicaoDTO limiteInstituicao;
    private LimiteCategoriaDTO limitePorCategoria;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConfiguracaoUsuarioDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(ConfiguracaoUsuarioDTO usuario) {
        this.usuario = usuario;
    }

    public Integer getInicioMesFiscal() {
        return inicioMesFiscal;
    }

    public void setInicioMesFiscal(Integer inicioMesFiscal) {
        this.inicioMesFiscal = inicioMesFiscal;
    }

    public LocalDate getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public void setUltimaAtualizacao(LocalDate ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    public Double getLimiteDesejadoMensal() {
        return limiteDesejadoMensal;
    }

    public void setLimiteDesejadoMensal(Double limiteDesejadoMensal) {
        this.limiteDesejadoMensal = limiteDesejadoMensal;
    }

    public LimiteInstituicaoDTO getLimiteInstituicao() {
        return limiteInstituicao;
    }

    public void setLimiteInstituicao(LimiteInstituicaoDTO limiteInstituicao) {
        this.limiteInstituicao = limiteInstituicao;
    }

    public LimiteCategoriaDTO getLimitePorCategoria() {
        return limitePorCategoria;
    }

    public void setLimitePorCategoria(LimiteCategoriaDTO limitePorCategoria) {
        this.limitePorCategoria = limitePorCategoria;
    }

    public static class LimiteCategoriaDTO{
        private CategoriaUsuarioDTO categoriaUsuario;
        private Double limiteDesejado;

        public static class CategoriaUsuarioDTO{
            private Integer id;
            private UsuarioDTO usuario;
            private CategoriaDTO categoria;

            public static class UsuarioDTO{
                private UUID id;
                private String nome;
                private String sobrenome;
                private LocalDate dataNascimento;
                private UsuarioSexo sexo;

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

                public UsuarioSexo getSexo() {
                    return sexo;
                }

                public void setSexo(UsuarioSexo sexo) {
                    this.sexo = sexo;
                }
            }
            public static class CategoriaDTO{
                private Integer id;
                private String titulo;

                public Integer getId() {
                    return id;
                }

                public void setId(Integer id) {
                    this.id = id;
                }

                public String getTitulo() {
                    return titulo;
                }

                public void setTitulo(String titulo) {
                    this.titulo = titulo;
                }
            }

            public Integer getId() {
                return id;
            }

            public void setId(Integer id) {
                this.id = id;
            }

            public UsuarioDTO getUsuario() {
                return usuario;
            }

            public void setUsuario(UsuarioDTO usuario) {
                this.usuario = usuario;
            }

            public CategoriaDTO getCategoria() {
                return categoria;
            }

            public void setCategoria(CategoriaDTO categoria) {
                this.categoria = categoria;
            }
        }
    }
    public static class LimiteInstituicaoDTO {

        private InstituicaoUsuarioDTO instituicaoUsuario;
        private Double limiteDesejado;

        public static class InstituicaoUsuarioDTO {

            private Integer id;
            private UsuarioDTO usuario;
            private InstituicaoDTO instituicao;

            public static class UsuarioDTO{
                private UUID id;
                private String nome;
                private String sobrenome;
                private LocalDate dataNascimento;
                private UsuarioSexo sexo;

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

                public UsuarioSexo getSexo() {
                    return sexo;
                }

                public void setSexo(UsuarioSexo sexo) {
                    this.sexo = sexo;
                }
            }

            public static class InstituicaoDTO{
                private Integer id;
                private String nome;

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
            }

            public Integer getId() {
                return id;
            }

            public void setId(Integer id) {
                this.id = id;
            }

            public UsuarioDTO getUsuario() {
                return usuario;
            }

            public void setUsuario(UsuarioDTO usuario) {
                this.usuario = usuario;
            }

            public InstituicaoDTO getInstituicao() {
                return instituicao;
            }

            public void setInstituicao(InstituicaoDTO instituicao) {
                this.instituicao = instituicao;
            }
        }

        public InstituicaoUsuarioDTO getInstituicaoUsuario() {
            return instituicaoUsuario;
        }

        public void setInstituicaoUsuario(InstituicaoUsuarioDTO instituicaoUsuario) {
            this.instituicaoUsuario = instituicaoUsuario;
        }

        public Double getLimiteDesejado() {
            return limiteDesejado;
        }

        public void setLimiteDesejado(Double limiteDesejado) {
            this.limiteDesejado = limiteDesejado;
        }
    }
    public static class ConfiguracaoUsuarioDTO{
        private UUID id;
        private String nome;
        private String sobrenome;
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
