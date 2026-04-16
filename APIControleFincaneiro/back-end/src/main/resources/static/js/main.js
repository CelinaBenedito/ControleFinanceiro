(function () {
    const LOCAL_API = "http://localhost:8080";

    function buildUrl(path) {
        if (/^https?:\/\//i.test(path)) {
            return path;
        }

        if (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1") {
            return `${LOCAL_API}${path}`;
        }

        return path;
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
        getTipos() {
            return get("/registros/gerarTipos");
        },
        getInstituicoes() {
            return get("/registros/gerarInstituicoes");
        },
        registrarGasto(payload) {
            return postJson("/registros/registrar", payload);
        },
        atualizarSaldo(payload) {
            return postJson("/registros/atualizarSaldo", payload);
        },
        adicionarTipo(payload) {
            return postJson("/registros/adicionarTipo", payload);
        },
        buscarRegistrosPorData(dataSelecionada) {
            return get(`/registros/buscarData/${dataSelecionada}`);
        },
        carregarRegistros() {
            return get("/registros/carregarRegistros");
        },
        adicionarSaldo(payload) {
            return postJson("/registros/adicionarSaldo", payload);
        },
        mostrarSaldoTotal() {
            return get("/registros/mostrarSaldoTotal");
        },
        mostrarTodasInstituicoes() {
            return get("/registros/mostrarTodasInstituicoes");
        },
        cadastrarUsuario(payload) {
            return postJson("/usuarios", payload);
        },
        loginUsuario(payload) {
            return postJson("/usuarios/login", payload);
        }
    };
})();
