(function () {
    const API = "http://localhost:8080";
    const usuarioLogado = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
    const userId = usuarioLogado?.id;

    let cfgId = null;
    let cfgData = null;   // armazena config completa carregada do servidor
    let instituicoes = [];
    let categorias = [];
    let tipoPersonalizada = null; // 'instituicao' | 'categoria'
    let limiteModalTipo = null; // 'instituicao' | 'categoria'

    // ── helpers ──────────────────────────────────────────────────
    function habilitarFecharAlertaAoClicarFora() {
        const div = document.getElementById("div_alerta");
        const conteudo = document.getElementById("conteudoAlerta");
        if (!div || !conteudo || div.dataset.closeOutsideBound === "1") return;

        div.dataset.closeOutsideBound = "1";
        div.addEventListener("click", (event) => {
            if (event.target === div) {
                div.style.display = "none";
            }
        });
    }

    function mostrarAlerta(texto) {
        habilitarFecharAlertaAoClicarFora();
        const div = document.getElementById("div_alerta");
        const conteudo = document.getElementById("conteudoAlerta");
        if (!div || !conteudo) return;
        conteudo.innerHTML = texto;
        div.style.display = "flex";
        setTimeout(() => { div.style.display = "none"; }, 3000);
    }

    function mostrarConfirmacao(texto, onConfirm) {
        habilitarFecharAlertaAoClicarFora();
        const div = document.getElementById("div_alerta");
        const conteudo = document.getElementById("conteudoAlerta");
        if (!div || !conteudo) { onConfirm(); return; }
        conteudo.innerHTML = "";
        const msg = document.createElement("span");
        msg.textContent = texto;
        const divBtns = document.createElement("div");
        divBtns.style.cssText = "display:flex;gap:8px;margin-top:10px;";
        const btnNo = document.createElement("button");
        btnNo.textContent = "Cancelar";
        btnNo.style.background = "#888";
        const btnOk = document.createElement("button");
        btnOk.textContent = "Confirmar";
        btnOk.addEventListener("click", () => { div.style.display = "none"; onConfirm(); });
        btnNo.addEventListener("click", () => { div.style.display = "none"; });
        divBtns.appendChild(btnNo);
        divBtns.appendChild(btnOk);
        conteudo.appendChild(msg);
        conteudo.appendChild(divBtns);
        div.style.display = "flex";
    }

    function postJson(url, payload) {
        return fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
    }

    function putJson(url, payload) {
        return fetch(url, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
    }

    function formatarLocalDateTime(valor) {
        if (window.MainAPI?.formatarLocalDateTime) {
            return window.MainAPI.formatarLocalDateTime(valor, "-");
        }
        return "-";
    }

    function chaveAlertas() {
        return `cfg_alertas_${userId}`;
    }

    function salvarPreferenciasAlertas() {
        const swWhatsapp = document.getElementById("sw_alerta_whatsapp");
        const swEmail = document.getElementById("sw_alerta_email");
        if (!swWhatsapp || !swEmail) return;

        const preferencias = {
            whatsapp: !!swWhatsapp.checked,
            email: !!swEmail.checked
        };
        localStorage.setItem(chaveAlertas(), JSON.stringify(preferencias));
    }

    function inicializarSwitchesAlerta() {
        const swWhatsapp = document.getElementById("sw_alerta_whatsapp");
        const swEmail = document.getElementById("sw_alerta_email");
        if (!swWhatsapp || !swEmail) return;

        const salvas = JSON.parse(localStorage.getItem(chaveAlertas()) || "null");
        swWhatsapp.checked = !!salvas?.whatsapp;
        swEmail.checked = !!salvas?.email;

        swWhatsapp.addEventListener("change", salvarPreferenciasAlertas);
        swEmail.addEventListener("change", salvarPreferenciasAlertas);
    }

    // ── INIT ─────────────────────────────────────────────────────
    async function init() {
        if (!userId) {
            window.location.href = "login.html";
            return;
        }
        inicializarSwitchesAlerta();
        await Promise.all([
            carregarConfig(),
            carregarInstituicoes(),
            carregarCategorias()
        ]);
    }

    // ── CONFIGURAÇÕES ─────────────────────────────────────────────
    async function carregarConfig() {
        try {
            const res = await fetch(`${API}/configuracoes/usuarios/${userId}`);
            if (!res.ok) return;
            const cfg = await res.json();
            cfgId = cfg.id;
            cfgData = cfg;

            const sel = document.getElementById("mesFiscal");
            if (sel && cfg.inicioMesFiscal != null) {
                sel.value = String(cfg.inicioMesFiscal);
            }

            const limiteInput = document.getElementById("limiteMensal");
            if (limiteInput && cfg.limiteDesejadoMensal != null) {
                limiteInput.value = cfg.limiteDesejadoMensal;
            }

            renderLimites(cfg.limiteInstituicao || [], cfg.limitePorCategoria || []);
        } catch (e) {
            console.error("Erro ao carregar configurações:", e);
        }
    }

    function renderLimites(limitesInst, limitesCateg) {
        const tbody = document.getElementById("corpoLimites");
        if (!tbody) return;
        tbody.innerHTML = "";

        const linhas = [];
        limitesInst.forEach(l => {
            linhas.push({ tipo: "Instituição", item: l.instituicao?.nome || "-", limite: l.limiteDesejado });
        });
        limitesCateg.forEach(l => {
            linhas.push({ tipo: "Categoria", item: l.categoria?.titulo || "-", limite: l.limiteDesejado });
        });

        if (linhas.length === 0) {
            tbody.innerHTML = `<tr class="cfg-table-empty"><td colspan="4" style="padding:20px;">Nenhum limite configurado ainda.</td></tr>`;
            return;
        }
        linhas.forEach(l => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td>${l.tipo}</td><td>${l.item}</td><td>R$ ${Number(l.limite).toFixed(2)}</td><td>-</td>`;
            tbody.appendChild(tr);
        });
    }

    window.salvarMesFiscal = async function () {
        const sel = document.getElementById("mesFiscal");
        if (!sel?.value) {
            mostrarAlerta("Selecione o início do mês fiscal.");
            return;
        }
        const mes = parseInt(sel.value, 10);
        const limiteInput = document.getElementById("limiteMensal");
        const limiteMensal = limiteInput?.value ? parseFloat(limiteInput.value) : cfgData?.limiteDesejadoMensal;
        try {
            if (cfgId) {
                const payload = { inicioMesFiscal: mes };
                if (limiteMensal != null && !isNaN(limiteMensal)) payload.limiteDesejadoMensal = limiteMensal;
                const res = await putJson(`${API}/configuracoes/edit/${cfgId}`, payload);
                if (!res.ok) { mostrarAlerta(`Erro ao salvar (HTTP ${res.status}).`); return; }
            } else {
                const payload = { fkUsuario: userId, inicioMesFiscal: mes };
                if (limiteMensal != null && !isNaN(limiteMensal)) payload.limiteDesejadoMensal = limiteMensal;
                const res = await postJson(`${API}/configuracoes`, payload);
                if (res.ok) {
                    const cfg = await res.json();
                    cfgId = cfg.id;
                }
            }
            await carregarConfig();
            mostrarAlerta("Configurações salvas!");
        } catch (e) {
            mostrarAlerta("Erro ao salvar configurações.");
            console.error(e);
        }
    };

    window.salvarLimite = async function ({ instId = null, catId = null, valor = null } = {}) {
        if (!cfgId) {
            mostrarAlerta("Salve o mês fiscal primeiro para criar as configurações.");
            return false;
        }

        const idInst = instId || document.getElementById("selInstituicaoLimite")?.value;
        const idCat = catId || document.getElementById("selCategoriaLimite")?.value;
        const valorNum = valor != null
            ? parseFloat(valor)
            : parseFloat(document.getElementById("ipt_limite_valor")?.value);

        if (!idInst && !idCat) {
            mostrarAlerta("Selecione uma instituição ou categoria para definir o limite.");
            return false;
        }
        if (!valorNum || valorNum <= 0) {
            mostrarAlerta("Informe um valor de limite válido.");
            return false;
        }

        // Preserva valores atuais para evitar sobrescrita com null no backend
        const payload = {};
        if (cfgData?.inicioMesFiscal != null) payload.inicioMesFiscal = cfgData.inicioMesFiscal;
        if (cfgData?.limiteDesejadoMensal != null) payload.limiteDesejadoMensal = cfgData.limiteDesejadoMensal;
        if (idInst) payload.limitesInstituicao = [{ instituicaoId: parseInt(idInst), valor: valorNum }];
        if (idCat) payload.limitesCategoria = [{ categoriaId: parseInt(idCat), valor: valorNum }];

        try {
            const res = await putJson(`${API}/configuracoes/edit/${cfgId}`, payload);
            if (res.ok) {
                await carregarConfig();
                mostrarAlerta("Limite salvo!");
                return true;
            } else {
                const body = await res.json().catch(() => ({}));
                mostrarAlerta(`Erro ao salvar limite (HTTP ${res.status}): ${body.message || ""}`);
                return false;
            }
        } catch (e) {
            mostrarAlerta("Erro ao salvar limite.");
            console.error(e);
            return false;
        }
    };

    window.abrirModalLimite = function (tipo) {
        limiteModalTipo = tipo;
        const modal = document.getElementById("modalLimite");
        const titulo = document.getElementById("modalLimiteTitulo");
        const label = document.getElementById("lblLimiteItem");
        const sel = document.getElementById("selLimiteItem");
        const inputValor = document.getElementById("ipt_limite_modal_valor");
        if (!modal || !titulo || !label || !sel || !inputValor) return;

        sel.innerHTML = `<option value="" disabled selected></option>`;

        if (tipo === "instituicao") {
            titulo.textContent = "Definir limite por instituição";
            label.textContent = "Selecionar instituição";
            instituicoes.forEach(inst => {
                const opt = document.createElement("option");
                opt.value = inst.intituicao.id;
                opt.textContent = inst.intituicao.nome;
                sel.appendChild(opt);
            });
        } else {
            titulo.textContent = "Definir limite por categoria";
            label.textContent = "Selecionar categoria";
            categorias.forEach(cat => {
                const opt = document.createElement("option");
                opt.value = cat.categoria.id;
                opt.textContent = cat.categoria.titulo;
                sel.appendChild(opt);
            });
        }

        inputValor.value = "";
        modal.style.display = "flex";
    };

    window.fecharModalLimite = function () {
        const modal = document.getElementById("modalLimite");
        if (modal) modal.style.display = "none";
        limiteModalTipo = null;
    };

    window.salvarLimiteModal = async function () {
        const sel = document.getElementById("selLimiteItem");
        const valor = document.getElementById("ipt_limite_modal_valor");
        const idSelecionado = sel?.value;
        const valorSelecionado = valor?.value;

        if (!limiteModalTipo) {
            mostrarAlerta("Tipo de limite inválido.");
            return;
        }

        if (!idSelecionado) {
            mostrarAlerta("Selecione um item para limitar.");
            return;
        }

        const ok = await window.salvarLimite({
            instId: limiteModalTipo === "instituicao" ? idSelecionado : null,
            catId: limiteModalTipo === "categoria" ? idSelecionado : null,
            valor: valorSelecionado
        });

        if (ok) window.fecharModalLimite();
    };

    // ── INSTITUIÇÕES ──────────────────────────────────────────────
    async function carregarInstituicoes() {
        try {
            const res = await fetch(`${API}/instituicoes/usuarios/${userId}`);
            instituicoes = res.status === 204 ? [] : (res.ok ? await res.json() : []);
            renderInstituicoes();
            preencherSelectInstituicoes();
            await preencherSelectNovaInstituicao();
        } catch (e) {
            console.error("Erro ao carregar instituições:", e);
        }
    }

    function renderInstituicoes() {
        const tbody = document.getElementById("corpoInstituicoes");
        if (!tbody) return;
        tbody.innerHTML = "";
        if (instituicoes.length === 0) {
            const tr = document.createElement("tr");
            tr.className = "cfg-table-empty";
            const td = document.createElement("td");
            td.colSpan = 3;
            td.style.padding = "20px";
            td.textContent = "Nenhuma instituição cadastrada.";
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }
        instituicoes.forEach(inst => {
            const tr = document.createElement("tr");

            const tdNome = document.createElement("td");
            tdNome.textContent = inst.intituicao.nome;
            tr.appendChild(tdNome);

            const tdMod = document.createElement("td");
            tdMod.textContent = formatarLocalDateTime(inst.ultimaAtualizacao);
            tr.appendChild(tdMod);

            const tdAcoes = document.createElement("td");
            const btn = document.createElement("button");
            btn.className = "cfg-btn danger";
            btn.textContent = "Desvincular";
            btn.addEventListener("click", async () => 
                mostrarConfirmacao("Remover esta instituição do seu perfil?", async () => {
                try {
                    const res = await fetch(`${API}/instituicoes/${inst.intituicao.id}/usuarios/${userId}`, { method: "PATCH" });
                    if (!res.ok) { mostrarAlerta(`Erro ao desvincular (HTTP ${res.status}).`); return; }
                    await carregarInstituicoes();
                } catch (e) {
                    mostrarAlerta("Erro ao desvincular instituição.");
                    console.error(e);
                }
            }));
            tdAcoes.appendChild(btn);
            tr.appendChild(tdAcoes);

            tbody.appendChild(tr);
        });
    }

    function preencherSelectInstituicoes() {
        const sel = document.getElementById("selInstituicaoLimite");
        if (!sel) return;
        sel.innerHTML = `<option value="" disabled selected></option>`;
        instituicoes.forEach(inst => {
            const opt = document.createElement("option");
            opt.value = inst.intituicao.id;
            opt.textContent = inst.intituicao.nome;
            sel.appendChild(opt);
        });
    }

    window.adicionarInstituicao = async function () {
        const sel = document.getElementById("sel_nova_instituicao");
        const instId = sel?.value;
        if (!instId) { mostrarAlerta("Selecione uma instituição."); return; }
        try {
            const res = await fetch(`${API}/instituicoes/${instId}/usuarios/${userId}`, { method: "POST" });
            if (!res.ok) { mostrarAlerta(`Erro ao adicionar instituição (HTTP ${res.status}).`); return; }
            sel.value = "";
            await carregarInstituicoes();
        } catch (e) {
            mostrarAlerta("Erro ao adicionar instituição.");
            console.error(e);
        }
    };

    async function preencherSelectNovaInstituicao() {
        const sel = document.getElementById("sel_nova_instituicao");
        if (!sel) return;
        try {
            const res = await fetch(`${API}/instituicoes`);
            if (!res.ok) return;
            const todas = await res.json();
            const vinculadasIds = new Set(instituicoes.map(i => i.intituicao.id));
            const disponiveis = todas.filter(i => !vinculadasIds.has(i.id));
            sel.innerHTML = `<option value="" disabled selected></option>`;
            disponiveis.forEach(inst => {
                const opt = document.createElement("option");
                opt.value = inst.id;
                opt.textContent = inst.nome;
                sel.appendChild(opt);
            });
        } catch (e) {
            console.error("Erro ao carregar instituições disponíveis:", e);
        }
    }



    // ── CATEGORIAS ────────────────────────────────────────────────
    async function carregarCategorias() {
        try {
            const res = await fetch(`${API}/categorias/usuario/${userId}`);
            categorias = res.status === 204 ? [] : (res.ok ? await res.json() : []);
            renderCategorias();
            preencherSelectCategorias();
            await preencherSelectNovaCategoria();
        } catch (e) {
            console.error("Erro ao carregar categorias:", e);
        }
    }

    async function preencherSelectNovaCategoria() {
        const sel = document.getElementById("sel_nova_categoria");
        if (!sel) return;
        try {
            const res = await fetch(`${API}/categorias`);
            if (!res.ok) return;
            const todas = await res.json();
            const vinculadasIds = new Set(categorias.map(c => c.categoria.id));
            const disponiveis = todas.filter(c => !vinculadasIds.has(c.id));
            sel.innerHTML = `<option value="" disabled selected></option>`;
            disponiveis.forEach(cat => {
                const opt = document.createElement("option");
                opt.value = cat.id;
                opt.textContent = cat.titulo;
                sel.appendChild(opt);
            });
        } catch (e) {
            console.error("Erro ao carregar categorias disponíveis:", e);
        }
    }

    function renderCategorias() {
        const tbody = document.getElementById("corpoCategorias");
        if (!tbody) return;
        tbody.innerHTML = "";
        if (categorias.length === 0) {
            const tr = document.createElement("tr");
            tr.className = "cfg-table-empty";
            const td = document.createElement("td");
            td.colSpan = 3;
            td.style.padding = "20px";
            td.textContent = "Nenhuma categoria cadastrada.";
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }
        categorias.forEach(cat => {
            const tr = document.createElement("tr");

            const tdNome = document.createElement("td");
            tdNome.textContent = cat.categoria.titulo;
            tr.appendChild(tdNome);

            const tdMod = document.createElement("td");
            tdMod.textContent = formatarLocalDateTime(cat.ultimaAtualizacao);
            tr.appendChild(tdMod);

            const tdAcoes = document.createElement("td");
            const btn = document.createElement("button");
            btn.className = "cfg-btn danger";
            btn.textContent = "Remover";
            btn.addEventListener("click", () => {
                mostrarConfirmacao("Remover esta categoria do seu perfil?", async () => {
                    try {
                        const res = await fetch(`${API}/categorias/${cat.categoria.id}/usuarios/${userId}`, { method: "PATCH" });
                        if (!res.ok) { mostrarAlerta(`Erro ao remover (HTTP ${res.status}).`); return; }
                        await carregarCategorias();
                    } catch (e) {
                        mostrarAlerta("Erro ao remover categoria.");
                        console.error(e);
                    }
                });
            });
            tdAcoes.appendChild(btn);
            tr.appendChild(tdAcoes);

            tbody.appendChild(tr);
        });
    }

    function preencherSelectCategorias() {
        const sel = document.getElementById("selCategoriaLimite");
        if (!sel) return;
        sel.innerHTML = `<option value="" disabled selected></option>`;
        categorias.forEach(cat => {
            const opt = document.createElement("option");
            opt.value = cat.categoria.id;
            opt.textContent = cat.categoria.titulo;
            sel.appendChild(opt);
        });
    }

    window.adicionarCategoria = async function () {
        const sel = document.getElementById("sel_nova_categoria");
        const catId = sel?.value;
        if (!catId) { mostrarAlerta("Selecione uma categoria."); return; }
        try {
            const res = await fetch(`${API}/categorias/${catId}/usuarios/${userId}`, { method: "POST" });
            if (!res.ok) { mostrarAlerta(`Erro ao adicionar categoria (HTTP ${res.status}).`); return; }
            sel.value = "";
            await carregarCategorias();
            window.dispatchEvent(new Event('xp:refresh'));
            mostrarAlerta("Categoria adicionada com sucesso!");
        } catch (e) {
            mostrarAlerta("Erro ao adicionar categoria.");
            console.error(e);
        }
    };

    window.abrirPersonalizada = function (tipo) {
        tipoPersonalizada = tipo;
        const modal  = document.getElementById("modalPersonalizada");
        const titulo = document.getElementById("modalPersonalizadaTitulo");
        const label  = document.getElementById("modalPersonalizadaLabel");
        const input  = document.getElementById("ipt_personalizada");
        if (tipo === "instituicao") {
            if (titulo) titulo.textContent = "Nova Instituição Personalizada";
            if (label)  label.textContent  = "Nome da instituição";
        } else {
            if (titulo) titulo.textContent = "Nova Categoria Personalizada";
            if (label)  label.textContent  = "Nome da categoria";
        }
        if (input) input.value = "";
        if (modal) modal.style.display = "flex";
    };

    window.fecharPersonalizada = function () {
        const modal = document.getElementById("modalPersonalizada");
        if (modal) modal.style.display = "none";
        tipoPersonalizada = null;
    };

    window.salvarPersonalizada = async function () {
        const input = document.getElementById("ipt_personalizada");
        const nome = input?.value?.trim();
        if (!nome) { mostrarAlerta("Informe o nome."); return; }
        try {
            if (tipoPersonalizada === "instituicao") {
                const resCreate = await postJson(`${API}/instituicoes`, { nome });
                if (!resCreate.ok) { mostrarAlerta("Erro ao criar instituição."); return; }
                const nova = await resCreate.json();
                const resLink = await fetch(`${API}/instituicoes/${nova.id}/usuarios/${userId}`, { method: "POST" });
                if (!resLink.ok) { mostrarAlerta("Erro ao vincular instituição."); return; }
                fecharPersonalizada();
                await carregarInstituicoes();
                window.dispatchEvent(new Event('xp:refresh'));
                mostrarAlerta("Instituição criada e vinculada com sucesso!");
            } else {
                const res = await postJson(`${API}/categorias/usuario/${userId}`, { titulo: nome });
                if (!res.ok) { mostrarAlerta("Erro ao criar categoria."); return; }
                fecharPersonalizada();
                await carregarCategorias();
                window.dispatchEvent(new Event('xp:refresh'));
                mostrarAlerta("Categoria personalizada criada com sucesso!");
            }
        } catch (e) {
            mostrarAlerta("Erro ao salvar.");
            console.error(e);
        }
    };

    window.removerCategoria = async function (catId) {
        try {
            await fetch(`${API}/categorias/${catId}/usuarios/${userId}`, { method: "PATCH" });
            await carregarCategorias();
        } catch (e) {
            mostrarAlerta("Erro ao remover categoria.");
            console.error(e);
        }
    };

    // ── EVENTOS (backend não implementado) ────────────────────────
    window.importarDados = function () {
        document.getElementById("inputImportar")?.click();
    };

    function obterFormatosExportacaoDisponiveis() {
        return [
            { tipo: "pdf", label: "PDF (.pdf)" },
            { tipo: "excel", label: "Excel (.xlsx)" },
            { tipo: "json", label: "JSON (.json)" },
            { tipo: "sql", label: "SQL (.sql)" }
        ];
    }

    function garantirModalExportacao() {
        let modal = document.getElementById("cfgExportModal");
        if (modal) return modal;

        modal = document.createElement("div");
        modal.id = "cfgExportModal";
        modal.className = "modal";
        modal.style.display = "none";
        modal.innerHTML = `
            <div class="conteudoModal" style="max-width:460px;">
                <span id="cfgExportFechar" class="fechar" style="cursor:pointer;">&times;</span>
                <h2 style="margin-top:0; color:#0e5454;">Exportar dados</h2>
                <p style="margin:0 0 12px 0; color:#486468;">Selecione o formato do arquivo:</p>
                <div style="display:flex; flex-direction:column; gap:10px;">
                    <select id="cfgExportTipo" class="select_modal"></select>
                    <div style="display:flex; gap:10px; justify-content:flex-end;">
                        <button id="cfgExportCancelar" class="cfg-btn" style="background:transparent;border:2px solid #367373;color:#367373;">Cancelar</button>
                        <button id="cfgExportConfirmar" class="cfg-btn">Exportar</button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        const fechar = () => { modal.style.display = "none"; };
        const btnFechar = document.getElementById("cfgExportFechar");
        const btnCancelar = document.getElementById("cfgExportCancelar");
        if (btnFechar) btnFechar.onclick = fechar;
        if (btnCancelar) btnCancelar.onclick = fechar;

        modal.addEventListener("click", (event) => {
            if (event.target === modal) {
                fechar();
            }
        });

        const btnConfirmar = document.getElementById("cfgExportConfirmar");
        if (btnConfirmar) {
            btnConfirmar.onclick = async () => {
                const select = document.getElementById("cfgExportTipo");
                const tipo = select?.value;
                if (!tipo) {
                    mostrarAlerta("Selecione um formato para exportação.");
                    return;
                }
                modal.style.display = "none";
                await exportarDadosPorTipo(tipo);
            };
        }

        return modal;
    }

    function abrirModalExportacao() {
        const modal = garantirModalExportacao();
        const select = document.getElementById("cfgExportTipo");
        if (!select) return;

        const formatos = obterFormatosExportacaoDisponiveis();
        select.innerHTML = formatos
            .map(f => `<option value="${f.tipo}">${f.label}</option>`)
            .join("");

        modal.style.display = "flex";
    }

    async function aguardarDesktopBridge(timeoutMs = 1800, intervaloMs = 120) {
        const inicio = Date.now();
        while (Date.now() - inicio < timeoutMs) {
            if (window.desktopBridge) {
                return window.desktopBridge;
            }
            await new Promise(resolve => setTimeout(resolve, intervaloMs));
        }
        return window.desktopBridge || null;
    }

    async function exportarDadosPorTipo(tipo) {
        if (!userId) {
            mostrarAlerta("Usuário não identificado para exportação.");
            return;
        }

        try {
            const res = await fetch(`${API}/registros/download/${userId}?tipo=${encodeURIComponent(tipo)}`);
            if (!res.ok) {
                mostrarAlerta(`Erro ao exportar ${tipo.toUpperCase()} (HTTP ${res.status}).`);
                return;
            }

            const blob = await res.blob();

            const contentDisposition = res.headers.get("content-disposition") || "";
            let nomeArquivo = `registros.${tipo === "excel" ? "xlsx" : tipo}`;
            const match = contentDisposition.match(/filename=([^;]+)/i);
            if (match && match[1]) {
                nomeArquivo = match[1].replace(/"/g, "").trim();
            }

            const userAgent = navigator.userAgent || "";
            const emWebViewJavaFx = /JavaFX/i.test(userAgent);

            const desktopBridge = emWebViewJavaFx
                ? await aguardarDesktopBridge()
                : window.desktopBridge;
            if (desktopBridge) {
                try {
                    const bytes = new Uint8Array(await blob.arrayBuffer());
                    let binario = "";
                    const tamanhoChunk = 0x8000;

                    for (let offset = 0; offset < bytes.length; offset += tamanhoChunk) {
                        const chunk = bytes.subarray(offset, offset + tamanhoChunk);
                        binario += String.fromCharCode(...chunk);
                    }

                    const salvo = desktopBridge.saveBase64File(nomeArquivo, btoa(binario));
                    if (salvo) {
                        mostrarAlerta(`${tipo.toUpperCase()} exportado com sucesso!`);
                    } else {
                        mostrarAlerta("Exportação cancelada pelo usuário.");
                    }
                    return;
                } catch (bridgeError) {
                    console.warn("desktopBridge indisponível ou incompatível nesta página:", bridgeError);
                }
            }

            if (emWebViewJavaFx) {
                mostrarAlerta("Modo desktop detectado, mas a integração de exportação não está ativa. Reinicie o app Desktop.");
                return;
            }

            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = nomeArquivo;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);

            mostrarAlerta(`${tipo.toUpperCase()} exportado com sucesso!`);
        } catch (e) {
            console.error(`Erro ao exportar ${tipo}:`, e);
            mostrarAlerta(`Erro ao exportar ${tipo.toUpperCase()}.`);
        }
    }

    window.exportarDados = function () {
        abrirModalExportacao();
    };

    window.processarImportacao = function () {
        mostrarAlerta("Importação de dados ainda não disponível.");
    };

    // ── CALENDÁRIO – SELEÇÃO DE PERÍODO PARA EXCLUSÃO ────────────
    let cfgCalCampo = null;        // 'inicio' | 'fim'
    let cfgCalDataSelecionada = null;
    let cfgDeleteInicio = null;    // YYYY-MM-DD
    let cfgDeleteFim    = null;    // YYYY-MM-DD

    let cfgCalMesAtual  = new Date().getMonth();
    let cfgCalAnoAtual  = new Date().getFullYear();

    function cfgCalFormatar(iso) {
        const [a, m, d] = iso.split("-");
        return `${d}/${m}/${a}`;
    }

    function cfgCalGerar() {
        const dias    = document.getElementById("cfgCalDias");
        const mesAno  = document.getElementById("cfgCalMesAno");
        const confirmar = document.getElementById("cfgCalConfirmar");
        if (!dias) return;

        dias.innerHTML = "";
        cfgCalDataSelecionada = null;
        if (confirmar) confirmar.disabled = true;

        mesAno.innerText = new Date(cfgCalAnoAtual, cfgCalMesAtual)
            .toLocaleString("pt-BR", { month: "long", year: "numeric" });

        const primeiroDia = new Date(cfgCalAnoAtual, cfgCalMesAtual, 1).getDay();
        const totalDias   = new Date(cfgCalAnoAtual, cfgCalMesAtual + 1, 0).getDate();

        for (let i = 0; i < primeiroDia; i++) dias.innerHTML += `<span></span>`;

        for (let dia = 1; dia <= totalDias; dia++) {
            const iso = `${cfgCalAnoAtual}-${String(cfgCalMesAtual + 1).padStart(2, "0")}-${String(dia).padStart(2, "0")}`;
            const span = document.createElement("span");
            span.innerText = dia;
            span.onclick = () => {
                dias.querySelectorAll(".diaSelecionado").forEach(e => e.classList.remove("diaSelecionado"));
                span.classList.add("diaSelecionado");
                cfgCalDataSelecionada = iso;
                if (confirmar) confirmar.disabled = false;
            };
            dias.appendChild(span);
        }
    }

    window.abrirCalendarioDelete = function (campo) {
        cfgCalCampo = campo;
        cfgCalMesAtual = new Date().getMonth();
        cfgCalAnoAtual = new Date().getFullYear();
        const modal = document.getElementById("cfgCalModal");
        if (modal) { modal.style.display = "flex"; cfgCalGerar(); }
    };

    document.addEventListener("DOMContentLoaded", () => {
        const fechar    = document.getElementById("cfgCalFechar");
        const anterior  = document.getElementById("cfgCalAnterior");
        const proximo   = document.getElementById("cfgCalProximo");
        const confirmar = document.getElementById("cfgCalConfirmar");
        const modal     = document.getElementById("cfgCalModal");

        if (fechar)    fechar.onclick   = () => { if (modal) modal.style.display = "none"; };
        if (anterior)  anterior.onclick = () => {
            cfgCalMesAtual--;
            if (cfgCalMesAtual < 0) { cfgCalMesAtual = 11; cfgCalAnoAtual--; }
            cfgCalGerar();
        };
        if (proximo)   proximo.onclick  = () => {
            cfgCalMesAtual++;
            if (cfgCalMesAtual > 11) { cfgCalMesAtual = 0; cfgCalAnoAtual++; }
            cfgCalGerar();
        };
        if (confirmar) confirmar.onclick = () => {
            if (!cfgCalDataSelecionada) return;
            if (cfgCalCampo === "inicio") {
                cfgDeleteInicio = cfgCalDataSelecionada;
                const lbl = document.getElementById("cfgDeleteInicioLabel");
                if (lbl) lbl.textContent = cfgCalFormatar(cfgDeleteInicio);
            } else {
                cfgDeleteFim = cfgCalDataSelecionada;
                const lbl = document.getElementById("cfgDeleteFimLabel");
                if (lbl) lbl.textContent = cfgCalFormatar(cfgDeleteFim);
            }
            if (modal) modal.style.display = "none";
        };
    });

    // ── APAGAR REGISTROS ─────────────────────────────────────────
    window.apagarRegistrosPeriodo = function () {
        if (!cfgDeleteInicio || !cfgDeleteFim) {
            mostrarAlerta("Escolha a data inicial e a data final do período.");
            return;
        }
        if (cfgDeleteInicio > cfgDeleteFim) {
            mostrarAlerta("A data inicial não pode ser maior que a data final.");
            return;
        }
        if (!cfgId) {
            mostrarAlerta("Configuração do usuário não carregada.");
            return;
        }

        mostrarConfirmacao(
            `Apagar todos os registros de ${cfgCalFormatar(cfgDeleteInicio)} até ${cfgCalFormatar(cfgDeleteFim)}? Esta ação é irreversível.`,
            async () => {
                try {
                    const res = await fetch(`${API}/configuracoes/${cfgId}/dados/periodo-tempo`, {
                        method: "DELETE",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ dataInical: cfgDeleteInicio, dataFinal: cfgDeleteFim })
                    });
                    if (res.status === 204) {
                        mostrarAlerta("Registros do período apagados com sucesso!");
                        cfgDeleteInicio = cfgDeleteFim = null;
                        const lI = document.getElementById("cfgDeleteInicioLabel");
                        const lF = document.getElementById("cfgDeleteFimLabel");
                        if (lI) lI.textContent = "";
                        if (lF) lF.textContent = "";
                    } else {
                        const txt = await res.text();
                        mostrarAlerta(`Erro ao apagar registros (HTTP ${res.status}): ${txt}`);
                    }
                } catch (e) {
                    console.error("Erro ao apagar registros por período:", e);
                    mostrarAlerta("Erro ao apagar registros do período.");
                }
            }
        );
    };

    window.apagarTodosRegistros = function () {
        if (!userId) {
            mostrarAlerta("Usuário não identificado.");
            return;
        }

        mostrarConfirmacao(
            "Apagar TODOS os registros do usuário? Esta ação é irreversível.",
            async () => {
                try {
                    const res = await fetch(`${API}/configuracoes/usuarios/${userId}/dados/deletar-tudo`, {
                        method: "DELETE"
                    });
                    if (res.status === 204) {
                        mostrarAlerta("Todos os registros foram apagados com sucesso!");
                    } else {
                        const txt = await res.text();
                        mostrarAlerta(`Erro ao apagar registros (HTTP ${res.status}): ${txt}`);
                    }
                } catch (e) {
                    console.error("Erro ao apagar todos os registros:", e);
                    mostrarAlerta("Erro ao apagar todos os registros.");
                }
            }
        );
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
