// ============================================================================
// EMPRÉSTIMOS - JavaScript
// ============================================================================

const API_BASE = 'http://localhost:8080';
let usuarioLogado = null;
let emprestimos = [];
let emprestimoEmEdicao = null;
let emprestimoParaPagamento = null;
let filtroAtual = 'todos';

// ── Inicialização ──
window.addEventListener('DOMContentLoaded', () => {
    const userData = localStorage.getItem('usuarioLogado');
    if (!userData) {
        window.location.href = 'index.html';
        return;
    }
    usuarioLogado = JSON.parse(userData);

    carregarResumo();
    carregarEmprestimos();
});

// ============================================================================
// CARREGAR DADOS
// ============================================================================

async function carregarResumo() {
    try {
        const response = await fetch(`${API_BASE}/emprestimos/usuarios/${usuarioLogado.id}/resumo`);
        if (!response.ok) throw new Error('Erro ao carregar resumo');

        const resumo = await response.json();

        document.getElementById('valorAReceber').textContent = formatarMoeda(resumo.valorTotalAReceber);
        document.getElementById('detalhesReceber').textContent =
            `${resumo.quantidadePessoasDevem} pessoa(s)`;

        document.getElementById('valorAPagar').textContent = formatarMoeda(resumo.valorTotalAPagar);
        document.getElementById('detalhesPagar').textContent =
            `${resumo.quantidadePessoasDevo} pessoa(s)`;
    } catch (error) {
        console.error('Erro ao carregar resumo:', error);
        mostrarNotificacao('Erro ao carregar resumo', 'erro');
    }
}

async function carregarEmprestimos() {
    try {
        const response = await fetch(`${API_BASE}/emprestimos/usuarios/${usuarioLogado.id}`);

        if (response.status === 204) {
            emprestimos = [];
            renderizarEmprestimos();
            return;
        }

        if (!response.ok) throw new Error('Erro ao carregar empréstimos');

        emprestimos = await response.json();
        renderizarEmprestimos();
    } catch (error) {
        console.error('Erro ao carregar empréstimos:', error);
        mostrarNotificacao('Erro ao carregar empréstimos', 'erro');
    }
}

// ============================================================================
// RENDERIZAÇÃO
// ============================================================================

