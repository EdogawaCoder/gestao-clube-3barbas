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
  simulation: null,
  loading: false,
  toast: null,
  health: 'unknown',
  attendances: [
    { barberId: 'barbeiro-1', barberName: 'Barbeiro 1' },
    { barberId: 'barbeiro-2', barberName: 'Barbeiro 2' },
    { barberId: 'barbeiro-1', barberName: 'Barbeiro 1' },
  ],
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
          <p class="eyebrow">Primeira entrega funcional</p>
          <h2>Rateio proporcional por atendimento</h2>
          <p>Valide os exemplos de 60/40 e acompanhe a distribuição exata dos centavos.</p>
        </div>
        <button class="button button--light" data-page="clube">Abrir simulador</button>
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
  const result = state.simulation
  return `
    <header class="page-heading">
      <div><p class="eyebrow">Configuração financeira</p><h1>Gestão do Clube</h1><p>Visualize a regra e simule a divisão de uma assinatura.</p></div>
      <span class="role-badge">Simulação disponível para a equipe</span>
    </header>
    <section class="club-layout">
      <form id="share-form" class="panel share-form">
        <div class="panel__heading">
          <div><p class="eyebrow">Plano de exemplo</p><h2>Simulador de rateio</h2></div>
          <span class="tag">Ciclo de 30 dias</span>
        </div>
        <div class="form-grid form-grid--three">
          <label>Mensalidade (R$)<input name="planValue" inputmode="decimal" value="200.00" required /></label>
          <label>Gerência (%)<input name="managerPercentage" inputmode="decimal" value="60" required /></label>
          <label>Barbeiros (%)<input name="barberPercentage" inputmode="decimal" value="40" required /></label>
        </div>
        <div class="section-title"><div><h3>Atendimentos no ciclo</h3><p>Cada linha representa uma visita válida.</p></div><button type="button" class="button button--secondary button--small" data-action="add-attendance">+ Atendimento</button></div>
        <div class="attendance-list">
          ${state.attendances.map((item, index) => `
            <div class="attendance-row">
              <span>${String(index + 1).padStart(2, '0')}</span>
              <label>ID<input data-field="barberId" data-index="${index}" value="${escapeHtml(item.barberId)}" required /></label>
              <label>Barbeiro<input data-field="barberName" data-index="${index}" value="${escapeHtml(item.barberName)}" required /></label>
              <button type="button" class="icon-button" data-action="remove-attendance" data-index="${index}" aria-label="Remover atendimento">×</button>
            </div>
          `).join('') || '<p class="empty-state">Nenhum atendimento: o fundo ficará não alocado.</p>'}
        </div>
        <button class="button button--primary" type="submit" ${state.loading ? 'disabled' : ''}>${state.loading ? 'Calculando…' : 'Calcular distribuição'}</button>
      </form>
      <aside class="panel result-panel">
        <div class="panel__heading"><div><p class="eyebrow">Resultado</p><h2>Fechamento previsto</h2></div></div>
        ${result ? resultView(result) : '<div class="result-placeholder"><span>40%</span><p>Preencha os atendimentos e calcule para visualizar o rateio.</p></div>'}
      </aside>
    </section>
  `
}

function resultView(result) {
  return `
    <div class="result-summary">
      <div><span>Valor do plano</span><strong>${money(result.valorPlano)}</strong></div>
      <div><span>Parcela gerencial</span><strong>${money(result.valorGerencia)}</strong><small>${result.percentualGerencia}%</small></div>
      <div><span>Fundo dos barbeiros</span><strong>${money(result.fundoBarbeiros)}</strong><small>${result.percentualBarbeiros}%</small></div>
    </div>
    ${Number(result.valorNaoAlocado) > 0 ? `<div class="warning-note"><strong>${money(result.valorNaoAlocado)} não alocados</strong><span>O ciclo ainda não possui atendimentos válidos.</span></div>` : ''}
    <div class="allocation-list">
      ${result.parcelas.map((item) => `
        <div class="allocation-item">
          <div class="avatar">${escapeHtml(item.barbeiroNome).slice(0, 1)}</div>
          <div><strong>${escapeHtml(item.barbeiroNome)}</strong><small>${item.quantidadeAtendimentos} atendimento(s) · ${item.percentualDoFundo}% do fundo</small></div>
          <span>${money(item.valor)}</span>
        </div>
      `).join('')}
    </div>
    <div class="balance-check"><span>Conferência do total</span><strong>✓ Sem diferença de centavos</strong></div>
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
    })
  })

  document.querySelector('#login-form')?.addEventListener('submit', handleLogin)
  document.querySelector('#share-form')?.addEventListener('submit', handleShareSimulation)

  document.querySelectorAll('[data-field]').forEach((input) => {
    input.addEventListener('input', () => {
      const index = Number(input.dataset.index)
      const key = input.dataset.field === 'barberId' ? 'barberId' : 'barberName'
      state.attendances[index][key] = input.value
    })
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
    if (action === 'add-attendance') button.addEventListener('click', () => {
      state.attendances.push({ barberId: '', barberName: '' })
      render()
    })
    if (action === 'remove-attendance') button.addEventListener('click', () => {
      state.attendances.splice(Number(button.dataset.index), 1)
      state.simulation = null
      render()
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

async function handleShareSimulation(event) {
  event.preventDefault()
  const form = new FormData(event.currentTarget)
  state.loading = true
  render()
  try {
    state.simulation = await apiFetch('/api/v1/rateios/simular', {
      method: 'POST',
      body: JSON.stringify({
        valorPlano: form.get('planValue'),
        percentualGerencia: form.get('managerPercentage'),
        percentualBarbeiros: form.get('barberPercentage'),
        atendimentos: state.attendances.map(({ barberId, barberName }) => ({
          barbeiroId: barberId,
          barbeiroNome: barberName,
        })),
      }),
    }, state.devSession)
    state.loading = false
    render()
  } catch (error) {
    state.loading = false
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
