import './styles.css'
import {
  firebaseConfigured,
  inspectUserAccess,
  loginWithEmail,
  logout,
  observeAuth,
  resendVerificationEmail,
  requestPasswordReset,
} from './firebase.js'
import { apiFetch } from './api.js'

const root = document.querySelector('#app')

const state = {
  user: null,
  authContext: null,
  accessGate: null,
  devSession: null,
  page: 'dashboard',
  loading: false,
  toast: null,
  health: 'unknown',
  // Gestão do Clube: assinantes e barbeiros já cadastrados, e o rateio calculado
  // a partir dos atendimentos reais (não é mais um simulador com dados soltos).
  clube: {
    carregado: false,
    assinantes: [],
    barbeiros: [],
    assinanteSelecionadoId: '',
    atendimentos: [],
    resultado: null,
    calculando: false,
    mostrarNovoAssinante: false,
    mostrarNovoBarbeiro: false,
  },
}

const menu = [
  ['dashboard', 'Visão geral', '⌂', ['GERENTE', 'ADMINISTRATIVO', 'BARBEIRO']],
  ['clube', 'Gestão do Clube', '◒', ['GERENTE', 'ADMINISTRATIVO', 'BARBEIRO']],
  ['assinantes', 'Assinantes', '♙', ['GERENTE', 'ADMINISTRATIVO', 'BARBEIRO']],
  ['rotina', 'Pagamentos e rotina', '↻', ['GERENTE', 'ADMINISTRATIVO', 'BARBEIRO']],
  ['relatorios', 'Relatórios', '▥', ['GERENTE']],
  ['logs', 'Logs', '≡', ['GERENTE']],
  ['equipe', 'Equipe', '♢', ['GERENTE']],
]

function normalizeRole(role) {
  const normalized = String(role || '').trim().toUpperCase()
  return {
    MANAGER: 'GERENTE',
    ADMINISTRATIVE: 'ADMINISTRATIVO',
    BARBER: 'BARBEIRO',
  }[normalized] || (['GERENTE', 'ADMINISTRATIVO', 'BARBEIRO'].includes(normalized) ? normalized : null)
}

function activeRole() {
  return state.devSession?.role || state.authContext?.role || null
}

function visibleMenu() {
  const role = activeRole()
  return menu.filter(([, , , roles]) => roles.includes(role))
}

function escapeHtml(value = '') {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function money(value) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(Number(value || 0))
}

// O ciclo de vigência da assinatura dura 30 dias (mesma regra do backend, ver
// Assinante.DURACAO_CICLO_DIAS). Aqui só formatamos/comparamos datas em cima do
// que a API já calcula e devolve (cicloInicio); o "fim" é sempre derivado dele.
const DURACAO_CICLO_DIAS = 30

function cicloFimIso(cicloInicioIso) {
  const fim = new Date(cicloInicioIso)
  fim.setUTCDate(fim.getUTCDate() + DURACAO_CICLO_DIAS)
  return fim.toISOString()
}

function estaNoCicloVigente(dataHoraIso, cicloInicioIso) {
  const t = new Date(dataHoraIso).getTime()
  return t >= new Date(cicloInicioIso).getTime() && t < new Date(cicloFimIso(cicloInicioIso)).getTime()
}

function paraInputDate(isoInstant) {
  return isoInstant ? isoInstant.slice(0, 10) : ''
}

function deInputDate(yyyyMmDd) {
  return `${yyyyMmDd}T00:00:00.000Z`
}

function formatarDataCurta(isoInstant) {
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(new Date(isoInstant))
}

function setToast(message, type = 'success') {
  state.toast = { message, type }
  render()
  window.setTimeout(() => {
    state.toast = null
    render()
  }, 4200)
}