function renderizarEmprestimos() {
    const container = document.getElementById('listaEmprestimos');

    let emprestimosFiltrados = [...emprestimos];

    // Aplicar filtros
    switch (filtroAtual) {
        case 'EMPRESTEI':
            emprestimosFiltrados = emprestimos.filter(e => e.tipo === 'EMPRESTEI');
            break;
        case 'PEDI_EMPRESTADO':
            emprestimosFiltrados = emprestimos.filter(e => e.tipo === 'PEDI_EMPRESTADO');
            break;
        case 'pendentes':
            emprestimosFiltrados = emprestimos.filter(e =>
                e.status === 'PENDENTE' || e.status === 'PAGO_PARCIAL');
            break;
        case 'quitados':
            emprestimosFiltrados = emprestimos.filter(e => e.status === 'QUITADO');
            break;
    }

    if (emprestimosFiltrados.length === 0) {
        container.innerHTML = `
            <div class="estado-vazio">
                <i class='bx bx-wallet'></i>
                <h3>Nenhum empréstimo encontrado</h3>
                <p>Clique em "Novo Empréstimo" para registrar um empréstimo.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = emprestimosFiltrados.map(e => criarCardEmprestimo(e)).join('');
}

function criarCardEmprestimo(emprestimo) {
    const tipoClass = emprestimo.tipo === 'EMPRESTEI' ? 'emprestei' : 'pedi-emprestado';
    const tipoLabel = emprestimo.tipo === 'EMPRESTEI' ? 'Emprestei' : 'Peguei Emprestado';
    const tipoIcon = emprestimo.tipo === 'EMPRESTEI' ? 'bx-down-arrow-circle' : 'bx-up-arrow-circle';

    const statusClass = emprestimo.status.toLowerCase().replace('_', '-');
    const statusLabel = formatarStatus(emprestimo.status);
    const statusIcon = getStatusIcon(emprestimo.status);

    const isQuitado = emprestimo.status === 'QUITADO';

    let dataInfo = '';
    if (emprestimo.dataPrevisao) {
        const dataPrevisao = new Date(emprestimo.dataPrevisao);
        const hoje = new Date();
        const atrasado = !isQuitado && dataPrevisao < hoje;
        const icon = atrasado ? 'bx-error-circle' : 'bx-calendar';
        const classe = atrasado ? 'style="color: #ef4444;"' : '';
        dataInfo = `
            <div class="emprestimo-data" ${classe}>
                <i class='bx ${icon}'></i>
                Previsão: ${formatarData(emprestimo.dataPrevisao)}
                ${atrasado ? '(Atrasado!)' : ''}
            </div>
        `;
    }

    const progressoHtml = emprestimo.status !== 'QUITADO' ? `
        <div class="emprestimo-progresso">
            <div class="progresso-bar-container">
                <div class="progresso-bar" style="width: ${emprestimo.percentualPago}%"></div>
            </div>
            <div class="progresso-texto">
                ${emprestimo.percentualPago.toFixed(1)}% pago
            </div>
        </div>
    ` : '';

    const observacoesHtml = emprestimo.observacoes ? `
        <div class="emprestimo-observacoes">
            <strong>Observações:</strong> ${emprestimo.observacoes}
        </div>
    ` : '';

    const botoesAcao = isQuitado ? `
        <button class="btn-acao reabrir" onclick="reabrirEmprestimo('${emprestimo.id}')">
            <i class='bx bx-refresh'></i> Reabrir
        </button>
        <button class="btn-acao deletar" onclick="confirmarDeletar('${emprestimo.id}')">
            <i class='bx bx-trash'></i> Deletar
        </button>
    ` : `
        <button class="btn-acao registrar" onclick="abrirModalPagamento('${emprestimo.id}')">
            <i class='bx bx-money'></i> Registrar Pagamento
        </button>
        <button class="btn-acao quitar" onclick="quitarEmprestimo('${emprestimo.id}')">
            <i class='bx bx-check-circle'></i> Quitar
        </button>
        <button class="btn-acao editar" onclick="editarEmprestimo('${emprestimo.id}')">
            <i class='bx bx-edit'></i> Editar
        </button>
        <button class="btn-acao deletar" onclick="confirmarDeletar('${emprestimo.id}')">
            <i class='bx bx-trash'></i> Deletar
        </button>
    `;

    return `
        <div class="emprestimo-item ${tipoClass} ${isQuitado ? 'quitado' : ''}">
            <div class="emprestimo-header">
                <div class="emprestimo-info">
                    <div class="emprestimo-tipo ${tipoClass}">
                        <i class='bx ${tipoIcon}'></i>
                        ${tipoLabel}
                    </div>
                    <h3 class="emprestimo-pessoa">${emprestimo.pessoaOuGrupo}</h3>
                    ${dataInfo}
                </div>
                <div class="emprestimo-status ${statusClass}">
                    <i class='bx ${statusIcon}'></i>
                    ${statusLabel}
                </div>
            </div>
            
            <div class="emprestimo-valores">
                <div class="emprestimo-valor-item">
                    <span class="emprestimo-valor-label">Valor Total</span>
                    <span class="emprestimo-valor-numero">${formatarMoeda(emprestimo.valorTotal)}</span>
                </div>
                <div class="emprestimo-valor-item">
                    <span class="emprestimo-valor-label">Valor Pago</span>
                    <span class="emprestimo-valor-numero">${formatarMoeda(emprestimo.valorPago)}</span>
                </div>
                <div class="emprestimo-valor-item">
                    <span class="emprestimo-valor-label">Valor Restante</span>
                    <span class="emprestimo-valor-numero">${formatarMoeda(emprestimo.valorRestante)}</span>
                </div>
            </div>
            
            ${progressoHtml}
            ${observacoesHtml}
            
            <div class="emprestimo-acoes-item">
                ${botoesAcao}
            </div>
        </div>
    `;
}

// ============================================================================
// FILTROS
// ============================================================================

function filtrarEmprestimos(filtro) {
    filtroAtual = filtro;

    // Atualizar botões ativos
    document.querySelectorAll('.btn-filtro').forEach(btn => {
        btn.classList.remove('ativo');
    });
    document.querySelector(`[data-filtro="${filtro}"]`).classList.add('ativo');

    renderizarEmprestimos();
}

// ============================================================================
// MODAIS
// ============================================================================

function abrirModalNovoEmprestimo() {
    emprestimoEmEdicao = null;
    document.getElementById('modalTitulo').textContent = 'Novo Empréstimo';
    document.getElementById('formEmprestimo').reset();
    document.getElementById('modalEmprestimo').classList.add('aberto');
}

function editarEmprestimo(emprestimoId) {
    emprestimoEmEdicao = emprestimos.find(e => e.id === emprestimoId);
    if (!emprestimoEmEdicao) return;

    document.getElementById('modalTitulo').textContent = 'Editar Empréstimo';
    document.getElementById('inputTipo').value = emprestimoEmEdicao.tipo;
    document.getElementById('inputPessoa').value = emprestimoEmEdicao.pessoaOuGrupo;
    document.getElementById('inputValor').value = emprestimoEmEdicao.valorTotal;
    document.getElementById('inputDataPrevisao').value = emprestimoEmEdicao.dataPrevisao || '';
    document.getElementById('inputObservacoes').value = emprestimoEmEdicao.observacoes || '';

    document.getElementById('modalEmprestimo').classList.add('aberto');
}

function fecharModalEmprestimo() {
    document.getElementById('modalEmprestimo').classList.remove('aberto');
    emprestimoEmEdicao = null;
}

function abrirModalPagamento(emprestimoId) {
    emprestimoParaPagamento = emprestimos.find(e => e.id === emprestimoId);
    if (!emprestimoParaPagamento) return;

    document.getElementById('infoPagamento').innerHTML = `
        <strong>Empréstimo:</strong> ${emprestimoParaPagamento.pessoaOuGrupo}<br>
        <strong>Valor Total:</strong> ${formatarMoeda(emprestimoParaPagamento.valorTotal)}<br>
        <strong>Já Pago:</strong> ${formatarMoeda(emprestimoParaPagamento.valorPago)}<br>
        <strong>Restante:</strong> ${formatarMoeda(emprestimoParaPagamento.valorRestante)}
    `;

    document.getElementById('formPagamento').reset();
    document.getElementById('modalPagamento').classList.add('aberto');
}

function fecharModalPagamento() {
    document.getElementById('modalPagamento').classList.remove('aberto');
    emprestimoParaPagamento = null;
}

// ============================================================================
// OPERAÇÕES
// ============================================================================

async function salvarEmprestimo(event) {
    event.preventDefault();

    const dados = {
        usuarioId: usuarioLogado.id,
        tipo: document.getElementById('inputTipo').value,
        pessoaOuGrupo: document.getElementById('inputPessoa').value,
        valorTotal: parseFloat(document.getElementById('inputValor').value),
        dataPrevisao: document.getElementById('inputDataPrevisao').value || null,
        observacoes: document.getElementById('inputObservacoes').value || null
    };

    try {
        let response;
        if (emprestimoEmEdicao) {
            response = await fetch(`${API_BASE}/emprestimos/${emprestimoEmEdicao.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(dados)
            });
        } else {
            response = await fetch(`${API_BASE}/emprestimos`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(dados)
            });
        }

        if (!response.ok) throw new Error('Erro ao salvar empréstimo');

        mostrarNotificacao(
            emprestimoEmEdicao ? 'Empréstimo atualizado!' : 'Empréstimo criado!',
            'sucesso'
        );

        fecharModalEmprestimo();
        await carregarResumo();
        await carregarEmprestimos();
    } catch (error) {
        console.error('Erro ao salvar:', error);
        mostrarNotificacao('Erro ao salvar empréstimo', 'erro');
    }
}

