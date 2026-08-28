package controle.api.back_end.service;

import controle.api.back_end.dto.usuario.mapper.UsuarioMappper;
import controle.api.back_end.exception.ContentTypeException;
import controle.api.back_end.exception.DadosInvalidosException;
import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.exception.MenorDeIdadeException;
import controle.api.back_end.exception.SenhasNaoCoincidemException;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Pronome;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.categoria.CategoriaUsuarioRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracoesService configuracoesService;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ConfiguracoesService configuracoesService,
                          EventoFinanceiroRepository eventoFinanceiroRepository,
                          EventoInstituicaoRepository eventoInstituicaoRepository,
                          InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                          CategoriaUsuarioRepository categoriaUsuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.configuracoesService = configuracoesService;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
    }

    public Page<Usuario> getUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    public Usuario getUsuarioById(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                                "Usuario de id: %s não encontrado".formatted(id)
                        )
                );
    }

    public BigDecimal getSaldoByUsuario(UUID userId) {
        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() ->
                new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado"
                        .formatted(userId)
                )
        );
        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findEventoFinanceiroByUsuario(usuario);

        BigDecimal saldo = BigDecimal.ZERO;

        for (EventoFinanceiro evento : eventosFinanceiros) {
            List<EventoInstituicao> instituicoes = eventoInstituicaoRepository
                    .findEventoInstituicaoByEventoFinanceiro_Id(evento.getId());

            if (instituicoes == null || instituicoes.isEmpty()) {
                saldo = InstituicaoService.getSaldo(saldo, evento);
                continue;
            }

            for (EventoInstituicao ei : instituicoes) {
                saldo = InstituicaoService.getSaldoPorMovimento(saldo, evento, ei);
            }
        }
        return saldo;
    }

    public Usuario createUsuario(Usuario entity) {
        //VALIDAÇÃO DE IDADE
        if (ageValidation(entity.getDataNascimento()) == false) {
            throw new MenorDeIdadeException("Usuario menor de idade");
        }
        if (usuarioRepository.findByEmail(entity.getEmail()).isPresent()) {
            throw new EntidadeJaExisteException(
                    "Já existe um usuário cadastrado com o email: %s".formatted(entity.getEmail())
            );
        }
        validatePronome(entity.getPronome(), entity.getPronomePersonalizado());
        return usuarioRepository.save(entity);
    }

    public Usuario LoginUsuario(Usuario login) {
        Usuario usuarioEncontrado = usuarioRepository.findByEmail(login.getEmail())
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Usuario de email: %s não encontrado".formatted(login.getEmail())
                ));

        if (!usuarioEncontrado.getSenha().equals(login.getSenha())) {
            throw new EntidadeNaoEncontradaException(
                    "Usuario de email: %s e senha informada não encontrado".formatted(login.getEmail())
            );
        }
        return usuarioEncontrado;
    }

    public Usuario editUsuario(UUID id, Usuario entity) {
        //VALIDAÇÃO DE IDADE
        if (entity.getDataNascimento() != null) {
            if (ageValidation(entity.getDataNascimento()) == false) {
                throw new MenorDeIdadeException("Data para adicionado é menor que 18 anos");
            }
        }

        Usuario userAtual = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                                "Usuario com o id: %s para editar não encontrado"
                                        .formatted(id)
                        )
                );

        if (entity.getEmail() != null && !entity.getEmail().equalsIgnoreCase(userAtual.getEmail())) {
            usuarioRepository.findByEmail(entity.getEmail()).ifPresent(outroUsuario -> {
                if (!outroUsuario.getId().equals(id)) {
                    throw new EntidadeJaExisteException(
                            "Já existe um usuário cadastrado com o email: %s".formatted(entity.getEmail())
                    );
                }
            });
        }

        Usuario edit = UsuarioMappper.toEdit(entity, userAtual);

        validatePronome(edit.getPronome(), edit.getPronomePersonalizado());

        return usuarioRepository.save(edit);
    }

    private void validatePronome(Pronome pronome, String pronomePersonalizado) {
        if (pronome == Pronome.PERSONALIZADO
                && (pronomePersonalizado == null || pronomePersonalizado.isBlank())) {
            throw new DadosInvalidosException(
                    "É necessário informar o texto do pronome personalizado quando a opção 'Personalizado' é escolhida"
            );
        }
    }

    public Boolean ageValidation(LocalDate dataNascimento) {
        LocalDate hoje = LocalDate.now();

        LocalDate dataMaioridade = hoje.minusYears(18);
        if (dataNascimento.isBefore(dataMaioridade) || dataNascimento.isEqual(dataMaioridade)) {
            System.out.println("Maior de 18 anos.");
            return true;
        }
        return false;
    }

    public void createConfiguracao(Usuario usuario) {
        Configuracoes configuracoes = new Configuracoes();
        configuracoes.setUsuario(usuario);
        configuracoes.setInicioMesFiscal(1);
        configuracoes.setUltimaAtualizacao(LocalDate.now());
        configuracoesService.createConfiguracao(configuracoes, usuario.getId());
    }

    public Double getXpByUserId(UUID user_id) {
        if (!usuarioRepository.existsById(user_id)) {
            throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado"
                    .formatted(user_id));
        }
        // Buscar registros do usuário
        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(user_id);
        int qtdRegistros = eventosFinanceiros.size();

        // Buscar instituições do usuário
        List<InstituicaoUsuario> instituicoes = instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(user_id);
        int qtdInstituicoes = instituicoes.size();

        List<CategoriaUsuario> categorias = categoriaUsuarioRepository.findAllByUsuario_IdAndIsAtivoIsTrue(user_id);
        int qtdCategorias = categorias.size();

        double xp = 0.0;
        xp += ((double) qtdRegistros / 10) * 100;
        xp += qtdInstituicoes * 100;
        xp += qtdCategorias * 50;

        return xp;
    }

    public Usuario getUsuario(UUID userId) {
        return usuarioRepository.findById(userId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Usuário de id: %s não encontrado."
                                        .formatted(userId)
                        )
                );
    }

    public void deleteUserbyId(UUID userId) {
        Usuario usuario = getUsuario(userId);
        usuario.setIsAtivo(false);
    }

    public Usuario activateUsuario(UUID userId) {
        Usuario usuario = getUsuario(userId);
        usuario.setIsAtivo(true);
        return usuario;
    }

    public Usuario editSenhaByUserId(UUID userId, String novaSenha, String antigaSenha) {
        Usuario usuario = getUsuario(userId);

        if (!usuario.getSenha().equals(antigaSenha)) {
            throw new SenhasNaoCoincidemException();
        }
        usuario.setSenha(novaSenha);
        return usuarioRepository.save(usuario);
    }

    public Usuario uploadImagemUsuario(UUID id, MultipartFile file) {
        Usuario usuario = getUsuario(id);

        try {
            // Usa ~/.myfinance/uploads/user_images/ para garantir permissao de escrita
            // em qualquer ambiente (dev, .exe instalado, etc.)
            String pastaDestino = System.getProperty("user.home")
                    + File.separator + ".myfinance"
                    + File.separator + "uploads"
                    + File.separator + "user_images"
                    + File.separator;
            File diretorio = new File(pastaDestino);
            if (!diretorio.exists()) {
                diretorio.mkdirs();
            }
            String contentType = file.getContentType();
            String extensao = "";

            if (contentType != null) {
                switch (contentType) {
                    case "image/jpeg":
                        extensao = "jpg";
                        break;
                    case "image/png":
                        extensao = "png";
                        break;
                    case "image/gif":
                        extensao = "gif";
                        break;
                    default:
                        throw new ContentTypeException("Extensão de imagem não aceita");
                }
            }

            String nomeArquivo = id.toString() + "_" + usuario.getSobrenome() +
                    "." + extensao;

            Path caminhoArquivo = Paths.get(pastaDestino, nomeArquivo);

            Files.copy(file.getInputStream(), caminhoArquivo, StandardCopyOption.REPLACE_EXISTING);

            usuario.setImagem("/uploads/user_images/" + nomeArquivo);
            return usuarioRepository.save(usuario);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar imagem: " + e.getMessage());
        }
    }
}