function loginView() {
  return `
    <main class="auth-shell">
      <section class="auth-story" aria-label="Apresentação do Clube 3 Barbas">
        <div class="brand brand--light brand--hero">
          <img class="brand__logo" src="/assets/3barbas_logo.png" alt="3 Barbas — Visagismo e Barbearia" />
        </div>
        <div class="auth-story__content">
          <h1>Bem-vindo ao <strong class="auth-story__name">Clube 3 Barbas</strong></h1>
          <p>Assinaturas, visitas e repasses acompanhados do primeiro pagamento ao fechamento de cada ciclo.</p>
        </div>
      </section>

      <section class="auth-panel">
        <div class="auth-card">
          <div class="auth-card__heading">
          <h2>Entre com sua conta da equipe</h2>
       

          <form id="login-form" class="form-stack">
            <label>E-mail
              <input name="email" type="email" autocomplete="username" placeholder="nome@3barbas.com.br" required />
            </label>
            <label>Senha
              <span class="password-field">
                <input name="password" type="password" autocomplete="current-password" placeholder="Sua senha" minlength="8" required />
                <button type="button" class="text-button" data-action="toggle-password" aria-label="Mostrar ou ocultar senha">Ver</button>
              </span>
            </label>
            <button class="button button--primary" type="submit" ${state.loading ? 'disabled' : ''}>
              ${state.loading ? 'Entrando…' : 'Entrar no portal'}
            </button>
          </form>

          <button class="link-button" data-action="forgot-password">Esqueci minha senha</button>
          ${import.meta.env.DEV ? `
            <div class="dev-access">
              <span>Desenvolvimento local</span>
              <button class="button button--secondary" data-action="dev-login">Acessar como Gerente</button>
            </div>
          ` : ''}
          <p class="auth-help">Problemas com o e-mail ou a senha? Entre em contato com o gerente responsável.</p>
        </div>
      </section>
    </main>
  `
}

function onboardingView() {
  const gate = state.accessGate || { type: 'checking' }
  const email = escapeHtml(state.user?.email || '')
  let content = ''

  if (gate.type === 'checking') {
    content = `<div class="access-state"><span class="spinner"></span><h2>Validando seu acesso</h2><p>Estamos conferindo o e-mail e o perfil.</p></div>`
  } else if (gate.type === 'verify-email') {
    content = `
      <div class="auth-card__heading"><p class="eyebrow">Primeiro acesso</p><h2>Confirme seu e-mail</h2><p>Enviaremos o link de verificação para <strong>${email}</strong>.</p></div>
      <div class="form-stack">
        <button class="button button--primary" data-action="resend-verification">Enviar link de verificação</button>
        <button class="button button--secondary" data-action="refresh-access">Já confirmei o e-mail</button>
      </div>
    `
  } else if (gate.type === 'missing-role') {
    content = `<div class="access-state"><span class="access-state__icon">!</span><h2>Perfil ainda não liberado</h2><p>Sua conta existe, mas o Gerente ainda precisa atribuir um perfil de acesso.</p><button class="button button--secondary" data-action="logout">Sair</button></div>`
  } else {
    content = `<div class="access-state"><span class="access-state__icon">!</span><h2>Não foi possível validar o acesso</h2><p>${escapeHtml(gate.message || 'Entre em contato com o responsável pelo sistema.')}</p><button class="button button--secondary" data-action="logout">Voltar ao login</button></div>`
  }

  return `
    <main class="auth-shell">
      <section class="auth-story" aria-label="Cadastro seguro do Clube 3 Barbas">
        <div class="brand brand--light brand--hero"><img class="brand__logo" src="/assets/3barbas_logo.png" alt="3 Barbas — Visagismo e Barbearia" /></div>
        <div class="auth-story__content"><p class="eyebrow">Primeiro acesso</p><h1>Sua conta.<br />Seu acesso.</h1><p>Confirme seu e-mail para proteger os dados financeiros e operacionais do Clube.</p></div>
      </section>
      <section class="auth-panel"><div class="auth-card">${content}<p class="auth-help">A função de acesso não pode ser escolhida pelo usuário. Apenas um Gerente pode atribuí-la.</p></div></section>
    </main>
  `
}

