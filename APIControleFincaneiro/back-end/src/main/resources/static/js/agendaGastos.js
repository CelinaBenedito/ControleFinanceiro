(function () {
    const MESES = [
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    ];

    const formatadorMoeda = new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    });

    const usuarioLogado = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
    const userId = usuarioLogado ? usuarioLogado.id : null;

    let hoje = new Date();
    hoje.setHours(0, 0, 0, 0);
    let anoAtual = hoje.getFullYear();
    let mesAtual = hoje.getMonth();

    let eventosPorDia = {};

    function classificar(registro) {
        const tipo = registro.eventoFinanceiro && registro.eventoFinanceiro.tipo
            ? registro.eventoFinanceiro.tipo.toLowerCase()
            : "";
        const dataEvento = registro.eventoFinanceiro && registro.eventoFinanceiro.dataEvento
            ? new Date(registro.eventoFinanceiro.dataEvento + "T00:00:00")
            : null;

        if (tipo === "transferencia") return "transferencia";
        if (tipo === "recebimento") return "recebimento";
        if (tipo === "gasto") {
            if (dataEvento && dataEvento > hoje) return "agendamento";
            return "gasto";
        }
        return "gasto";
    }

    function toLocalDateKey(isoString) {
        return isoString.substring(0, 10);
    }

    function carregarEventos() {
        if (!userId) {
            renderizarCalendario();
            return Promise.resolve();
        }
        return MainAPI.carregarRegistros(userId)
            .then(registros => {
                eventosPorDia = {};
                registros.forEach(r => {
                    const dataISO = r.eventoFinanceiro && r.eventoFinanceiro.dataEvento;
                    if (!dataISO) return;
                    const key = toLocalDateKey(dataISO);
                    if (!eventosPorDia[key]) eventosPorDia[key] = [];
                    eventosPorDia[key].push(r);
                });
                renderizarCalendario();
            })
            .catch(() => {
                renderizarCalendario();
            });
    }

    function renderizarCalendario() {
        const grid = document.getElementById("calGrid");
        const nomeMes = document.getElementById("mesNome");

        nomeMes.textContent = MESES[mesAtual] + " " + anoAtual;
        grid.innerHTML = "";

        const primeiroDia = new Date(anoAtual, mesAtual, 1).getDay();
        const diasNoMes = new Date(anoAtual, mesAtual + 1, 0).getDate();
        const todayKey = hoje.getFullYear() + "-" + String(hoje.getMonth() + 1).padStart(2, "0") + "-" + String(hoje.getDate()).padStart(2, "0");

        for (let i = 0; i < primeiroDia; i++) {
            const vazio = document.createElement("div");
            vazio.className = "ag-day ag-empty";
            grid.appendChild(vazio);
        }

        for (let dia = 1; dia <= diasNoMes; dia++) {
            const key = anoAtual + "-" + String(mesAtual + 1).padStart(2, "0") + "-" + String(dia).padStart(2, "0");
            const cell = document.createElement("div");
            cell.className = "ag-day";
            if (key === todayKey) cell.classList.add("ag-today");
            cell.dataset.key = key;

            const numEl = document.createElement("div");
            numEl.className = "ag-day-num";
            numEl.textContent = dia;
            cell.appendChild(numEl);

            const eventos = eventosPorDia[key] || [];
            if (eventos.length > 0) {
                const dotsEl = document.createElement("div");
                dotsEl.className = "ag-dots";
                const tiposVistos = new Set();
                eventos.forEach(function(r) {
                    const tipo = classificar(r);
                    if (!tiposVistos.has(tipo)) {
                        tiposVistos.add(tipo);
                        const dot = document.createElement("div");
                        dot.className = "ag-dot ag-dot-" + tipo;
                        dotsEl.appendChild(dot);
                    }
                });
                cell.appendChild(dotsEl);
            }

            cell.addEventListener("click", (function(k, d) {
                return function() { selecionarDia(k, d); };
            })(key, dia));
            grid.appendChild(cell);
        }
    }

    function selecionarDia(key, dia) {
        document.querySelectorAll(".ag-day.ag-selected").forEach(function(el) { el.classList.remove("ag-selected"); });
        const cell = document.querySelector(".ag-day[data-key='" + key + "']");
        if (cell) cell.classList.add("ag-selected");

        const empty = document.getElementById("resumoEmpty");
        const conteudo = document.getElementById("resumoConteudo");
        const eventos = eventosPorDia[key] || [];

        conteudo.innerHTML = "";

        if (eventos.length === 0) {
            empty.style.display = "";
            empty.textContent = "Nenhum evento neste dia.";
            return;
        }

        empty.style.display = "none";

        const partes = key.split("-");
        const dataHeader = document.createElement("div");
        dataHeader.className = "ag-resumo-data";
        dataHeader.textContent = partes[2] + "/" + partes[1] + "/" + partes[0];
        conteudo.appendChild(dataHeader);

        eventos.forEach(function(r) {
            const tipo = classificar(r);
            const titulo = r.gastoDetalhe ? r.gastoDetalhe.tituloGasto : "-";
            const valor = r.eventoFinanceiro ? (r.eventoFinanceiro.valor || 0) : 0;
            const chip = document.createElement("div");
            chip.className = "ag-evento " + tipo;
            const prefix = tipo === "recebimento" ? "+" : tipo === "transferencia" ? "" : "-";
            chip.innerHTML = '<span class="ag-evento-titulo">' + titulo + '</span>'
                + '<span class="ag-evento-valor">' + prefix + formatadorMoeda.format(Math.abs(valor)) + '</span>';
            conteudo.appendChild(chip);
        });
    }

    function mostrarMesCompleto() {
        const empty = document.getElementById("resumoEmpty");
        const conteudo = document.getElementById("resumoConteudo");
        conteudo.innerHTML = "";

        const prefixo = anoAtual + "-" + String(mesAtual + 1).padStart(2, "0") + "-";
        const chavesDoMes = Object.keys(eventosPorDia)
            .filter(function(k) { return k.startsWith(prefixo); })
            .sort();

        if (chavesDoMes.length === 0) {
            empty.style.display = "";
            empty.textContent = "Nenhum evento neste mês.";
            return;
        }

        empty.style.display = "none";

        chavesDoMes.forEach(function(key) {
            const partes = key.split("-");
            const dataHeader = document.createElement("div");
            dataHeader.className = "ag-resumo-data";
            dataHeader.textContent = partes[2] + "/" + partes[1] + "/" + partes[0];
            conteudo.appendChild(dataHeader);

            eventosPorDia[key].forEach(function(r) {
                const tipo = classificar(r);
                const titulo = r.gastoDetalhe ? r.gastoDetalhe.tituloGasto : "-";
                const valor = r.eventoFinanceiro ? (r.eventoFinanceiro.valor || 0) : 0;
                const chip = document.createElement("div");
                chip.className = "ag-evento " + tipo;
                const prefix = tipo === "recebimento" ? "+" : tipo === "transferencia" ? "" : "-";
                chip.innerHTML = '<span class="ag-evento-titulo">' + titulo + '</span>'
                    + '<span class="ag-evento-valor">' + prefix + formatadorMoeda.format(Math.abs(valor)) + '</span>';
                conteudo.appendChild(chip);
            });
        });
    }

    document.getElementById("btnPrevMes").addEventListener("click", function() {
        mesAtual--;
        if (mesAtual < 0) { mesAtual = 11; anoAtual--; }
        renderizarCalendario();
        mostrarMesCompleto();
    });

    document.getElementById("btnNextMes").addEventListener("click", function() {
        mesAtual++;
        if (mesAtual > 11) { mesAtual = 0; anoAtual++; }
        renderizarCalendario();
        mostrarMesCompleto();
    });

    carregarEventos().then(function() { mostrarMesCompleto(); });
})();