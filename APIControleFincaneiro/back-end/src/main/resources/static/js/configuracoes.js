div_alerta.style.display = 'none';

function alerta(texto) {
    div_alerta.style.display = "flex";
    conteudoAlerta.innerHTML = texto;
}

function getUsuario() {
    return JSON.parse(localStorage.getItem('usuario') || '{}');
}

function carregarConfiguracoes() {
    carregarInstituicoes();
    carregarCategorias();
    carregarConfig();
}

/* ===================== CONFIGURAÇÃO ===================== */

function carregarConfig() {
    const configId = localStorage.getItem('configId');
    if (!configId) {
        console.warn("Nenhuma configuração armazenada. Preencha e salve para criar.");
        return;
    }
    fetch(`/configuracoes/${configId}`)
        .then(res => {
            if (res.ok) return res.json();
            throw new Error("Configuração não encontrada");
        })
        .then(json => {
            if (json.inicioMesFiscal) {
                document.getElementById('mesFiscal').value = json.inicioMesFiscal;
            }
            if (json.limiteDesejadoMensal) {
                document.getElementById('ipt_limiteMensal').value = json.limiteDesejadoMensal;
            }
        })
        .catch(() => {
            localStorage.removeItem('configId');
        });
}

function salvarConfiguracoes() {
    const usuario = getUsuario();
    const configId = localStorage.getItem('configId');
    const mesFiscal = Number(document.getElementById('mesFiscal').value);
    const limiteInput = document.getElementById('ipt_limiteMensal').value;
    const limite = limiteInput ? Number(limiteInput) : null;

    if (!mesFiscal) {
        alerta(`Selecione um dia de início do mês fiscal <button onclick='div_alerta.style.display="none"'>OK</button>`);
        return;
    }

    if (configId) {
        fetch(`/configuracoes/edit/${configId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                inicioMesFiscal: mesFiscal,
                limiteDesejadoMensal: limite
            })
        }).then(res => {
            if (res.ok) {
                alerta(`Configurações salvas com sucesso! <button onclick='div_alerta.style.display="none"'>OK</button>`);
            } else {
                alerta(`Erro ao salvar configurações <button onclick='div_alerta.style.display="none"'>OK</button>`);
            }
        });
    } else {
        if (!usuario.id) {
            alerta(`Você precisa estar logado <button onclick='div_alerta.style.display="none"'>OK</button>`);
            return;
        }
        fetch('/configuracoes', {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                fkUsuario: usuario.id,
                inicioMesFiscal: mesFiscal,
                limiteDesejadoMensal: limite
            })
        }).then(res => {
            if (res.ok) return res.json();
            if (res.status === 409) throw new Error("Você já possui uma configuração. Recarregue a página.");
            throw new Error("Erro ao criar configuração");
        }).then(json => {
            localStorage.setItem('configId', json.id);
            alerta(`Configurações criadas com sucesso! <button onclick='div_alerta.style.display="none"'>OK</button>`);
        }).catch(err => {
            alerta(`${err.message} <button onclick='div_alerta.style.display="none"'>OK</button>`);
        });
    }
}

/* ===================== INSTITUIÇÕES ===================== */

function carregarInstituicoes() {
    const usuario = getUsuario();
    if (!usuario.id) return;
    fetch(`/instituicoes/usuarios/${usuario.id}`)
        .then(res => {
            if (res.status === 204) return [];
            if (!res.ok) throw new Error("Erro ao carregar instituições");
            return res.json();
        })
        .then(json => {
            const lista = document.getElementById('listaInstituicoes');
            lista.innerHTML = '';
            if (!json || json.length === 0) {
                lista.innerHTML = '<p>Nenhuma instituição vinculada.</p>';
                return;
            }
            json.forEach(inst => {
                lista.innerHTML += `
                    <div class="cardRegistro">
                        <span class="registroTitulo">${inst.nome}</span>
                        <button class="danger" onclick="removerInstituicao(${inst.id})">✖</button>
                    </div>
                `;
            });
        })
        .catch(err => console.error(err));
}

function adicionarInstituicao() {
    const usuario = getUsuario();
    if (!usuario.id) {
        alerta(`Você precisa estar logado <button onclick='div_alerta.style.display="none"'>OK</button>`);
        return;
    }
    const nome = document.getElementById('ipt_novaInstituicao').value.trim();
    if (!nome) {
        alerta(`Informe o nome da instituição <button onclick='div_alerta.style.display="none"'>OK</button>`);
        return;
    }

    fetch('/instituicoes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nome: nome })
    }).then(res => {
        if (res.ok) return res.json();
        if (res.status === 409) {
            return fetch('/instituicoes')
                .then(r => r.json())
                .then(all => {
                    const found = all.find(i => i.nome.toLowerCase() === nome.toLowerCase());
                    if (!found) throw new Error("Instituição não encontrada no sistema");
                    return found;
                });
        }
        throw new Error("Erro ao criar instituição");
    }).then(inst => {
        return fetch(`/instituicoes/${inst.id}/usuarios/${usuario.id}`, { method: 'POST' });
    }).then(res => {
        if (res.ok || res.status === 201) {
            document.getElementById('ipt_novaInstituicao').value = '';
            carregarInstituicoes();
            alerta(`Instituição adicionada com sucesso! <button onclick='div_alerta.style.display="none"'>OK</button>`);
        } else if (res.status === 409) {
            alerta(`Você já está vinculado a essa instituição <button onclick='div_alerta.style.display="none"'>OK</button>`);
        } else {
            alerta(`Erro ao vincular instituição <button onclick='div_alerta.style.display="none"'>OK</button>`);
        }
    }).catch(err => {
        alerta(`${err.message} <button onclick='div_alerta.style.display="none"'>OK</button>`);
    });
}

function removerInstituicao(instituicaoId) {
    const usuario = getUsuario();
    fetch(`/instituicoes/${instituicaoId}/usuarios/${usuario.id}`, {
        method: 'PATCH'
    }).then(res => {
        if (res.ok || res.status === 204) {
            carregarInstituicoes();
            alerta(`Instituição removida! <button onclick='div_alerta.style.display="none"'>OK</button>`);
        } else {
            alerta(`Erro ao remover instituição <button onclick='div_alerta.style.display="none"'>OK</button>`);
        }
    });
}

/* ===================== CATEGORIAS ===================== */

function carregarCategorias() {
    const usuario = getUsuario();
    if (!usuario.id) return;
    fetch(`/categorias/usuario/${usuario.id}`)
        .then(res => {
            if (res.status === 204) return [];
            if (!res.ok) throw new Error("Erro ao carregar categorias");
            return res.json();
        })
        .then(json => {
            const lista = document.getElementById('listaCategorias');
            lista.innerHTML = '';
            if (!json || json.length === 0) {
                lista.innerHTML = '<p>Nenhuma categoria vinculada.</p>';
                return;
            }
            json.forEach(cat => {
                lista.innerHTML += `
                    <div class="cardRegistro">
                        <span class="registroTitulo">${cat.categoria.titulo}</span>
                        <button class="danger" onclick="removerCategoria(${cat.categoria.id})">✖</button>
                    </div>
                `;
            });
        })
        .catch(err => console.error(err));
}

function adicionarCategoria() {
    const usuario = getUsuario();
    if (!usuario.id) {
        alerta(`Você precisa estar logado <button onclick='div_alerta.style.display="none"'>OK</button>`);
        return;
    }
    const titulo = document.getElementById('ipt_novaCategoria').value.trim();
    if (!titulo) {
        alerta(`Informe o nome da categoria <button onclick='div_alerta.style.display="none"'>OK</button>`);
        return;
    }

    fetch('/categorias', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ titulo: titulo })
    }).then(res => {
        if (res.ok) return res.json();
        if (res.status === 409) {
            return fetch('/categorias')
                .then(r => r.json())
                .then(all => {
                    const found = all.find(c => c.titulo.toLowerCase() === titulo.toLowerCase());
                    if (!found) throw new Error("Categoria não encontrada no sistema");
                    return found;
                });
        }
        throw new Error("Erro ao criar categoria");
    }).then(cat => {
        return fetch(`/categorias/${cat.id}/usuarios/${usuario.id}`, { method: 'POST' });
    }).then(res => {
        if (res.ok || res.status === 201) {
            document.getElementById('ipt_novaCategoria').value = '';
            carregarCategorias();
            alerta(`Categoria adicionada com sucesso! <button onclick='div_alerta.style.display="none"'>OK</button>`);
        } else if (res.status === 409) {
            alerta(`Você já possui essa categoria <button onclick='div_alerta.style.display="none"'>OK</button>`);
        } else {
            alerta(`Erro ao vincular categoria <button onclick='div_alerta.style.display="none"'>OK</button>`);
        }
    }).catch(err => {
        alerta(`${err.message} <button onclick='div_alerta.style.display="none"'>OK</button>`);
    });
}

function removerCategoria(categoriaId) {
    const usuario = getUsuario();
    fetch(`/categorias/${categoriaId}/usuarios/${usuario.id}`, {
        method: 'PATCH'
    }).then(res => {
        if (res.ok || res.status === 204) {
            carregarCategorias();
            alerta(`Categoria removida! <button onclick='div_alerta.style.display="none"'>OK</button>`);
        } else {
            alerta(`Erro ao remover categoria <button onclick='div_alerta.style.display="none"'>OK</button>`);
        }
    });
}
