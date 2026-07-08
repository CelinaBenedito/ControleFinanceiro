// ══════════════════════════════════════════════════════════════
//  SISTEMA DE PERSONAGEM — Controle Financeiro RPG
// ══════════════════════════════════════════════════════════════
'use strict';

window.PersonagemSistema = (function () {

    const RACAS = [
        { id: 'humano',        nome: 'Humano',                   pelePad: '#F4C4A0', orelha: 'redonda',  asa: false, desc: 'Adaptável e resiliente.' },
        { id: 'goblin',        nome: 'Goblin',                   pelePad: '#7BC44A', orelha: 'grande',   asa: false, desc: 'Esperto e engenhoso.' },
        { id: 'elfo',          nome: 'Elfo',                     pelePad: '#ECD8C0', orelha: 'pontuda',  asa: false, desc: 'Gracioso e milenar.' },
        { id: 'druida',        nome: 'Druida',                   pelePad: '#C8A870', orelha: 'redonda',  asa: false, desc: 'Um com a natureza.' },
        { id: 'elfo_negro',    nome: 'Elfo Negro',               pelePad: '#7B6B9B', orelha: 'pontuda',  asa: false, desc: 'Das profundezas sombrias.' },
        { id: 'elfo_gelo',     nome: 'Elfo da Floresta de Gelo', pelePad: '#C0D8F0', orelha: 'pontuda',  asa: false, desc: 'Filho do inverno eterno.' },
        { id: 'fada',          nome: 'Fada',                     pelePad: '#F9C6D0', orelha: 'pequena',  asa: true,  desc: 'Mágica e encantadora.' },
        { id: 'fada_floresta', nome: 'Fada da Floresta',         pelePad: '#A8D8A8', orelha: 'pequena',  asa: true,  desc: 'Guardiã das árvores.' },
        { id: 'fada_sonhos',   nome: 'Fada dos Sonhos',          pelePad: '#D4B0E8', orelha: 'pequena',  asa: true,  desc: 'Tecelã de sonhos.' },
        { id: 'fada_gelo',     nome: 'Fada do Gelo',             pelePad: '#C8ECF8', orelha: 'pequena',  asa: true,  desc: 'Espírito do inverno.' },
    ];

    const CORES_PELE = [
        { id: 'marfim',  cor: '#F8DCC0', nome: 'Marfim'   },
        { id: 'claro',   cor: '#F4C4A0', nome: 'Claro'    },
        { id: 'dourado', cor: '#E8B870', nome: 'Dourado'  },
        { id: 'moreno',  cor: '#D4936A', nome: 'Moreno'   },
        { id: 'canela',  cor: '#C8824A', nome: 'Canela'   },
        { id: 'escuro',  cor: '#8B5E3C', nome: 'Escuro'   },
        { id: 'ebano',   cor: '#5C3318', nome: 'Ébano'    },
        { id: 'verde',   cor: '#7BC44A', nome: 'Verde'    },
        { id: 'azulado', cor: '#C0D8F0', nome: 'Azulado'  },
        { id: 'roxo',    cor: '#7B6B9B', nome: 'Roxo'     },
        { id: 'rosa',    cor: '#F9C6D0', nome: 'Rosa'     },
        { id: 'gelo',    cor: '#C8ECF8', nome: 'Gelo'     },
    ];

    const CORES_OLHO = [
        { id: 'castanho', cor: '#3D1F0A', nome: 'Castanho' },
        { id: 'preto',    cor: '#0D0D0D', nome: 'Preto'    },
        { id: 'azul',     cor: '#1A5FA0', nome: 'Azul'     },
        { id: 'verde',    cor: '#1E6B48', nome: 'Verde'    },
        { id: 'cinza',    cor: '#5A6878', nome: 'Cinza'    },
        { id: 'roxo',     cor: '#6B3A8A', nome: 'Roxo'     },
        { id: 'ambar',    cor: '#B85C00', nome: 'Âmbar'    },
        { id: 'vermelho', cor: '#8B0000', nome: 'Vermelho' },
        { id: 'rosa',     cor: '#B01870', nome: 'Rosa'     },
        { id: 'dourado',  cor: '#A07000', nome: 'Dourado'  },
    ];

    const CORES_CABELO = [
        { id: 'castanho', cor: '#8B5A2B', nome: 'Castanho' },
        { id: 'preto',    cor: '#1A1A1A', nome: 'Preto'    },
        { id: 'loiro',    cor: '#D4AF37', nome: 'Loiro'    },
        { id: 'ruivo',    cor: '#CC4400', nome: 'Ruivo'    },
        { id: 'branco',   cor: '#F0F0F0', nome: 'Branco'   },
        { id: 'cinza',    cor: '#8A8A8A', nome: 'Cinza'    },
        { id: 'roxo',     cor: '#8B3DB8', nome: 'Roxo'     },
        { id: 'azul',     cor: '#1A5FA0', nome: 'Azul'     },
        { id: 'rosa',     cor: '#FF69B4', nome: 'Rosa'     },
        { id: 'verde',    cor: '#228B22', nome: 'Verde'    },
        { id: 'laranja',  cor: '#FF6B00', nome: 'Laranja'  },
        { id: 'prata',    cor: '#C0C0C0', nome: 'Prata'    },
    ];

    const ESTILOS_FEM = [
        { id: 'liso_longo',     nome: 'Liso Longo',     emoji: '🌿' },
        { id: 'liso_curto',     nome: 'Liso Curto',     emoji: '✂️'  },
        { id: 'cacheado_longo', nome: 'Cacheado Longo', emoji: '🌀'  },
        { id: 'cacheado_curto', nome: 'Cacheado Curto', emoji: '☁️'  },
        { id: 'coque',          nome: 'Coque',          emoji: '🎀'  },
    ];

    const ESTILOS_MASC = [
        { id: 'curto',    nome: 'Curto',    emoji: '💈' },
        { id: 'ondulado', nome: 'Ondulado', emoji: '🌊' },
        { id: 'cacheado', nome: 'Cacheado', emoji: '🌀' },
        { id: 'comprido', nome: 'Comprido', emoji: '🎸' },
        { id: 'moicano',  nome: 'Moicano',  emoji: '⚡'  },
    ];

    const CATALOGO = [
        { id: 'roupa_basica',      tipo: 'roupa',     nome: 'Roupa Básica',          nivelMin: 1,  preco: 0,    c1: '#8B7355', c2: '#6B5335', desc: 'O ponto de partida de todo aventureiro.',      emoji: '👕' },
        { id: 'roupa_aventureiro', tipo: 'roupa',     nome: 'Roupa de Aventureiro',  nivelMin: 3,  preco: 50,   c1: '#4A6741', c2: '#2D4A28', desc: 'Resistente para longas jornadas.',             emoji: '🧥' },
        { id: 'armadura_couro',    tipo: 'roupa',     nome: 'Armadura de Couro',     nivelMin: 8,  preco: 200,  c1: '#8B4513', c2: '#5C2D0A', desc: 'Proteção básica e eficiente.',                 emoji: '🛡️' },
        { id: 'capa_vento',        tipo: 'roupa',     nome: 'Capa do Vento',         nivelMin: 8,  preco: 250,  c1: '#4169E1', c2: '#1A3A8F', desc: 'Flutua graciosamente com a brisa.',            emoji: '🌬️' },
        { id: 'armadura_runa',     tipo: 'roupa',     nome: 'Armadura Rúnica',       nivelMin: 15, preco: 500,  c1: '#2F4F4F', c2: '#7B68EE', desc: 'Gravada com runas de proteção mágica.',       emoji: '⚔️' },
        { id: 'armadura_lendaria', tipo: 'roupa',     nome: 'Armadura Lendária',     nivelMin: 30, preco: 2000, c1: '#DAA520', c2: '#8B6914', desc: 'A armadura dos grandes heróis imortais.',      emoji: '👑' },
        { id: 'chapeu_mago',       tipo: 'acessorio', nome: 'Chapéu de Mago',        nivelMin: 5,  preco: 100,  c1: '#4B0082', c2: '#FFD700', desc: 'Um chapéu pontudo cheio de mistério arcano.', emoji: '🎩' },
        { id: 'colar_magico',      tipo: 'acessorio', nome: 'Colar Mágico',          nivelMin: 5,  preco: 120,  c1: '#00CED1', c2: '#E0FFFF', desc: 'Emana energia mágica constante.',             emoji: '📿' },
        { id: 'coroa_natureza',    tipo: 'acessorio', nome: 'Coroa da Natureza',     nivelMin: 10, preco: 300,  c1: '#228B22', c2: '#FFD700', desc: 'Forjada com galhos vivos e flores eternas.',  emoji: '🌿' },
        { id: 'anel_poder',        tipo: 'acessorio', nome: 'Anel de Poder',         nivelMin: 10, preco: 320,  c1: '#8B0000', c2: '#FFD700', desc: 'Amplifica os poderes mágicos do portador.',   emoji: '💍' },
        { id: 'coroa_astral',      tipo: 'acessorio', nome: 'Coroa Astral',          nivelMin: 25, preco: 1200, c1: '#191970', c2: '#E8E8FF', desc: 'Feita de estrelas cristalizadas do cosmos.',  emoji: '⭐' },
        { id: 'aura_fogo',         tipo: 'poder',     nome: 'Aura de Fogo',          nivelMin: 15, preco: 500,  c1: '#FF4500', c2: '#FFD700', desc: 'Chamas dançam ao redor do personagem.',      emoji: '🔥' },
        { id: 'aura_gelo',         tipo: 'poder',     nome: 'Aura de Gelo',          nivelMin: 20, preco: 800,  c1: '#00BFFF', c2: '#E0F8FF', desc: 'Cristais de gelo flutuam ao redor.',         emoji: '❄️' },
        { id: 'aura_arcana',       tipo: 'poder',     nome: 'Aura Arcana',           nivelMin: 25, preco: 1200, c1: '#8A2BE2', c2: '#DDA0DD', desc: 'Energia arcana pulsa visivelmente no ar.',   emoji: '✨' },
        { id: 'aura_divina',       tipo: 'poder',     nome: 'Aura Divina',           nivelMin: 30, preco: 2000, c1: '#FFD700', c2: '#FFFACD', desc: 'Uma luz divina emana do ser escolhido.',     emoji: '🌟' },
    ];

    function moedasPorNivel(n) { return Math.round(n * 75); }
    function xpParaNivel(n) { return Math.round(500 * Math.pow(n, 1.5)); }
    function calcularNivel(xpTotal) {
        const xp = Math.max(0, Number(xpTotal) || 0);
        let nivel = 1, resto = xp, prox = xpParaNivel(1);
        while (resto >= prox) { resto -= prox; nivel++; prox = xpParaNivel(nivel); }
        return { nivel, xpRestante: resto, xpProximo: prox, progresso: prox > 0 ? (resto / prox) * 100 : 0 };
    }

    function _darken(hex, f) {
        f = f || 0.18;
        try {
            const r = Math.max(0, Math.round(parseInt(hex.slice(1,3),16)*(1-f)));
            const g = Math.max(0, Math.round(parseInt(hex.slice(3,5),16)*(1-f)));
            const b = Math.max(0, Math.round(parseInt(hex.slice(5,7),16)*(1-f)));
            return '#'+[r,g,b].map(v=>v.toString(16).padStart(2,'0')).join('');
        } catch(_) { return '#888'; }
    }

    function _chave() {
        const u = JSON.parse(localStorage.getItem('usuarioLogado') || 'null');
        return u ? `personagem_${u.id}` : null;
    }
    function getPersonagem() { const k = _chave(); return k ? JSON.parse(localStorage.getItem(k)||'null') : null; }
    function salvarPersonagem(d) { const k = _chave(); if (k) localStorage.setItem(k, JSON.stringify(d)); }

    function criarPersonagem(raca, genero, custom) {
        custom = custom || {};
        const racaDados = RACAS.find(r => r.id === raca) || RACAS[0];
        const d = {
            raca, genero,
            corPele:      custom.corPele      || racaDados.pelePad,
            corOlho:      custom.corOlho      || '#3D1F0A',
            corCabelo:    custom.corCabelo     || '#8B5A2B',
            estiloCabelo: custom.estiloCabelo  || (genero === 'feminino' ? 'liso_longo' : 'curto'),
            nivel: 1, moedas: 150,
            itensComprados: ['roupa_basica'],
            equipados: { roupa: 'roupa_basica', acessorio: null, poder: null },
            criadoEm: Date.now(),
        };
        salvarPersonagem(d);
        return d;
    }

    async function sincronizarNivel() {
        const u = JSON.parse(localStorage.getItem('usuarioLogado')||'null');
        const p = getPersonagem();
        if (!u||!p) return null;
        try {
            const res = await fetch(`http://localhost:8080/usuarios/calculo-xp/${u.id}`);
            if (!res.ok) return null;
            const { nivel } = calcularNivel(Number(await res.json()));
            const ant = p.nivel||1;
            if (nivel > ant) {
                let ganho = 0;
                for (let n = ant+1; n <= nivel; n++) ganho += moedasPorNivel(n);
                p.nivel = nivel; p.moedas = (p.moedas||0) + ganho;
                salvarPersonagem(p);
                return { subiuNivel: true, novoNivel: nivel, moedasGanhas: ganho };
            }
            p.nivel = nivel; salvarPersonagem(p);
        } catch(_) {}
        return null;
    }

    function comprarItem(itemId) {
        const p = getPersonagem();
        if (!p) return { ok: false, msg: 'Nenhum personagem encontrado.' };
        const item = CATALOGO.find(i => i.id === itemId);
        if (!item) return { ok: false, msg: 'Item inexistente.' };
        if (p.nivel < item.nivelMin) return { ok: false, msg: `Nível ${item.nivelMin} necessário.` };
        if ((p.moedas||0) < item.preco) return { ok: false, msg: 'Moedas insuficientes.' };
        if ((p.itensComprados||[]).includes(itemId)) return { ok: false, msg: 'Item já adquirido.' };
        p.moedas -= item.preco;
        p.itensComprados = [...(p.itensComprados||[]), itemId];
        salvarPersonagem(p);
        return { ok: true, msg: `${item.nome} adquirido!`, moedas: p.moedas };
    }

    function equiparItem(itemId) {
        const p = getPersonagem();
        if (!p||(!(p.itensComprados||[]).includes(itemId))) return false;
        const item = CATALOGO.find(i => i.id === itemId);
        if (!item) return false;
        p.equipados = p.equipados||{};
        p.equipados[item.tipo] = p.equipados[item.tipo] === itemId ? null : itemId;
        salvarPersonagem(p);
        return true;
    }

    // ══════════════════════════════════════════════════════════
    //  CABELO — Camada traseira (renderizada ANTES da cabeça)
    // ══════════════════════════════════════════════════════════
    function _hairBack(estilo, cab, fem) {
        if (!fem) {
            // Masculino: só o "comprido" tem traseiro
            if (estilo === 'comprido') return `<rect x="58" y="40" width="84" height="118" rx="20" fill="${cab}" opacity="0.88"/>`;
            return '';
        }
        // Feminino
        switch (estilo) {
            case 'liso_curto':
                return `<rect x="60" y="46" width="80" height="70" rx="18" fill="${cab}" opacity="0.88"/>`;
            case 'cacheado_longo':
                return `
                <ellipse cx="52"  cy="102" rx="24" ry="52" fill="${cab}" opacity="0.85"/>
                <ellipse cx="148" cy="102" rx="24" ry="52" fill="${cab}" opacity="0.85"/>
                <ellipse cx="56"  cy="158" rx="19" ry="26" fill="${cab}" opacity="0.80"/>
                <ellipse cx="144" cy="158" rx="19" ry="26" fill="${cab}" opacity="0.80"/>`;
            case 'cacheado_curto':
                // Afrô: halo atrás bem acima e nas laterais
                return `
                <ellipse cx="100" cy="66" rx="58" ry="40" fill="${cab}" opacity="0.88"/>
                <ellipse cx="48"  cy="74" rx="22" ry="30" fill="${cab}" opacity="0.85"/>
                <ellipse cx="152" cy="74" rx="22" ry="30" fill="${cab}" opacity="0.85"/>`;
            case 'coque':
                return '';
            case 'liso_longo':
            default:
                return `<rect x="56" y="44" width="88" height="138" rx="22" fill="${cab}" opacity="0.88"/>`;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  CABELO — Franja/topo (renderizada APÓS o rosto, clipada
    //  para não cobrir os olhos — clip até y=63)
    // ══════════════════════════════════════════════════════════
    function _hairFront(estilo, cab, fem) {
        const clip = `clip-path="url(#clipFringe)"`;
        let inner = '';

        if (!fem) {
            switch (estilo) {
                case 'ondulado':
                    inner = `
                    <ellipse cx="100" cy="34" rx="48" ry="22" fill="${cab}"/>
                    <path d="M57,50 Q70,42 84,49 Q98,56 100,47 Q102,38 116,47 Q130,56 143,50 L143,64 L57,64Z" fill="${cab}"/>`;
                    break;
                case 'cacheado':
                    inner = `
                    <ellipse cx="100" cy="34" rx="50" ry="26" fill="${cab}"/>
                    <ellipse cx="70"  cy="42" rx="21" ry="19" fill="${cab}"/>
                    <ellipse cx="130" cy="42" rx="21" ry="19" fill="${cab}"/>
                    <ellipse cx="84"  cy="34" rx="16" ry="21" fill="${cab}"/>
                    <ellipse cx="116" cy="34" rx="16" ry="21" fill="${cab}"/>
                    <ellipse cx="100" cy="28" rx="18" ry="22" fill="${cab}"/>`;
                    break;
                case 'comprido':
                    inner = `
                    <ellipse cx="100" cy="36" rx="47" ry="21" fill="${cab}"/>
                    <rect x="57" y="40" width="86" height="26" rx="5" fill="${cab}"/>`;
                    break;
                case 'moicano':
                    inner = `<rect x="88" y="20" width="24" height="46" rx="10" fill="${cab}"/>`;
                    break;
                case 'curto':
                default:
                    inner = `
                    <ellipse cx="100" cy="36" rx="47" ry="21" fill="${cab}"/>
                    <rect x="57" y="40" width="86" height="24" rx="5" fill="${cab}"/>
                    <ellipse cx="76"  cy="54" rx="17" ry="12" fill="${cab}" transform="rotate(-12 76 54)"/>
                    <ellipse cx="124" cy="54" rx="17" ry="12" fill="${cab}" transform="rotate(12 124 54)"/>`;
            }
        } else {
            switch (estilo) {
                case 'liso_curto':
                    inner = `
                    <ellipse cx="100" cy="35" rx="46" ry="20" fill="${cab}"/>
                    <ellipse cx="78"  cy="51" rx="19" ry="14" fill="${cab}" transform="rotate(-16 78 51)"/>
                    <ellipse cx="100" cy="46" rx="15" ry="12" fill="${cab}"/>
                    <ellipse cx="122" cy="51" rx="18" ry="13" fill="${cab}" transform="rotate(12 122 51)"/>`;
                    break;
                case 'cacheado_longo':
                    inner = `
                    <ellipse cx="100" cy="30" rx="52" ry="26" fill="${cab}"/>
                    <ellipse cx="72"  cy="40" rx="23" ry="20" fill="${cab}"/>
                    <ellipse cx="128" cy="40" rx="23" ry="20" fill="${cab}"/>
                    <ellipse cx="84"  cy="34" rx="17" ry="23" fill="${cab}"/>
                    <ellipse cx="116" cy="34" rx="17" ry="23" fill="${cab}"/>
                    <ellipse cx="100" cy="25" rx="19" ry="21" fill="${cab}"/>`;
                    break;
                case 'cacheado_curto':
                    inner = `
                    <ellipse cx="100" cy="28" rx="54" ry="30" fill="${cab}"/>
                    <ellipse cx="72"  cy="36" rx="22" ry="24" fill="${cab}"/>
                    <ellipse cx="128" cy="36" rx="22" ry="24" fill="${cab}"/>
                    <ellipse cx="100" cy="20" rx="24" ry="22" fill="${cab}"/>
                    <ellipse cx="84"  cy="28" rx="16" ry="20" fill="${cab}"/>
                    <ellipse cx="116" cy="28" rx="16" ry="20" fill="${cab}"/>`;
                    break;
                case 'coque':
                    inner = `
                    <ellipse cx="100" cy="36" rx="46" ry="20" fill="${cab}"/>
                    <rect    x="60"   y="38"  width="80" height="22" rx="4"  fill="${cab}"/>
                    <circle  cx="100" cy="20" r="20"  fill="${cab}"/>
                    <circle  cx="100" cy="20" r="13"  fill="${_darken(cab,0.08)}"/>`;
                    break;
                case 'liso_longo':
                default:
                    inner = `
                    <ellipse cx="100" cy="33" rx="50" ry="24" fill="${cab}"/>
                    <ellipse cx="78"  cy="52" rx="21" ry="15" fill="${cab}" transform="rotate(-18 78 52)"/>
                    <ellipse cx="100" cy="46" rx="16" ry="13" fill="${cab}"/>
                    <ellipse cx="122" cy="52" rx="19" ry="14" fill="${cab}" transform="rotate(14 122 52)"/>`;
            }
        }
        return `<g ${clip}>${inner}</g>`;
    }

    // ══════════════════════════════════════════════════════════
    //  SVG PRINCIPAL
    // ══════════════════════════════════════════════════════════
    function _svg(raca, genero, roupa, acess, poder, tam, custom) {
        const W = 200, H = 280;
        const fem  = genero === 'feminino';
        const pele = custom.corPele   || raca.pelePad;
        const pelD = _darken(pele, 0.18);
        const olho = custom.corOlho   || '#3D1F0A';
        const cab  = custom.corCabelo  || '#8B5A2B';
        const est  = custom.estiloCabelo || (fem ? 'liso_longo' : 'curto');
        const rC   = roupa ? roupa.c1 : '#8B7355';
        const rD   = roupa ? roupa.c2 : '#6B5335';

        // Orelhas
        let ore = '';
        if (raca.orelha==='pontuda') {
            ore = `<polygon points="56,52 44,22 68,50" fill="${pele}" stroke="${pelD}" stroke-width="1.2"/>
                   <polygon points="144,52 156,22 132,50" fill="${pele}" stroke="${pelD}" stroke-width="1.2"/>`;
        } else if (raca.orelha==='grande') {
            ore = `<polygon points="52,58 32,14 74,54" fill="${pele}" stroke="${pelD}" stroke-width="1"/>
                   <polygon points="148,58 168,14 126,54" fill="${pele}" stroke="${pelD}" stroke-width="1"/>`;
        } else if (raca.orelha==='pequena') {
            ore = `<polygon points="58,54 52,40 70,52" fill="${pele}" stroke="${pelD}" stroke-width="1"/>
                   <polygon points="142,54 148,40 130,52" fill="${pele}" stroke="${pelD}" stroke-width="1"/>`;
        } else {
            ore = `<ellipse cx="56" cy="76" rx="9" ry="11" fill="${pele}" stroke="${pelD}" stroke-width="1"/>
                   <ellipse cx="144" cy="76" rx="9" ry="11" fill="${pele}" stroke="${pelD}" stroke-width="1"/>`;
        }

        // Asas
        let asa = '';
        if (raca.asa) {
            const aC = raca.id==='fada_floresta' ? '#78C878' : raca.id==='fada_gelo' ? '#A8EEFF' : raca.id==='fada_sonhos' ? '#CC99FF' : '#FFB6C1';
            asa = `<ellipse cx="52"  cy="148" rx="38" ry="22" fill="${aC}" opacity="0.55" transform="rotate(-35 52 148)"/>
                   <ellipse cx="52"  cy="165" rx="26" ry="14" fill="${aC}" opacity="0.45" transform="rotate(-20 52 165)"/>
                   <ellipse cx="148" cy="148" rx="38" ry="22" fill="${aC}" opacity="0.55" transform="rotate(35 148 148)"/>
                   <ellipse cx="148" cy="165" rx="26" ry="14" fill="${aC}" opacity="0.45" transform="rotate(20 148 165)"/>`;
        }

        // Roupa overlay
        let rOv = '';
        const rId = roupa ? roupa.id : '';
        if (rId==='armadura_couro') rOv = `<rect x="78" y="123" width="44" height="60" rx="5" fill="${rD}" opacity="0.45"/><line x1="100" y1="122" x2="100" y2="183" stroke="${rD}" stroke-width="2" opacity="0.5"/>`;
        else if (rId==='capa_vento') rOv = `<path d="M62,120 Q46,170 52,238 Q78,205 100,192 Q122,205 148,238 Q154,170 138,120" fill="${rD}" opacity="0.55"/>`;
        else if (rId==='armadura_runa') rOv = `<rect x="76" y="120" width="48" height="65" rx="4" fill="${rD}" opacity="0.35"/><text x="88" y="150" font-size="13" fill="#9B89FF" opacity="0.95" font-family="serif">ᚱᚢᚾ</text>`;
        else if (rId==='armadura_lendaria') rOv = `<rect x="73" y="118" width="54" height="67" rx="6" fill="${rD}" opacity="0.4"/><polygon points="100,124 106,138 100,135 94,138" fill="#FFD700" opacity="0.95"/>`;

        // Acessório
        let aHtml = '';
        if (acess) {
            const c1=acess.c1, c2=acess.c2;
            if (acess.id==='chapeu_mago') aHtml=`<polygon points="100,6 72,52 128,52" fill="${c1}"/><rect x="66" y="52" width="68" height="11" rx="5" fill="${c1}"/><circle cx="100" cy="10" r="6" fill="${c2}"/>`;
            else if (acess.id==='colar_magico') aHtml=`<path d="M82,122 Q100,132 118,122" stroke="${c1}" stroke-width="2.5" fill="none"/><circle cx="100" cy="132" r="6" fill="${c1}"/><circle cx="100" cy="132" r="3" fill="${c2}"/>`;
            else if (acess.id==='coroa_natureza') aHtml=`<rect x="72" y="30" width="56" height="13" rx="5" fill="${c1}"/><line x1="82" y1="30" x2="78" y2="16" stroke="${c1}" stroke-width="3.5"/><line x1="100" y1="30" x2="100" y2="13" stroke="${c1}" stroke-width="3.5"/><line x1="118" y1="30" x2="122" y2="16" stroke="${c1}" stroke-width="3.5"/><circle cx="78" cy="14" r="5" fill="${c2}"/><circle cx="100" cy="11" r="5" fill="${c2}"/><circle cx="122" cy="14" r="5" fill="${c2}"/>`;
            else if (acess.id==='anel_poder') aHtml=`<circle cx="150" cy="162" r="7" fill="${c1}" stroke="${c2}" stroke-width="2"/><circle cx="150" cy="162" r="3.5" fill="${c2}"/>`;
            else if (acess.id==='coroa_astral') aHtml=`<rect x="70" y="33" width="60" height="11" rx="4" fill="${c1}"/><polygon points="80,33 83,16 86,33" fill="${c1}"/><polygon points="97,33 100,11 103,33" fill="${c1}"/><polygon points="114,33 117,16 120,33" fill="${c1}"/><circle cx="83" cy="15" r="3.5" fill="${c2}"/><circle cx="100" cy="9" r="4.5" fill="${c2}"/><circle cx="117" cy="15" r="3.5" fill="${c2}"/>`;
        }

        // Poder
        let pHtml = '';
        if (poder) {
            const c1=poder.c1, c2=poder.c2;
            if (poder.id==='aura_fogo') pHtml=`<circle cx="100" cy="135" r="92" fill="none" stroke="${c1}" stroke-width="3" opacity="0.35" stroke-dasharray="8,5"/><circle cx="28" cy="72" r="6" fill="${c2}" opacity="0.75"/><circle cx="172" cy="82" r="5" fill="${c1}" opacity="0.65"/>`;
            else if (poder.id==='aura_gelo') pHtml=`<circle cx="100" cy="135" r="92" fill="none" stroke="${c1}" stroke-width="2" opacity="0.45" stroke-dasharray="5,4"/><polygon points="22,60 27,76 38,60 27,44" fill="${c1}" opacity="0.55"/><polygon points="172,70 177,86 188,70 177,54" fill="${c1}" opacity="0.55"/>`;
            else if (poder.id==='aura_arcana') pHtml=`<circle cx="100" cy="135" r="93" fill="${c1}" opacity="0.05"/><circle cx="100" cy="135" r="91" fill="none" stroke="${c1}" stroke-width="2.5" opacity="0.55" stroke-dasharray="7,3"/><text x="22" y="52" font-size="14" fill="${c1}" opacity="0.8">✦</text><text x="165" y="58" font-size="12" fill="${c2}" opacity="0.7">✦</text>`;
            else if (poder.id==='aura_divina') pHtml=`<circle cx="100" cy="135" r="91" fill="none" stroke="${c1}" stroke-width="3" opacity="0.45"/><line x1="100" y1="16" x2="100" y2="50" stroke="${c1}" stroke-width="2.5" opacity="0.7"/><text x="86" y="16" font-size="18" fill="${c1}" opacity="0.9">★</text>`;
        }

        const goblin = raca.id==='goblin';
        const corpo  = goblin ? `<rect x="78" y="122" width="44" height="60" rx="8" fill="${rC}"/>` : `<rect x="70" y="118" width="60" height="68" rx="9" fill="${rC}"/>`;
        const bochechas = (raca.asa||raca.orelha==='pontuda'||raca.orelha==='pequena')
            ? `<ellipse cx="78" cy="84" rx="9" ry="5.5" fill="#FFB6C1" opacity="0.45"/><ellipse cx="122" cy="84" rx="9" ry="5.5" fill="#FFB6C1" opacity="0.45"/>` : '';

        // ── Cabelo separado: back (antes do corpo) e front (clipped, depois do rosto)
        const hBack  = _hairBack(est, cab, fem);
        const hFront = _hairFront(est, cab, fem);

        return `<svg viewBox="0 0 ${W} ${H}" xmlns="http://www.w3.org/2000/svg" width="${tam}" height="${Math.round(tam*H/W)}" style="display:block;">
            <defs>
                <!-- Limita a franja ao topo do rosto (y < 63), acima dos olhos em cy=74 -->
                <clipPath id="clipFringe">
                    <rect x="30" y="0" width="140" height="63"/>
                </clipPath>
            </defs>
            ${pHtml}
            ${asa}
            <!-- ── Cabelo traseiro (ANTES das pernas e corpo para ficar atrás) ── -->
            ${hBack}
            <!-- Pernas -->
            <rect x="76"  y="186" width="22" height="58" rx="9" fill="${rC}"/>
            <rect x="102" y="186" width="22" height="58" rx="9" fill="${rC}"/>
            <rect x="72"  y="236" width="30" height="12" rx="6" fill="${rD}"/>
            <rect x="98"  y="236" width="30" height="12" rx="6" fill="${rD}"/>
            <!-- Corpo -->
            ${corpo}
            ${rOv}
            <!-- Braços e Mãos -->
            <rect x="44"  y="120" width="27" height="60" rx="11" fill="${pele}"/>
            <rect x="129" y="120" width="27" height="60" rx="11" fill="${pele}"/>
            <circle cx="57"  cy="182" r="12" fill="${pele}"/>
            <circle cx="143" cy="182" r="12" fill="${pele}"/>
            <!-- Orelhas -->
            ${ore}
            <!-- ── Cabeça (sobre o cabelo traseiro) ── -->
            <ellipse cx="100" cy="76" rx="46" ry="44" fill="${pele}"/>
            <ellipse cx="100" cy="96" rx="32" ry="24" fill="${pelD}" opacity="0.10"/>
            <!-- Olhos -->
            <ellipse cx="85"  cy="74" rx="8" ry="9" fill="white"/>
            <ellipse cx="115" cy="74" rx="8" ry="9" fill="white"/>
            <circle cx="87"  cy="75" r="5.5" fill="${olho}"/>
            <circle cx="117" cy="75" r="5.5" fill="${olho}"/>
            <circle cx="88.5" cy="73" r="2" fill="white"/>
            <circle cx="118.5" cy="73" r="2" fill="white"/>
            <!-- Boca -->
            <path d="M91,92 Q100,100 109,92" stroke="${pelD}" stroke-width="2.2" fill="none" stroke-linecap="round"/>
            ${bochechas}
            <!-- ── Franja (clipada acima dos olhos) ── -->
            ${hFront}
            ${aHtml}
        </svg>`;
    }

    function renderizarPersonagem(container, opcoes) {
        if (!container) return;
        opcoes = opcoes || {};
        const p = getPersonagem();
        if (!p) {
            container.innerHTML = `<div class="char-vazio"><i class='bx bx-ghost'></i><span>Sem personagem</span></div>`;
            return;
        }
        const raca  = RACAS.find(r => r.id === p.raca) || RACAS[0];
        const eq    = p.equipados || {};
        const roupa = CATALOGO.find(i => i.id === eq.roupa)     || CATALOGO[0];
        const acess = CATALOGO.find(i => i.id === eq.acessorio) || null;
        const poder = CATALOGO.find(i => i.id === eq.poder)     || null;
        container.innerHTML = _svg(raca, p.genero||'feminino', roupa, acess, poder, opcoes.tamanho||200, {
            corPele:      p.corPele      || raca.pelePad,
            corOlho:      p.corOlho      || '#3D1F0A',
            corCabelo:    p.corCabelo     || '#8B5A2B',
            estiloCabelo: p.estiloCabelo  || (p.genero==='feminino' ? 'liso_longo' : 'curto'),
        });
    }

    function renderizarPersonagemCustom(container, racaId, genero, tam, custom) {
        if (!container) return;
        custom = custom || {};
        const raca  = RACAS.find(r => r.id === racaId) || RACAS[0];
        container.innerHTML = _svg(raca, genero||'feminino', CATALOGO[0], null, null, tam||100, {
            corPele:      custom.corPele      || raca.pelePad,
            corOlho:      custom.corOlho      || '#3D1F0A',
            corCabelo:    custom.corCabelo     || '#8B5A2B',
            estiloCabelo: custom.estiloCabelo  || (genero==='feminino' ? 'liso_longo' : 'curto'),
        });
    }

    // ══════════════════════════════════════════════════════════
    //  ANIMAÇÕES DE COMEMORAÇÃO
    // ══════════════════════════════════════════════════════════
    let _ultimaCelebracao = 0;

    function triggerCelebracao(containerId) {
        const container = document.getElementById(containerId || 'mascoteAddReg');
        if (!container) return;

        // Evita disparar múltiplas vezes em sequência
        const agora = Date.now();
        if (agora - _ultimaCelebracao < 1200) return;
        _ultimaCelebracao = agora;

        const svg = container.querySelector('svg');
        if (!svg) return;

        // Alterna entre 2 animações
        const tipo = Math.random() < 0.5 ? 1 : 2;
        const classe = `char-celebrate-${tipo}`;

        svg.classList.remove('char-celebrate-1', 'char-celebrate-2');
        void svg.offsetWidth; // reflow para reiniciar animação
        svg.classList.add(classe);
        svg.addEventListener('animationend', () => svg.classList.remove(classe), { once: true });

        // Gera partículas ao redor do personagem
        _gerarParticulas(container, tipo);
    }

    const _EMOJIS_CELEBRACAO = ['⭐','✨','🎉','💫','🌟','🎊','💥','🎈'];
    const _EMOJIS_VITORIA    = ['🏆','💪','🎯','🔥','⚡','🌈','💎','👑'];

    function _gerarParticulas(container, tipo) {
        const rect    = container.getBoundingClientRect();
        const emojis  = tipo === 1 ? _EMOJIS_CELEBRACAO : _EMOJIS_VITORIA;
        const count   = 8;

        for (let i = 0; i < count; i++) {
            const p = document.createElement('div');
            p.className = `char-particle char-particle-${tipo}`;
            p.textContent = emojis[Math.floor(Math.random() * emojis.length)];

            // Posição inicial centrada no personagem
            const cx = rect.left + rect.width / 2;
            const cy = rect.top  + rect.height / 2;
            const angle  = (i / count) * 360 + Math.random() * 30;
            const dist   = 40 + Math.random() * 50;
            const rad    = angle * Math.PI / 180;
            const tx     = Math.cos(rad) * dist;
            const ty     = Math.sin(rad) * dist;
            const dur    = 700 + Math.random() * 500;
            const delay  = Math.random() * 200;
            const size   = 14 + Math.random() * 12;

            p.style.cssText = `
                position: fixed;
                left: ${cx}px;
                top: ${cy}px;
                font-size: ${size}px;
                line-height: 1;
                pointer-events: none;
                z-index: 99999;
                animation: charParticula ${dur}ms ease-out ${delay}ms forwards;
                --tx: ${tx}px;
                --ty: ${ty}px;`;

            document.body.appendChild(p);
            setTimeout(() => p.remove(), dur + delay + 100);
        }
    }

    return {
        RACAS, CATALOGO,
        CORES_PELE, CORES_OLHO, CORES_CABELO,
        ESTILOS_FEM, ESTILOS_MASC,
        moedasPorNivel, calcularNivel,
        getPersonagem, salvarPersonagem, criarPersonagem,
        sincronizarNivel, comprarItem, equiparItem,
        renderizarPersonagem, renderizarPersonagemCustom,
        triggerCelebracao,
    };
})();