function dashboardView() {
  const health = {
    up: ['API acessível', 'status-pill--up'],
    down: ['API indisponível', 'status-pill--down'],
    checking: ['Verificando API', 'status-pill--checking'],
    unknown: ['API não verificada', 'status-pill--checking'],
  }[state.health]
  return `
    <header class="page-heading">
      <div>
        <p class="eyebrow">Hoje, ${new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long' }).format(new Date())}</p>
        <h1>Visão geral</h1>
        <p>Os indicadores serão alimentados pelos pagamentos e atendimentos registrados.</p>
      </div>
      <span class="status-pill ${health[1]}"><i></i> ${health[0]}</span>
    </header>
    <section class="metric-grid" aria-label="Indicadores do clube">
      ${metricCard('Assinantes ativos', '—', 'Aguardando persistência', '↗')}
      ${metricCard('Pagamentos no mês', '—', 'Aguardando persistência', 'R$')}
      ${metricCard('Atendimentos hoje', '—', 'Aguardando persistência', '✦')}
      ${metricCard('Renovações pendentes', '—', 'Aguardando persistência', '!')}
    </section>
    <section class="dashboard-grid">
      <article class="panel panel--feature">
        <div>
          <p class="eyebrow">Rateio real</p>
          <h2>Quem atendeu, quanto recebe</h2>
          <p>Escolha um assinante, registre a visita e acompanhe a divisão 60/40 entre gerência e barbeiros.</p>
        </div>
        <button class="button button--light" data-page="clube">Abrir Gestão do Clube</button>
      </article>
      <article class="panel">
        <div class="panel__heading">
          <div><p class="eyebrow">Próximas ações</p><h2>Implantação do MVP</h2></div>
        </div>
        <ol class="roadmap-list">
          <li class="is-done"><span>1</span><div><strong>Regra de rateio</strong><small>Implementada e testada</small></div></li>
          <li><span>2</span><div><strong>Assinantes e ciclos</strong><small>Persistência Firestore</small></div></li>
          <li><span>3</span><div><strong>Pagamentos e atendimentos</strong><small>Operação diária</small></div></li>
        </ol>
      </article>
    </section>
  `
}

function metricCard(label, value, detail, icon) {
  return `<article class="metric-card"><div class="metric-card__icon">${icon}</div><p>${label}</p><strong>${value}</strong><small>${detail}</small></article>`
}

