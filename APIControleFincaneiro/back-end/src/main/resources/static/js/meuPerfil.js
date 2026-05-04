(function () {
    const API = "http://localhost:8080";
    const usuarioLogado = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
    const userId = usuarioLogado?.id;

    // ── helpers ──────────────────────────────────────────────────
    function mostrarAlerta(texto) {
        const div = document.getElementById("div_alerta");
        const conteudo = document.getElementById("conteudoAlerta");
        if (!div || !conteudo) return;
        conteudo.textContent = texto;
        div.style.display = "flex";
        setTimeout(() => { div.style.display = "none"; }, 3000);
    }

    function mostrarConfirmacao(texto, onConfirm) {
        const div = document.getElementById("div_alerta");
        const conteudo = document.getElementById("conteudoAlerta");
        if (!div || !conteudo) { onConfirm(); return; }
        conteudo.innerHTML = "";
        const msg = document.createElement("span");
        msg.textContent = texto;
        const divBtns = document.createElement("div");
        divBtns.style.cssText = "display:flex;gap:8px;margin-top:10px;";
        const btnOk = document.createElement("button");
        btnOk.textContent = "Confirmar";
        btnOk.addEventListener("click", () => { div.style.display = "none"; onConfirm(); });
        const btnNo = document.createElement("button");
        btnNo.textContent = "Cancelar";
        btnNo.style.background = "#888";
        btnNo.addEventListener("click", () => { div.style.display = "none"; });
        divBtns.appendChild(btnNo);
        divBtns.appendChild(btnOk);
        conteudo.appendChild(msg);
        conteudo.appendChild(divBtns);
        div.style.display = "flex";
    }

    function putJson(url, payload) {
        return fetch(url, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
    }

    function formatarData(isoStr) {
        if (!isoStr) return "—";
        const [y, m, d] = isoStr.split("-");
        return `${d}/${m}/${y}`;
    }

    function formatarLocalDateTime(valor) {
        if (window.MainAPI?.formatarLocalDateTime) {
            return window.MainAPI.formatarLocalDateTime(valor, "—");
        }
        return "—";
    }

    // ── INIT ─────────────────────────────────────────────────────
    async function init() {
        if (!userId) {
            window.location.href = "login.html";
            return;
        }
        // Busca dados frescos da API para garantir sobrenome, sexo, dataNascimento e email
        try {
            const res = await fetch(`${API}/usuarios/${userId}`);
            if (res.ok) {
                const usuarioAtualizado = await res.json();
                const merged = { ...usuarioLogado, ...usuarioAtualizado };
                localStorage.setItem("usuarioLogado", JSON.stringify(merged));
                popularHero(merged);
            } else {
                popularHero(usuarioLogado);
            }
        } catch (e) {
            popularHero(usuarioLogado);
        }
        await Promise.all([
            carregarConfig(),
            carregarInstituicoes(),
            carregarCategorias()
        ]);
    }

    // ── HERO ─────────────────────────────────────────────────────
    function popularHero(u) {
        const nomeEl = document.getElementById("pfNome");
        if (nomeEl) nomeEl.textContent = `${u.nome} ${u.sobrenome}`;
        const sexoEl = document.getElementById("pfSexo");
        if (sexoEl) sexoEl.textContent = u.sexo || "—";
        const nascEl = document.getElementById("pfNascimento");
        if (nascEl) nascEl.textContent = formatarData(u.dataNascimento);
        const emailEl = document.getElementById("pfEmail");
        if (emailEl) emailEl.textContent = u.email || "—";
    }

    // ── EDITAR PERFIL ─────────────────────────────────────────────
    window.abrirEdicaoPerfil = function () {
        // Lê sempre o dado mais recente do localStorage
        const u = JSON.parse(localStorage.getItem("usuarioLogado") || "null") || {};

        let modal = document.getElementById("modalEdicao");
        if (!modal) {
            modal = document.createElement("div");
            modal.id = "modalEdicao";
            modal.style.cssText = `
                position:fixed;inset:0;background:rgba(0,0,0,0.5);
                display:flex;align-items:center;justify-content:center;z-index:9999;
            `;
            modal.innerHTML = `
                <div style="background:#FAFFFF;border-radius:20px;padding:32px;width:min(460px,90vw);display:flex;flex-direction:column;gap:16px;">
                    <h2 style="color:#004C58;margin:0;font-size:1.4rem;">Editar Perfil</h2>
                    <input id="edNome" type="text" placeholder="Nome"
                        style="height:48px;border:2px solid #367373;border-radius:8px;padding:0 14px;font-size:16px;">
                    <input id="edSobrenome" type="text" placeholder="Sobrenome"
                        style="height:48px;border:2px solid #367373;border-radius:8px;padding:0 14px;font-size:16px;">
                    <input id="edEmail" type="email" placeholder="Email"
                        style="height:48px;border:2px solid #367373;border-radius:8px;padding:0 14px;font-size:16px;">
                    <input id="edNascimento" type="date"
                        style="height:48px;border:2px solid #367373;border-radius:8px;padding:0 14px;font-size:16px;">
                    <select id="edSexo" style="height:48px;border:2px solid #367373;border-radius:8px;padding:0 14px;font-size:16px;appearance:none;">
                        <option value="">Sexo</option>
                        <option value="Masculino">Masculino</option>
                        <option value="Feminino">Feminino</option>
                        <option value="PrefiroNãoIdentificar">Prefiro não identificar</option>
                    </select>
                    <div style="display:flex;gap:12px;margin-top:4px;">
                        <button onclick="salvarEdicaoPerfil()"
                            style="flex:1;height:48px;background:#367373;color:#fff;border:none;border-radius:8px;font-size:16px;cursor:pointer;">
                            Salvar
                        </button>
                        <button onclick="document.getElementById('modalEdicao').style.display='none'"
                            style="flex:1;height:48px;background:transparent;color:#367373;border:2px solid #367373;border-radius:8px;font-size:16px;cursor:pointer;">
                            Cancelar
                        </button>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
        }

        // Preenche os campos com os dados atuais sempre que o modal abre
        document.getElementById("edNome").value = u.nome || "";
        document.getElementById("edSobrenome").value = u.sobrenome || "";
        document.getElementById("edEmail").value = u.email || "";
        document.getElementById("edNascimento").value = u.dataNascimento || "";
        document.getElementById("edSexo").value = u.sexo || "";

        modal.style.display = "flex";
    };

    window.salvarEdicaoPerfil = async function () {
        const emailVal = document.getElementById("edEmail").value.trim();
        const payload = {
            nome: document.getElementById("edNome").value.trim(),
            sobrenome: document.getElementById("edSobrenome").value.trim(),
            dataNascimento: document.getElementById("edNascimento").value || undefined,
            sexo: document.getElementById("edSexo").value || undefined
        };
        if (emailVal) payload.email = emailVal;

        if (!payload.nome || !payload.sobrenome || !payload.email) {
            mostrarAlerta("Preencha nome, sobrenome e email.");
            return;
        }

        try {
            const res = await putJson(`${API}/usuarios/${userId}`, payload);
            if (!res.ok) { mostrarAlerta("Erro ao salvar perfil."); return; }
            const atualizado = await res.json();
            // Atualiza localStorage
            const novoUsuario = { ...usuarioLogado, ...atualizado };
            localStorage.setItem("usuarioLogado", JSON.stringify(novoUsuario));
            popularHero(novoUsuario);
            document.getElementById("modalEdicao").style.display = "none";
        } catch (e) {
            mostrarAlerta("Erro ao salvar perfil.");
            console.error(e);
        }
    };

    // ── CONFIGURAÇÕES (resumo) ────────────────────────────────────
    async function carregarConfig() {
        try {
            const res = await fetch(`${API}/configuracoes/usuarios/${userId}`);
            if (!res.ok) return;
            const cfg = await res.json();

            const mfEl = document.getElementById("resumoMesFiscal");
            if (mfEl) mfEl.textContent = cfg.inicioMesFiscal != null
                ? `Dia ${cfg.inicioMesFiscal}` : "—";

            const limEl = document.getElementById("resumoLimite");
            if (limEl) limEl.textContent = cfg.limiteDesejadoMensal != null
                ? `R$ ${Number(cfg.limiteDesejadoMensal).toFixed(2)}` : "—";

            const atEl = document.getElementById("resumoAtualizacao");
            if (atEl) atEl.textContent = cfg.ultimaAtualizacao
                ? formatarData(cfg.ultimaAtualizacao) : "—";
        } catch (e) {
            console.error("Erro ao carregar configurações:", e);
        }
    }

    // ── INSTITUIÇÕES ──────────────────────────────────────────────
    async function carregarInstituicoes() {
        try {
            const res = await fetch(`${API}/instituicoes/usuarios/${userId}`);
            const lista = res.status === 204 ? [] : (res.ok ? await res.json() : []);
            renderInstituicoes(lista);
        } catch (e) {
            console.error("Erro ao carregar instituições:", e);
        }
    }

    function renderInstituicoes(lista) {
        const tbody = document.getElementById("corpoInstituicoes");
        if (!tbody) return;
        tbody.innerHTML = "";
        if (lista.length === 0) {
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.colSpan = 4;
            td.style.cssText = "padding:16px;text-align:center;color:#888;";
            td.textContent = "Nenhuma instituição vinculada.";
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }
        lista.forEach(inst => {
            const tr = document.createElement("tr");

            const tdNome = document.createElement("td");
            tdNome.textContent = inst.intituicao.nome;
            tr.appendChild(tdNome);

            const tdLimite = document.createElement("td");
            tdLimite.textContent = "—";
            tr.appendChild(tdLimite);

            const tdMod = document.createElement("td");
            tdMod.textContent = formatarLocalDateTime(inst.ultimaAtualizacao);
            tr.appendChild(tdMod);

            const tdAcoes = document.createElement("td");
            const divAcoes = document.createElement("div");
            divAcoes.className = "pf-table-actions";
            const btn = document.createElement("button");
            btn.className = "pf-icon-btn delete";
            btn.title = "Desvincular";
            btn.innerHTML = "<i class='bx bx-trash'></i>";
            btn.addEventListener("click", () => desvincularInstituicao(inst.intituicao.id));
            divAcoes.appendChild(btn);
            tdAcoes.appendChild(divAcoes);
            tr.appendChild(tdAcoes);

            tbody.appendChild(tr);
        });
    }

    window.desvincularInstituicao = function (instId) {
        mostrarConfirmacao("Desvincular esta instituição do seu perfil?", async () => {
            try {
                await fetch(`${API}/instituicoes/${instId}/usuarios/${userId}`, { method: "PATCH" });
                await carregarInstituicoes();
            } catch (e) {
                mostrarAlerta("Erro ao desvincular instituição.");
                console.error(e);
            }
        });
    };

    window.adicionarInstituicaoModal = async function () {
        // Carrega todas as instituições disponíveis para o usuário escolher
        let todasInst = [];
        try {
            const res = await fetch(`${API}/instituicoes`);
            todasInst = res.ok ? await res.json() : [];
        } catch (e) { console.error(e); }

        if (document.getElementById("modalInstituicao")) {
            document.getElementById("modalInstituicao").remove();
        }

        const options = todasInst.map(i =>
            `<option value="${i.id}">${i.nome}</option>`
        ).join("");

        const modal = document.createElement("div");
        modal.id = "modalInstituicao";
        modal.style.cssText = `
            position:fixed;inset:0;background:rgba(0,0,0,0.5);
            display:flex;align-items:center;justify-content:center;z-index:9999;
        `;
        modal.innerHTML = `
            <div style="background:#FAFFFF;border-radius:20px;padding:32px;width:min(400px,90vw);display:flex;flex-direction:column;gap:16px;">
                <h2 style="color:#004C58;margin:0;font-size:1.4rem;">Vincular Instituição</h2>
                <select id="selNovaInst" style="height:48px;border:2px solid #367373;border-radius:8px;padding:0 14px;font-size:16px;appearance:none;">
                    <option value="">Selecione...</option>
                    ${options}
                </select>
                <div style="display:flex;gap:12px;">
                    <button onclick="confirmarAdicionarInst()"
                        style="flex:1;height:48px;background:#367373;color:#fff;border:none;border-radius:8px;font-size:16px;cursor:pointer;">
                        Vincular
                    </button>
                    <button onclick="document.getElementById('modalInstituicao').remove()"
                        style="flex:1;height:48px;background:transparent;color:#367373;border:2px solid #367373;border-radius:8px;font-size:16px;cursor:pointer;">
                        Cancelar
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
    };

    window.confirmarAdicionarInst = async function () {
        const instId = document.getElementById("selNovaInst")?.value;
        if (!instId) { mostrarAlerta("Selecione uma instituição."); return; }
        try {
            const res = await fetch(`${API}/instituicoes/${instId}/usuarios/${userId}`, { method: "POST" });
            if (!res.ok) {
                const body = await res.json().catch(() => ({}));
                mostrarAlerta(body.message || "Erro ao vincular instituição.");
                return;
            }
            document.getElementById("modalInstituicao")?.remove();
            await carregarInstituicoes();
        } catch (e) {
            mostrarAlerta("Erro ao vincular instituição.");
            console.error(e);
        }
    };

    // ── CATEGORIAS ────────────────────────────────────────────────
    async function carregarCategorias() {
        try {
            const res = await fetch(`${API}/categorias/usuario/${userId}`);
            const lista = res.status === 204 ? [] : (res.ok ? await res.json() : []);
            renderCategorias(lista);
        } catch (e) {
            console.error("Erro ao carregar categorias:", e);
        }
    }

    function renderCategorias(lista) {
        const tbody = document.getElementById("corpoCategorias");
        if (!tbody) return;
        tbody.innerHTML = "";
        if (lista.length === 0) {
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.colSpan = 4;
            td.style.cssText = "padding:16px;text-align:center;color:#888;";
            td.textContent = "Nenhuma categoria registrada.";
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }
        lista.forEach(cat => {
            const tr = document.createElement("tr");

            const tdNome = document.createElement("td");
            tdNome.textContent = cat.categoria.titulo;
            tr.appendChild(tdNome);

            const tdLimite = document.createElement("td");
            tdLimite.textContent = "—";
            tr.appendChild(tdLimite);

            const tdMod = document.createElement("td");
            tdMod.textContent = formatarLocalDateTime(cat.ultimaAtualizacao);
            tr.appendChild(tdMod);

            const tdAcoes = document.createElement("td");
            const divAcoes = document.createElement("div");
            divAcoes.className = "pf-table-actions";
            const btn = document.createElement("button");
            btn.className = "pf-icon-btn delete";
            btn.title = "Remover";
            btn.innerHTML = "<i class='bx bx-trash'></i>";
            btn.addEventListener("click", () => removerCategoria(cat.categoria.id));
            divAcoes.appendChild(btn);
            tdAcoes.appendChild(divAcoes);
            tr.appendChild(tdAcoes);

            tbody.appendChild(tr);
        });
    }

    window.removerCategoria = function (catId) {
        mostrarConfirmacao("Remover esta categoria do seu perfil?", async () => {
            try {
                await fetch(`${API}/categorias/${catId}/usuarios/${userId}`, { method: "PATCH" });
                await carregarCategorias();
            } catch (e) {
                mostrarAlerta("Erro ao remover categoria.");
                console.error(e);
            }
        });
    };

    window.adicionarCategoriaModal = function () {
        if (document.getElementById("modalCategoria")) {
            document.getElementById("modalCategoria").style.display = "flex";
            return;
        }
        const modal = document.createElement("div");
        modal.id = "modalCategoria";
        modal.style.cssText = `
            position:fixed;inset:0;background:rgba(0,0,0,0.5);
            display:flex;align-items:center;justify-content:center;z-index:9999;
        `;
        modal.innerHTML = `
            <div style="background:#FAFFFF;border-radius:20px;padding:32px;width:min(400px,90vw);display:flex;flex-direction:column;gap:16px;">
                <h2 style="color:#004C58;margin:0;font-size:1.4rem;">Nova Categoria</h2>
                <input id="edTituloCategoria" type="text" placeholder="Nome da categoria"
                    style="height:48px;border:2px solid #367373;border-radius:8px;padding:0 14px;font-size:16px;">
                <div style="display:flex;gap:12px;">
                    <button onclick="confirmarAdicionarCategoria()"
                        style="flex:1;height:48px;background:#367373;color:#fff;border:none;border-radius:8px;font-size:16px;cursor:pointer;">
                        Adicionar
                    </button>
                    <button onclick="document.getElementById('modalCategoria').style.display='none'"
                        style="flex:1;height:48px;background:transparent;color:#367373;border:2px solid #367373;border-radius:8px;font-size:16px;cursor:pointer;">
                        Cancelar
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
    };

    window.confirmarAdicionarCategoria = async function () {
        const titulo = document.getElementById("edTituloCategoria")?.value?.trim();
        if (!titulo) { mostrarAlerta("Informe o nome da categoria."); return; }
        try {
            const res = await fetch(`${API}/categorias/usuario/${userId}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ titulo })
            });
            if (!res.ok) { mostrarAlerta("Erro ao criar categoria."); return; }
            document.getElementById("modalCategoria").style.display = "none";
            document.getElementById("edTituloCategoria").value = "";
            await carregarCategorias();
        } catch (e) {
            mostrarAlerta("Erro ao criar categoria.");
            console.error(e);
        }
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