async function salvarPagamento(event) {
    event.preventDefault();

    const valorPagamento = parseFloat(document.getElementById('inputValorPagamento').value);

    if (valorPagamento > emprestimoParaPagamento.valorRestante) {
        mostrarNotificacao('Valor não pode ser maior que o restante!', 'erro');
        return;
    }

    try {
        const response = await fetch(
            `${API_BASE}/emprestimos/${emprestimoParaPagamento.id}/pagamento-parcial`,
            {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ valorPago: valorPagamento })
            }
        );

        if (!response.ok) throw new Error('Erro ao registrar pagamento');

        mostrarNotificacao('Pagamento registrado!', 'sucesso');
        fecharModalPagamento();
        await carregarResumo();
        await carregarEmprestimos();
    } catch (error) {
        console.error('Erro ao registrar pagamento:', error);
        mostrarNotificacao('Erro ao registrar pagamento', 'erro');
    }
}

async function quitarEmprestimo(emprestimoId) {
    if (!confirm('Deseja marcar este empréstimo como quitado?')) return;

    try {
        const response = await fetch(`${API_BASE}/emprestimos/${emprestimoId}/quitar`, {
            method: 'PATCH'
        });

        if (!response.ok) throw new Error('Erro ao quitar empréstimo');

        mostrarNotificacao('Empréstimo quitado!', 'sucesso');
        await carregarResumo();
        await carregarEmprestimos();
    } catch (error) {
        console.error('Erro ao quitar:', error);
        mostrarNotificacao('Erro ao quitar empréstimo', 'erro');
    }
}