function clubView() {
  const clube = state.clube
  const assinanteId = clube.assinanteSelecionadoId
  const assinanteSelecionado = assinanteId && assinanteId !== 'todos'
    ? clube.assinantes.find((item) => item.id === assinanteId)
    : null

  return `
    <header class="page-heading">
      <div><p class="eyebrow">Rateio real</p><h1>Gestão do Clube</h1><p>Escolha o assinante, registre quem atendeu, e veja quanto cada barbeiro deve receber.</p></div>
      ${assinanteSelecionado ? `
        <div class="cycle-pill">
          <div>
            <span>Ciclo vigente</span>
            <strong>${formatarDataCurta(assinanteSelecionado.cicloInicio)} – ${formatarDataCurta(cicloFimIso(assinanteSelecionado.cicloInicio))}</strong>
          </div>
        </div>
      ` : ''}
    </header>
    <section class="club-layout">
      <div class="panel share-form">
        <div class="panel__heading">
          <div><p class="eyebrow">Assinante</p><h2>Quem foi atendido</h2></div>
        </div>
        <div class="form-grid">
          <label>Assinante do clube
            <select id="assinante-select">
              <option value="">Selecione um assinante…</option>
              <option value="todos" ${assinanteId === 'todos' ? 'selected' : ''}>Todos os assinantes já atendidos</option>
              ${clube.assinantes.map((item) => `
                <option value="${item.id}" ${assinanteId === item.id ? 'selected' : ''}>${escapeHtml(item.nome)}</option>
              `).join('')}
            </select>
          </label>
        </div>
        <button type="button" class="link-button" data-action="toggle-novo-assinante">
          ${clube.mostrarNovoAssinante ? '– Cancelar novo assinante' : '+ Novo assinante'}
        </button>
        ${clube.mostrarNovoAssinante ? `
          <form id="novo-assinante-form" class="form-grid form-grid--three attendance-row" style="grid-template-columns: 2fr 1fr 1fr 1fr auto; margin-top: 10px;">
            <label>Nome do assinante<input name="nome" required /></label>
            <label>Plano (R$)<input name="valorPlano" inputmode="decimal" value="200.00" required /></label>
            <label>Gerência (%)<input name="percentualGerencia" inputmode="decimal" value="60" required /></label>
            <label>Barbeiros (%)<input name="percentualBarbeiros" inputmode="decimal" value="40" required /></label>
            <button class="button button--primary button--small" type="submit" style="align-self: end;">Cadastrar</button>
          </form>
        ` : ''}

        ${assinanteSelecionado ? `
          <div class="divider"></div>
          <div class="section-title">
            <div><h3>Plano de ${escapeHtml(assinanteSelecionado.nome)}</h3><p>Editável — mudanças valem a partir do próximo cálculo.</p></div>
          </div>
          <form id="editar-assinante-form" class="edit-plan-form" data-assinante-id="${assinanteSelecionado.id}">
            <label>Mensalidade (R$)<input name="valorPlano" inputmode="decimal" value="${assinanteSelecionado.valorPlano}" required /></label>
            <label>Gerência (%)<input name="percentualGerencia" id="editar-pct-gerencia" inputmode="decimal" value="${assinanteSelecionado.percentualGerencia}" required /></label>
            <label>Barbeiros (%)<input id="editar-pct-barbeiros" inputmode="decimal" value="${assinanteSelecionado.percentualBarbeiros}" disabled /></label>
            <label>Início do ciclo<input name="cicloInicio" type="date" value="${paraInputDate(assinanteSelecionado.cicloInicio)}" required /></label>
            <button class="button button--secondary button--small" type="submit">Salvar alterações</button>
          </form>
        ` : ''}

        ${assinanteId && assinanteId !== 'todos' ? `
          <div class="divider"></div>
          <div class="section-title">
            <div><h3>Atendimentos${assinanteSelecionado ? ` de ${escapeHtml(assinanteSelecionado.nome)}` : ''}</h3><p>Só as visitas dentro do ciclo vigente entram no cálculo.</p></div>
          </div>
          <form id="novo-atendimento-form" class="attendance-row" style="grid-template-columns: 1fr auto;">
            <label>Barbeiro que atendeu
              <select name="barbeiroId" required>
                <option value="">Selecione…</option>
                ${clube.barbeiros.map((barbeiro) => `<option value="${barbeiro.id}">${escapeHtml(barbeiro.nome)}</option>`).join('')}
              </select>
            </label>
            <button class="button button--primary button--small" type="submit" style="align-self: end;">Registrar visita</button>
          </form>

          <button type="button" class="link-button" data-action="toggle-novo-barbeiro">
            ${clube.mostrarNovoBarbeiro ? '– Cancelar novo barbeiro' : '+ Barbeiro não está na lista? Cadastrar'}
          </button>
          ${clube.mostrarNovoBarbeiro ? `
            <form id="novo-barbeiro-form" class="attendance-row" style="grid-template-columns: 1fr auto;">
              <label>Nome do barbeiro<input name="nome" required /></label>
              <button class="button button--secondary button--small" type="submit" style="align-self: end;">Cadastrar</button>
            </form>
          ` : ''}
          <div class="attendance-list" style="margin-top: 16px;">
            ${clube.atendimentos.map((item) => {
              const noCiclo = assinanteSelecionado ? estaNoCicloVigente(item.dataHora, assinanteSelecionado.cicloInicio) : true
              return `
              <div class="visit-item ${noCiclo ? '' : 'is-fora-do-ciclo'}">
                <div class="avatar">${escapeHtml(item.barbeiroNome).slice(0, 1)}</div>
                <div><strong>${escapeHtml(item.barbeiroNome)}</strong><small>${new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(item.dataHora))}</small></div>
                <span class="status-chip ${noCiclo ? 'status-chip--in' : 'status-chip--out'}">${noCiclo ? 'no ciclo' : 'fora do ciclo'}</span>
                <button type="button" class="icon-button" data-action="remover-atendimento" data-atendimento-id="${item.id}" aria-label="Remover atendimento" title="Remover atendimento lançado errado">×</button>
              </div>`
            }).join('') || '<p class="empty-state">Nenhuma visita registrada ainda para este assinante.</p>'}
          </div>
        ` : ''}
        ${!assinanteId ? '<p class="empty-state" style="margin-top: 16px;">Selecione um assinante (ou "Todos") para ver e registrar atendimentos.</p>' : ''}
      </div>
      <aside class="panel result-panel">
        <div class="panel__heading"><div><p class="eyebrow">Resultado</p><h2>Quanto pagar aos barbeiros</h2></div></div>
        ${clube.calculando
          ? '<div class="result-placeholder"><span class="spinner"></span><p>Calculando…</p></div>'
          : clube.resultado
            ? (assinanteId === 'todos' ? resultadoGeralView(clube.resultado) : resultadoAssinanteView(clube.resultado))
            : '<div class="result-placeholder"><span>40%</span><p>Selecione um assinante para ver a divisão entre os barbeiros.</p></div>'}
      </aside>
    </section>
  `
}

