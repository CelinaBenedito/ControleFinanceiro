(function () {
    const MESES = [
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    ];

    const formatadorMoeda = new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    });

    let hoje = new Date();
    let anoAtual = hoje.getFullYear();
    let mesAtual = hoje.getMonth();

    // All loaded records indexed by "YYYY-MM-DD"
    let eventosPorDia = {};

    // Classify a record into a dot/event type
    function classificar(registro) {
        const tipo = (registro.tituloTipo || "").toLowerCase();
        const titulo = (registro.tituloGasto || "").toLowerCase();
        if (tipo.includes("transfer") || titulo.includes("transfer")) {
            return "transferencia";
        }
        if (tipo.includes("agenda") || tipo.includes("agend")) {
            return "agendamento";
        }
        if (registro.valor > 0 ||
            tipo.includes("receb") ||
            tipo.includes("salário") ||
            tipo.includes("salario") ||
            tipo.includes("renda")) {
            return "recebimento";
        }
        return "gasto";
    }

    function toLocalDateKey(isoString) {
        // Backend returns dates like "2026-04-01T00:00:00" or "2026-04-01"
        return isoString.substring(0, 10);
    }

    function carregarEventos() {
        return MainAPI.carregarRegistros()
            .then(registros => {
                eventosPorDia = {};
                registros.forEach(r => {
                    const key = toLocalDateKey(r.dataGasto);
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

        nomeMes.textContent = `${MESES[mesAtual]} ${anoAtual}`;
        grid.innerHTML = "";

        const primeiroDia = new Date(anoAtual, mesAtual, 1).getDay(); // 0=Sun
        const diasNoMes = new Date(anoAtual, mesAtual + 1, 0).getDate();
        const todayKey = toLocalDateKey(hoje.toISOString());

        // Empty cells before first day
        for (let i = 0; i < primeiroDia; i++) {
            const vazio = document.createElement("div");
            vazio.className = "ag-day ag-empty";
            grid.appendChild(vazio);
        }

        for (let dia = 1; dia <= diasNoMes; dia++) {
            const key = `${anoAtual}-${String(mesAtual + 1).padStart(2, "0")}-${String(dia).padStart(2, "0")}`;
            const cell = document.createElement("div");
            cell.className = "ag-day";
            if (key === todayKey) cell.classList.add("ag-today");
            cell.dataset.key = key;
            cell.dataset.dia = dia;

            // Day number
            const numEl = document.createElement("div");
            numEl.className = "ag-day-num";
            numEl.textContent = dia;
            cell.appendChild(numEl);

            // Dots
            const eventos = eventosPorDia[key] || [];
            if (eventos.length > 0) {
                const dotsEl = document.createElement("div");
                dotsEl.className = "ag-dots";
                const tiposVistos = new Set();
                eventos.forEach(r => {
                    const tipo = classificar(r);
                    if (!tiposVistos.has(tipo)) {
                        tiposVistos.add(tipo);
                        const dot = document.createElement("div");
                        dot.className = `ag-dot ag-dot-${tipo}`;
                        dotsEl.appendChild(dot);
                    }
                });
                cell.appendChild(dotsEl);
            }

            cell.addEventListener("click", () => selecionarDia(key, dia));
            grid.appendChild(cell);
        }
    }

    function selecionarDia(key, dia) {
        // Highlight selected
        document.querySelectorAll(".ag-day.ag-selected").forEach(el => el.classList.remove("ag-selected"));
        const cell = document.querySelector(`.ag-day[data-key="${key}"]`);
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

        const [ano, mes, d] = key.split("-");
        const dataHeader = document.createElement("div");
        dataHeader.className = "ag-resumo-data";
        dataHeader.textContent = `${d}/${mes}/${ano}`;
        conteudo.appendChild(dataHeader);

        eventos.forEach(r => {
            const tipo = classificar(r);
            const chip = document.createElement("div");
            chip.className = `ag-evento ${tipo}`;

            const prefix = tipo === "recebimento" ? "+" : tipo === "transferencia" ? "" : "-";
            const valorAbs = Math.abs(r.valor);

            chip.innerHTML = `
                <span class="ag-evento-titulo">${r.tituloGasto}</span>
                <span class="ag-evento-valor">${prefix}${formatadorMoeda.format(valorAbs)}</span>
            `;
            conteudo.appendChild(chip);
        });
    }

    function mostrarMesCompleto() {
        const empty = document.getElementById("resumoEmpty");
        const conteudo = document.getElementById("resumoConteudo");
        conteudo.innerHTML = "";

        const chavesDoMes = Object.keys(eventosPorDia)
            .filter(k => k.startsWith(`${anoAtual}-${String(mesAtual + 1).padStart(2, "0")}-`))
            .sort();

        if (chavesDoMes.length === 0) {
            empty.style.display = "";
            empty.textContent = "Nenhum evento neste mês.";
            return;
        }

        empty.style.display = "none";

        chavesDoMes.forEach(key => {
            const [ano, mes, d] = key.split("-");
            const dataHeader = document.createElement("div");
            dataHeader.className = "ag-resumo-data";
            dataHeader.textContent = `${d}/${mes}/${ano}`;
            conteudo.appendChild(dataHeader);

            eventosPorDia[key].forEach(r => {
                const tipo = classificar(r);
                const chip = document.createElement("div");
                chip.className = `ag-evento ${tipo}`;
                const prefix = tipo === "recebimento" ? "+" : tipo === "transferencia" ? "" : "-";
                const valorAbs = Math.abs(r.valor);
                chip.innerHTML = `
                    <span class="ag-evento-titulo">${r.tituloGasto}</span>
                    <span class="ag-evento-valor">${prefix}${formatadorMoeda.format(valorAbs)}</span>
                `;
                conteudo.appendChild(chip);
            });
        });
    }

    // Navigation
    document.getElementById("btnPrevMes").addEventListener("click", () => {
        mesAtual--;
        if (mesAtual < 0) { mesAtual = 11; anoAtual--; }
        renderizarCalendario();
        mostrarMesCompleto();
    });

    document.getElementById("btnNextMes").addEventListener("click", () => {
        mesAtual++;
        if (mesAtual > 11) { mesAtual = 0; anoAtual++; }
        renderizarCalendario();
        mostrarMesCompleto();
    });

    // Initialize
    carregarEventos().then(() => mostrarMesCompleto());
})();