async function reabrirEmprestimo(emprestimoId) {
    if (!confirm('Deseja reabrir este empréstimo?')) return;

    try {
        const response = await fetch(`${API_BASE}/emprestimos/${emprestimoId}/reabrir`, {
            method: 'PATCH'
        });

        if (!response.ok) throw new Error('Erro ao reabrir empréstimo');

        mostrarNotificacao('Empréstimo reaberto!', 'sucesso');
        await carregarResumo();
        await carregarEmprestimos();
    } catch (error) {
        console.error('Erro ao reabrir:', error);
        mostrarNotificacao('Erro ao reabrir empréstimo', 'erro');
    }
}

async function confirmarDeletar(emprestimoId) {
    if (!confirm('Deseja realmente deletar este empréstimo? Esta ação não pode ser desfeita.')) return;

    try {
        const response = await fetch(`${API_BASE}/emprestimos/${emprestimoId}`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Erro ao deletar empréstimo');

        mostrarNotificacao('Empréstimo deletado!', 'sucesso');
        await carregarResumo();
        await carregarEmprestimos();
    } catch (error) {
        console.error('Erro ao deletar:', error);
        mostrarNotificacao('Erro ao deletar empréstimo', 'erro');
    }
}

// ============================================================================
// UTILITÁRIOS
// ============================================================================

function formatarMoeda(valor) {
    return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    }).format(valor);
}

function formatarData(dataStr) {
    if (!dataStr) return '';
    const data = new Date(dataStr + 'T00:00:00');
    return data.toLocaleDateString('pt-BR');
}

function formatarStatus(status) {
    const statusMap = {
        'PENDENTE': 'Pendente',
        'PAGO_PARCIAL': 'Pago Parcial',
        'QUITADO': 'Quitado'
    };
    return statusMap[status] || status;
}

function getStatusIcon(status) {
    const iconMap = {
        'PENDENTE': 'bx-time-five',
        'PAGO_PARCIAL': 'bx-info-circle',
        'QUITADO': 'bx-check-circle'
    };
    return iconMap[status] || 'bx-info-circle';
}

function mostrarNotificacao(mensagem, tipo = 'info') {
    // Criar elemento de notificação
    const notif = document.createElement('div');
    notif.className = `notificacao notif-${tipo}`;
    notif.innerHTML = `
        <i class='bx ${tipo === 'sucesso' ? 'bx-check-circle' : 'bx-error-circle'}'></i>
        <span>${mensagem}</span>
    `;

    // Adicionar estilos inline
    Object.assign(notif.style, {
        position: 'fixed',
        top: '20px',
        right: '20px',
        zIndex: '99999',
        background: tipo === 'sucesso' ? '#10b981' : '#ef4444',
        color: '#fff',
        padding: '1rem 1.5rem',
        borderRadius: '10px',
        boxShadow: '0 8px 24px rgba(0,0,0,0.2)',
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem',
        fontSize: '0.95rem',
        fontWeight: '600',
        animation: 'slideInRight 0.3s ease'
    });

    document.body.appendChild(notif);

    setTimeout(() => {
        notif.style.animation = 'slideOutRight 0.3s ease';
        setTimeout(() => notif.remove(), 300);
    }, 3000);
}

// Adicionar estilos de animação
const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    @keyframes slideOutRight {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Fechar modal ao clicar fora
window.onclick = function(event) {
    const modalEmprestimo = document.getElementById('modalEmprestimo');
    const modalPagamento = document.getElementById('modalPagamento');

    if (event.target === modalEmprestimo) {
        fecharModalEmprestimo();
    }
    if (event.target === modalPagamento) {
        fecharModalPagamento();
    }
};