function resultadoAssinanteView(result) {
  return `
    <div class="result-summary">
      <div><span>Valor do plano</span><strong>${money(result.valorPlano)}</strong></div>
      <div><span>Parcela gerencial</span><strong>${money(result.valorGerencia)}</strong><small>${result.percentualGerencia}%</small></div>
      <div><span>Fundo dos barbeiros</span><strong>${money(result.fundoBarbeiros)}</strong><small>${result.percentualBarbeiros}%</small></div>
    </div>
    ${Number(result.valorNaoAlocado) > 0 ? `<div class="warning-note"><strong>${money(result.valorNaoAlocado)} não alocados</strong><span>Este assinante ainda não tem atendimentos registrados.</span></div>` : ''}
    <div class="allocation-list">
      ${result.parcelas.map((item) => `
        <div class="allocation-item">
          <div class="avatar">${escapeHtml(item.barbeiroNome).slice(0, 1)}</div>
          <div><strong>${escapeHtml(item.barbeiroNome)}</strong><small>${item.quantidadeAtendimentos} atendimento(s) · ${item.percentualDoFundo}% do fundo</small></div>
          <span>${money(item.valor)}</span>
        </div>
      `).join('')}
    </div>
  `
}

function resultadoGeralView(result) {
  return `
    <div class="result-summary">
      <div><span>Assinantes já atendidos</span><strong>${result.totalAssinantesAtendidos}</strong></div>
      <div><span>Total pago em planos</span><strong>${money(result.valorTotalPlanos)}</strong></div>
      <div><span>Total da gerência</span><strong>${money(result.valorTotalGerencia)}</strong></div>
      <div><span>Total dos barbeiros</span><strong>${money(result.valorTotalBarbeiros)}</strong></div>
    </div>
    <div class="allocation-list">
      ${result.parcelas.map((item) => `
        <div class="allocation-item">
          <div class="avatar">${escapeHtml(item.barbeiroNome).slice(0, 1)}</div>
          <div><strong>${escapeHtml(item.barbeiroNome)}</strong><small>${item.quantidadeAtendimentos} atendimento(s) no total</small></div>
          <span>${money(item.valor)}</span>
        </div>
      `).join('') || '<p class="empty-state">Nenhum assinante atendido ainda.</p>'}
    </div>
  `
}

function plannedView(page) {
  const item = menu.find(([id]) => id === page)
  return `
    <header class="page-heading"><div><p class="eyebrow">Módulo planejado</p><h1>${escapeHtml(item?.[1] || page)}</h1><p>Este módulo faz parte do backlog aprovado para as próximas entregas.</p></div></header>
    <section class="panel planned-panel">
      <span>${item?.[2] || '•'}</span>
      <h2>Estrutura preparada</h2>
      <p>Autorização, modelo de dados e critérios de aceite deste módulo estão documentados. A implementação seguirá após o fluxo de assinantes, ciclos e pagamentos.</p>
    </section>
  `
}

