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

    window.MainAPI = {
        request,
        get,
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
        }
    };
})();
