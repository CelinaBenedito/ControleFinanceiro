(function () {
    const API = "http://localhost:8080";
    const usuarioLogado = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
    const userId = usuarioLogado?.id;

    let cfgId = null;
    let cfgData = null;   // armazena config completa carregada do servidor
    let instituicoes = [];
    let categorias = [];

    // ── helpers ──────────────────────────────────────────────────
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

    // ── INIT ─────────────────────────────────────────────────────
    async function init() {
        if (!userId) {
            window.location.href = "login.html";
            return;
        }
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
            linhas.push({ inst: l.instituicao?.nome || "-", cat: "-", limite: l.limiteDesejado });
        });
        limitesCateg.forEach(l => {
            linhas.push({ inst: "-", cat: l.categoria?.titulo || "-", limite: l.limiteDesejado });
        });

        if (linhas.length === 0) {
            tbody.innerHTML = `<tr class="cfg-table-empty"><td colspan="4" style="padding:20px;">Nenhum limite configurado ainda.</td></tr>`;
            return;
        }
        linhas.forEach(l => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td>${l.inst}</td><td>${l.cat}</td><td>R$ ${Number(l.limite).toFixed(2)}</td><td>-</td>`;
            tbody.appendChild(tr);
        });
    }

    window.salvarMesFiscal = async function () {
        const sel = document.getElementById("mesFiscal");
        if (!sel?.value) {
            alert("Selecione o início do mês fiscal.");
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
                if (!res.ok) { alert(`Erro ao salvar (HTTP ${res.status}).`); return; }
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
            alert("Configurações salvas!");
        } catch (e) {
            alert("Erro ao salvar configurações.");
            console.error(e);
        }
    };

    window.salvarLimite = async function () {
        if (!cfgId) {
            alert("Salve o mês fiscal primeiro para criar as configurações.");
            return;
        }
        const instId = document.getElementById("selInstituicaoLimite")?.value;
        const catId = document.getElementById("selCategoriaLimite")?.value;
        const valor = parseFloat(document.getElementById("ipt_limite_valor")?.value);

        if (!instId && !catId) {
            alert("Selecione uma instituição ou categoria para definir o limite.");
            return;
        }
        if (!valor || valor <= 0) {
            alert("Informe um valor de limite válido.");
            return;
        }

        // Preserva valores atuais para evitar sobrescrita com null no backend
        const payload = {};
        if (cfgData?.inicioMesFiscal != null) payload.inicioMesFiscal = cfgData.inicioMesFiscal;
        if (cfgData?.limiteDesejadoMensal != null) payload.limiteDesejadoMensal = cfgData.limiteDesejadoMensal;
        if (instId) payload.limitesInstituicao = [{ instituicaoId: parseInt(instId), valor }];
        if (catId) payload.limitesCategoria = [{ categoriaId: parseInt(catId), valor }];

        try {
            const res = await putJson(`${API}/configuracoes/edit/${cfgId}`, payload);
            if (res.ok) {
                await carregarConfig();
                alert("Limite salvo!");
            } else {
                const body = await res.json().catch(() => ({}));
                alert(`Erro ao salvar limite (HTTP ${res.status}): ${body.message || ""}`);
            }
        } catch (e) {
            alert("Erro ao salvar limite.");
            console.error(e);
        }
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
            tdMod.textContent = "-";
            tr.appendChild(tdMod);

            const tdAcoes = document.createElement("td");
            const btn = document.createElement("button");
            btn.className = "cfg-btn danger";
            btn.textContent = "Desvincular";
            btn.addEventListener("click", async () => {
                try {
                    const res = await fetch(`${API}/instituicoes/${inst.intituicao.id}/usuarios/${userId}`, { method: "PATCH" });
                    if (!res.ok) { alert(`Erro ao desvincular (HTTP ${res.status}).`); return; }
                    await carregarInstituicoes();
                } catch (e) {
                    alert("Erro ao desvincular instituição.");
                    console.error(e);
                }
            });
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
        if (!instId) { alert("Selecione uma instituição."); return; }
        try {
            const res = await fetch(`${API}/instituicoes/${instId}/usuarios/${userId}`, { method: "POST" });
            if (!res.ok) { alert(`Erro ao adicionar instituição (HTTP ${res.status}).`); return; }
            sel.value = "";
            await carregarInstituicoes();
        } catch (e) {
            alert("Erro ao adicionar instituição.");
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
        } catch (e) {
            console.error("Erro ao carregar categorias:", e);
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
            tdMod.textContent = "-";
            tr.appendChild(tdMod);

            const tdAcoes = document.createElement("td");
            const btn = document.createElement("button");
            btn.className = "cfg-btn danger";
            btn.textContent = "Remover";
            btn.addEventListener("click", async () => {
                try {
                    const res = await fetch(`${API}/categorias/${cat.categoria.id}/usuarios/${userId}`, { method: "PATCH" });
                    if (!res.ok) { alert(`Erro ao remover (HTTP ${res.status}).`); return; }
                    await carregarCategorias();
                } catch (e) {
                    alert("Erro ao remover categoria.");
                    console.error(e);
                }
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
        const input = document.getElementById("ipt_nova_categoria");
        const titulo = input?.value?.trim();
        if (!titulo) { alert("Informe o nome da categoria."); return; }
        try {
            const res = await postJson(`${API}/categorias/usuario/${userId}`, { titulo });
            if (!res.ok) { alert("Erro ao criar categoria."); return; }
            input.value = "";
            await carregarCategorias();
        } catch (e) {
            alert("Erro ao adicionar categoria.");
            console.error(e);
        }
    };

    window.removerCategoria = async function (catId) {
        try {
            await fetch(`${API}/categorias/${catId}/usuarios/${userId}`, { method: "PATCH" });
            await carregarCategorias();
        } catch (e) {
            alert("Erro ao remover categoria.");
            console.error(e);
        }
    };

    // ── EVENTOS (backend não implementado) ────────────────────────
    window.importarDados = function () {
        document.getElementById("inputImportar")?.click();
    };
    window.exportarDados = function () {
        alert("Exportação de dados ainda não disponível.");
    };
    window.processarImportacao = function () {
        alert("Importação de dados ainda não disponível.");
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