function appView() {
  const displayName = state.user?.displayName || state.devSession?.name || 'Equipe 3 Barbas'
  const role = activeRole()
  const allowedMenu = visibleMenu()
  if (!allowedMenu.some(([id]) => id === state.page)) {
    state.page = 'dashboard'
  }
  const currentView = state.page === 'dashboard'
    ? dashboardView()
    : state.page === 'clube'
      ? clubView()
      : plannedView(state.page)

  return `
    <div class="app-shell">
      <aside class="sidebar">
        <div class="brand brand--light brand--sidebar"><img class="brand__logo" src="/assets/3barbas_logo.png" alt="3 Barbas — Visagismo e Barbearia" /></div>
        <nav aria-label="Navegação principal">
          ${allowedMenu.map(([id, label, icon]) => `<button class="nav-item ${state.page === id ? 'is-active' : ''}" data-page="${id}"><span>${icon}</span>${label}</button>`).join('')}
        </nav>
        <div class="sidebar__footer"><small>Clube 3 Barbas</small><span>Versão inicial · 0.1.0</span></div>
      </aside>
      <div class="workspace">
        <header class="topbar">
          <button class="menu-button" data-action="toggle-menu" aria-label="Abrir menu">☰</button>
          <div class="topbar__spacer"></div>
          <div class="user-menu"><div class="avatar">${escapeHtml(displayName).slice(0, 1)}</div><div><strong>${escapeHtml(displayName)}</strong><small>${escapeHtml(role)}</small></div><button class="icon-button" data-action="logout" title="Sair">↪</button></div>
        </header>
        <main class="content">${currentView}</main>
      </div>
    </div>
  `
}

function render() {
  const hasAppAccess = Boolean(state.devSession || state.authContext?.ready)
  const mainView = hasAppAccess ? appView() : state.user ? onboardingView() : loginView()
  root.innerHTML = `${mainView}${state.toast ? `<div class="toast toast--${state.toast.type}">${escapeHtml(state.toast.message)}</div>` : ''}`
  bindEvents()
}

function bindEvents() {
  document.querySelectorAll('[data-page]').forEach((button) => {
    button.addEventListener('click', () => {
      state.page = button.dataset.page
      document.querySelector('.sidebar')?.classList.remove('is-open')
      render()
      if (state.page === 'clube' && !state.clube.carregado) {
        carregarDadosDoClube()
      }
    })
  })

  document.querySelector('#login-form')?.addEventListener('submit', handleLogin)
  document.querySelector('#novo-assinante-form')?.addEventListener('submit', handleNovoAssinante)
  document.querySelector('#novo-barbeiro-form')?.addEventListener('submit', handleNovoBarbeiro)
  document.querySelector('#novo-atendimento-form')?.addEventListener('submit', handleNovoAtendimento)
  document.querySelector('#editar-assinante-form')?.addEventListener('submit', handleEditarAssinante)

  document.getElementById('editar-pct-gerencia')?.addEventListener('input', (event) => {
    const gerencia = Math.max(0, Math.min(100, Number(event.target.value) || 0))
    document.getElementById('editar-pct-barbeiros').value = (100 - gerencia).toFixed(0)
  })

  document.querySelector('#assinante-select')?.addEventListener('change', (event) => {
    state.clube.assinanteSelecionadoId = event.target.value
    state.clube.mostrarNovoBarbeiro = false
    render()
    carregarAtendimentosEDivisao()
  })

  document.querySelectorAll('[data-action]').forEach((button) => {
    const action = button.dataset.action
    if (action === 'dev-login') button.addEventListener('click', () => {
      state.devSession = { uid: 'gerente-local', name: 'Gerente local', role: 'GERENTE' }
      render()
      refreshHealth()
    })
    if (action === 'logout') button.addEventListener('click', async () => {
      state.devSession = null
      state.authContext = null
      state.accessGate = null
      state.health = 'unknown'
      await logout()
      state.user = null
      render()
    })
    if (action === 'resend-verification') button.addEventListener('click', handleResendVerification)
    if (action === 'refresh-access') button.addEventListener('click', () => evaluateUserAccess(state.user, true))
    if (action === 'forgot-password') button.addEventListener('click', handlePasswordReset)
    if (action === 'toggle-password') button.addEventListener('click', () => {
      const input = document.querySelector('input[name="password"]')
      input.type = input.type === 'password' ? 'text' : 'password'
      button.textContent = input.type === 'password' ? 'Ver' : 'Ocultar'
    })
    if (action === 'toggle-novo-assinante') button.addEventListener('click', () => {
      state.clube.mostrarNovoAssinante = !state.clube.mostrarNovoAssinante
      render()
    })
    if (action === 'toggle-novo-barbeiro') button.addEventListener('click', () => {
      state.clube.mostrarNovoBarbeiro = !state.clube.mostrarNovoBarbeiro
      render()
    })
    if (action === 'remover-atendimento') button.addEventListener('click', () => {
      handleRemoverAtendimento(button.dataset.atendimentoId)
    })
    if (action === 'toggle-menu') button.addEventListener('click', () => {
      document.querySelector('.sidebar')?.classList.toggle('is-open')
    })
  })
}

