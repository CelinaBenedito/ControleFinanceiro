(function () {
    const LOCAL_API = "http://localhost:8080";

    function buildUrl(path) {
        if (/^https?:\/\//i.test(path)) {
            return path;
        }
        return `${LOCAL_API}${path}`;
    }

    function request(path, options) {
        return fetch(buildUrl(path), options);
    }

    function get(path) {
        return request(path, { method: "GET" }).then(res => res.json());
    }

    function postJson(path, payload) {
        return request(path, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });
    }

    function formatarLocalDateTime(valor, vazio = "-") {
        if (!valor) return vazio;

        if (Array.isArray(valor) && valor.length >= 5) {
            const [ano, mes, dia, hora = 0, min = 0] = valor;
            return `${String(dia).padStart(2, "0")}/${String(mes).padStart(2, "0")}/${ano} ${String(hora).padStart(2, "0")}:${String(min).padStart(2, "0")}`;
        }

        if (typeof valor === "string") {
            const dt = new Date(valor);
            if (!isNaN(dt.getTime())) {
                const d = String(dt.getDate()).padStart(2, "0");
                const m = String(dt.getMonth() + 1).padStart(2, "0");
                const y = dt.getFullYear();
                const h = String(dt.getHours()).padStart(2, "0");
                const mi = String(dt.getMinutes()).padStart(2, "0");
                return `${d}/${m}/${y} ${h}:${mi}`;
            }
        }

        if (typeof valor === "object") {
            const ano = valor.year ?? valor.ano;
            const mes = valor.monthValue ?? valor.mes;
            const dia = valor.dayOfMonth ?? valor.dia;
            const hora = valor.hour ?? valor.hora ?? 0;
            const min = valor.minute ?? valor.minuto ?? 0;
            if (ano && mes && dia) {
                return `${String(dia).padStart(2, "0")}/${String(mes).padStart(2, "0")}/${ano} ${String(hora).padStart(2, "0")}:${String(min).padStart(2, "0")}`;
            }
        }

        return vazio;
    }

    window.MainAPI = {
        request,
        get,
        formatarLocalDateTime,
        getTipos(userId) {
            return request(`/categorias/usuario/${userId}`, { method: "GET" })
                .then(res => {
                    if (res.status === 204) return [];
                    return res.json();
                });
        },
        getInstituicoes(userId) {
            if (userId) {
                return request(`/instituicoes/usuarios/${userId}`, { method: "GET" })
                    .then(res => {
                        if (res.status === 204) return [];
                        return res.json();
                    });
            }
            return get("/instituicoes");
        },
        registrarGasto(payload) {
            return postJson("/registros", payload);
        },
        adicionarSaldo(payload) {
            return this.registrarGasto(payload);
        },
        atualizarSaldo(payload) {
            return this.registrarGasto(payload);
        },
        carregarRegistros(userId) {
            return request(`/registros/${userId}`, { method: "GET" })
                .then(res => {
                    if (res.status === 204) return [];
                    return res.json();
                });
        },
        filtrarRegistros(userId, filtros) {
            const params = new URLSearchParams();

            if (filtros.valor) params.append("valor", filtros.valor);
            if (filtros.dataEvento) params.append("dataEvento", filtros.dataEvento);
            if (filtros.descricao) params.append("descricao", filtros.descricao);
            if (filtros.titulo) params.append("titulo", filtros.titulo);

            (filtros.tipo || []).forEach(v => params.append("tipo", v));
            (filtros.tipoMovimento || []).forEach(v => params.append("tipoMovimento", v));
            (filtros.instituicaoUsuario || []).forEach(v => params.append("instituicaoUsuario", v));
            (filtros.categoriaUsuario || []).forEach(v => params.append("categoriaUsuario", v));

            const query = params.toString();
            const path = query
                ? `/registros/filtro/usuarios/${userId}?${query}`
                : `/registros/filtro/usuarios/${userId}`;

            return request(path, { method: "GET" })
                .then(res => {
                    if (res.status === 204) return [];
                    return res.json();
                });
        },
        adicionarTipo(payload, userId) {
            return postJson(`/categorias/usuario/${userId}`, { titulo: payload.titulo });
        },
        buscarRegistrosPorData(userId, dataSelecionada) {
            return request(`/registros/${userId}`, { method: "GET" })
                .then(res => {
                    if (res.status === 204) return [];
                    return res.json();
                })
                .then(registros => registros.filter(r => {
                    const dataEvento = r.eventoFinanceiro && r.eventoFinanceiro.dataEvento;
                    return dataEvento === dataSelecionada;
                }));
        },
        mostrarSaldoTotal(userId) {
            return request(`/registros/usuario/${userId}/saldo`, { method: "GET" })
                .then(res => res.json())
                .then(data => [{ valorTotal: data.valorTotal }]);
        },
        mostrarTodasInstituicoes(userId) {
            return request(`/registros/usuario/${userId}/saldo/instituicoes`, { method: "GET" })
                .then(res => {
                    if (res.status === 204) return [];
                    return res.json();
                });
        },
        cadastrarUsuario(payload) {
            return postJson("/usuarios", payload);
        },
        loginUsuario(payload) {
            return postJson("/usuarios/login", payload);
        },
        editarRegistro(eventoId, payload) {
            return request(`/registros/${eventoId}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
        },
        deletarRegistro(eventoId) {
            return request(`/registros/${eventoId}`, { method: "DELETE" });
        }
    };
})();
