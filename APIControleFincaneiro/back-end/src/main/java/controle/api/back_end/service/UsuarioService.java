package controle.api.back_end.service;

import controle.api.back_end.dto.usuario.mapper.UsuarioMappper;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.exception.MenorDeIdadeException;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> getUsuarios(){
        return usuarioRepository.findAll();
    }

    public Usuario getUsuarioById(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Usuario de id: %s não encontrado".
                                formatted(id)
                        )
                );
    }

    public Usuario createUsuario(Usuario entity) {
        //VALIDAÇÃO DE IDADE
        if(ageValidation(entity.getDataNascimento()) == false){
            throw new MenorDeIdadeException("Usuario menor de idade");
        }

        return usuarioRepository.save(entity);
    }

    public Usuario LoginUsuario(Usuario login) {
        List<Usuario> usuarioByEmailAndSenha = usuarioRepository.
                findUsuarioByEmailAndSenha(
                login.getEmail(),
                        login.getSenha()
        );
               if(usuarioByEmailAndSenha.isEmpty()){
                   throw new EntidadeNaoEncontradaException(
                           "Usuario de email: %s e senha: %s não encontrado".
                           formatted(
                                   login.getEmail(),
                                   login.getSenha()
                           )
                   );
               }
               return usuarioByEmailAndSenha.getFirst();
    }

    public Usuario editUsuario(UUID id,Usuario entity) {
        //VALIDAÇÃO DE IDADE
        if(entity.getDataNascimento()!= null){
            if(ageValidation(entity.getDataNascimento()) == false){
                throw new MenorDeIdadeException("Data para adicionado é menor que 18 anos");
            }
        }

        Usuario userAtual = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Usuario com o id: %s para editar não encontrado"
                                .formatted(id)
                        )
                );

        Usuario edit = UsuarioMappper.toEdit(entity, userAtual);

        return usuarioRepository.save(edit);
    }

    public Boolean ageValidation(LocalDate dataNascimento){
        LocalDate hoje = LocalDate.now();

        LocalDate dataMaioridade = hoje.minusYears(18);
        if (dataNascimento.isBefore(dataMaioridade) || dataNascimento.isEqual(dataMaioridade)) {
            System.out.println("Maior de 18 anos.");
            return true;
        }
        return false;
    }
}