async function handleLogin(event) {
  event.preventDefault()
  const form = new FormData(event.currentTarget)
  state.loading = true
  render()
  try {
    await loginWithEmail(form.get('email'), form.get('password'))
  } catch (error) {
    state.loading = false
    setToast(authErrorMessage(error), 'error')
  }
}

async function handleResendVerification() {
  try {
    await resendVerificationEmail(state.user)
    setToast('Link de verificação enviado. Confira também a caixa de spam.')
  } catch (error) {
    setToast(authErrorMessage(error), 'error')
  }
}

async function evaluateUserAccess(user, forceRefresh = false) {
  if (!user) return
  state.accessGate = { type: 'checking' }
  state.authContext = null
  render()

  try {
    const access = await inspectUserAccess(user, forceRefresh)
    const role = normalizeRole(access.role)
    if (!role) {
      state.accessGate = { type: 'missing-role' }
    } else if (!access.emailVerified) {
      state.accessGate = { type: 'verify-email' }
    } else {
      state.accessGate = null
      state.authContext = { ready: true, role }
      if (!visibleMenu().some(([id]) => id === state.page)) {
        state.page = 'dashboard'
      }
    }
  } catch (error) {
    state.accessGate = { type: 'error', message: authErrorMessage(error) }
  }

  render()
  if (state.authContext?.ready) {
    refreshHealth()
  }
}

async function refreshHealth() {
  if (state.health === 'checking') return
  state.health = 'checking'
  render()
  try {
    await apiFetch('/api/v1/health', {}, state.devSession)
    state.health = 'up'
  } catch {
    state.health = 'down'
  }
  render()
}

function authErrorMessage(error) {
  const messages = {
    'auth/invalid-credential': 'E-mail ou senha inválidos.',
    'auth/user-not-found': 'E-mail ou senha inválidos.',
    'auth/wrong-password': 'E-mail ou senha inválidos.',
    'auth/invalid-email': 'Informe um e-mail válido.',
    'auth/too-many-requests': 'Muitas tentativas. Aguarde alguns minutos e tente novamente.',
    'auth/requires-recent-login': 'Entre novamente para concluir esta operação.',
  }
  return messages[error?.code] || error?.message || 'Não foi possível concluir a autenticação.'
}

async function handlePasswordReset() {
  const email = document.querySelector('input[name="email"]')?.value
  if (!email) {
    setToast('Informe seu e-mail antes de solicitar a recuperação.', 'error')
    return
  }
  try {
    await requestPasswordReset(email)
    setToast('Se a conta existir, o link de recuperação foi enviado.')
  } catch (error) {
    if (error?.code === 'auth/user-not-found') {
      setToast('Se a conta existir, o link de recuperação foi enviado.')
    } else {
      setToast(authErrorMessage(error), 'error')
    }
  }
}

async function carregarDadosDoClube() {
  try {
    const [assinantes, barbeiros] = await Promise.all([
      apiFetch('/api/v1/assinantes', {}, state.devSession),
      apiFetch('/api/v1/barbeiros', {}, state.devSession),
    ])
    state.clube.assinantes = assinantes
    state.clube.barbeiros = barbeiros
    state.clube.carregado = true
    render()
  } catch (error) {
    setToast(error.message, 'error')
  }
}

async function carregarAtendimentosEDivisao() {
  const clube = state.clube
  const assinanteId = clube.assinanteSelecionadoId
  if (!assinanteId) {
    clube.atendimentos = []
    clube.resultado = null
    render()
    return
  }

  clube.calculando = true
  render()
  try {
    if (assinanteId === 'todos') {
      clube.atendimentos = []
      clube.resultado = await apiFetch('/api/v1/rateios/geral', {}, state.devSession)
    } else {
      const [atendimentos, resultado] = await Promise.all([
        apiFetch(`/api/v1/assinantes/${assinanteId}/atendimentos`, {}, state.devSession),
        apiFetch(`/api/v1/rateios/assinantes/${assinanteId}`, {}, state.devSession),
      ])
      clube.atendimentos = atendimentos
      clube.resultado = resultado
    }
  } catch (error) {
    setToast(error.message, 'error')
  } finally {
    clube.calculando = false
    render()
  }
}

async function handleNovoAssinante(event) {
  event.preventDefault()
  const form = new FormData(event.currentTarget)
  try {
    const assinante = await apiFetch('/api/v1/assinantes', {
      method: 'POST',
      body: JSON.stringify({
        nome: form.get('nome'),
        valorPlano: form.get('valorPlano'),
        percentualGerencia: form.get('percentualGerencia'),
        percentualBarbeiros: form.get('percentualBarbeiros'),
      }),
    }, state.devSession)
    state.clube.assinantes.push(assinante)
    state.clube.mostrarNovoAssinante = false
    state.clube.assinanteSelecionadoId = assinante.id
    render()
    setToast(`${assinante.nome} cadastrado.`)
    carregarAtendimentosEDivisao()
  } catch (error) {
    setToast(error.message, 'error')
  }
}

async function handleEditarAssinante(event) {
  event.preventDefault()
  const form = new FormData(event.currentTarget)
  const assinanteId = event.currentTarget.dataset.assinanteId
  const atual = state.clube.assinantes.find((item) => item.id === assinanteId)
  const percentualGerencia = form.get('percentualGerencia')
  try {
    const atualizado = await apiFetch(`/api/v1/assinantes/${assinanteId}`, {
      method: 'PUT',
      body: JSON.stringify({
        nome: atual.nome,
        valorPlano: form.get('valorPlano'),
        percentualGerencia,
        percentualBarbeiros: 100 - Number(percentualGerencia),
        cicloInicio: deInputDate(form.get('cicloInicio')),
      }),
    }, state.devSession)
    const indice = state.clube.assinantes.findIndex((item) => item.id === assinanteId)
    state.clube.assinantes[indice] = atualizado
    setToast('Assinatura atualizada.')
    carregarAtendimentosEDivisao()
  } catch (error) {
    setToast(error.message, 'error')
  }
}

async function handleNovoBarbeiro(event) {
  event.preventDefault()
  const form = new FormData(event.currentTarget)
  try {
    const barbeiro = await apiFetch('/api/v1/barbeiros', {
      method: 'POST',
      body: JSON.stringify({ nome: form.get('nome') }),
    }, state.devSession)
    state.clube.barbeiros.push(barbeiro)
    state.clube.mostrarNovoBarbeiro = false
    render()
    setToast(`${barbeiro.nome} cadastrado na equipe.`)
  } catch (error) {
    setToast(error.message, 'error')
  }
}

async function handleNovoAtendimento(event) {
  event.preventDefault()
  const form = new FormData(event.currentTarget)
  const assinanteId = state.clube.assinanteSelecionadoId
  try {
    await apiFetch(`/api/v1/assinantes/${assinanteId}/atendimentos`, {
      method: 'POST',
      body: JSON.stringify({ barbeiroId: form.get('barbeiroId') }),
    }, state.devSession)
    setToast('Atendimento registrado.')
    carregarAtendimentosEDivisao()
  } catch (error) {
    setToast(error.message, 'error')
  }
}

async function handleRemoverAtendimento(atendimentoId) {
  const assinanteId = state.clube.assinanteSelecionadoId
  if (!assinanteId || assinanteId === 'todos') return
  try {
    await apiFetch(`/api/v1/assinantes/${assinanteId}/atendimentos/${atendimentoId}`, {
      method: 'DELETE',
    }, state.devSession)
    setToast('Atendimento removido.')
    carregarAtendimentosEDivisao()
  } catch (error) {
    setToast(error.message, 'error')
  }
}

observeAuth((user) => {
  state.user = user
  state.loading = false
  state.authContext = null
  if (user) {
    evaluateUserAccess(user)
  } else {
    state.accessGate = null
    render()
  }
})
