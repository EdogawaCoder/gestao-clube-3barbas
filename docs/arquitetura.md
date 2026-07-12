# Arquitetura e segurança

## Componentes

```text
Firebase Hosting
├─ SPA em HTML/CSS/JavaScript
└─ /api/** → Cloud Run: clube-3-barbas-api
                ├─ Spring Security + Firebase ID Token
                ├─ casos de uso e regras financeiras
                └─ Firestore

Cloud Scheduler
└─ Cloud Run Job: fechamento/status/resumo diário

Cloud Tasks
└─ worker privado: PDF/XLSX e tarefas longas
```

O Hosting tem limite de 60 segundos para conteúdo dinâmico. Relatórios grandes serão assíncronos: a API cria uma tarefa, o worker gera o arquivo e o transmite ao Gerente sob demanda por um endpoint autenticado, sem depender do Firebase Storage.

## Autenticação

- Firebase Authentication with Identity Platform.
- E-mail/senha como primeiro fator e SMS MFA obrigatório.
- E-mail verificado antes de cadastrar MFA.
- Telefones em E.164.
- `role` em custom claim: `GERENTE`, `ADMINISTRATIVO` ou `BARBEIRO`.
- A API também exige e-mail verificado e `firebase.sign_in_second_factor` no ID Token.
- Perfil completo e situação da conta em `users/{uid}`.
- Ao desativar usuário: desabilitar conta, revogar refresh tokens e preservar histórico.
- Firebase Auth (`disabled` + revogação) é a fonte autoritativa de bloqueio; `users/{uid}` é a projeção operacional reconciliada.

O reset padrão envia um link por e-mail. Depois da troca, o login continua exigindo o segundo fator por SMS; assim o acesso à conta depende das duas validações. O procedimento administrativo para perda de telefone precisa ser manual e auditado.

## Autorização

O navegador fala com o Firebase apenas para autenticação. Ele envia o ID Token como `Authorization: Bearer ...` à API. Spring Security verifica assinatura, revogação e perfil antes do caso de uso.

O Firebase Admin SDK ignora Firestore Security Rules. Por isso:

- Firestore nega acesso direto ao navegador.
- Toda mutação é validada novamente na API.
- Endpoints usam autorização por perfil.
- Alteração de perfil nunca aceita o perfil enviado pelo próprio usuário.
- Logs são append-only.
- Dados sensíveis não são copiados para logs.
- A autenticação por headers de desenvolvimento só inicia com profile `local`, flag explícita e fora do Cloud Run.

## Persistência e consistência

- Dinheiro é `long` em centavos no Firestore e `BigDecimal` na borda Java.
- Percentuais são pontos-base: 60% = `6000`.
- Datas persistidas em UTC; operação e relatórios usam `America/Sao_Paulo`.
- Ciclos usam intervalo semiaberto `[startsAt, endsAt)`.
- Pagamento, ciclo, assinatura, assinante, auditoria e outbox são atualizados em transação idempotente.
- IDs determinísticos ou chaves de idempotência evitam pagamento/ciclo duplicado.
- Mudanças de plano criam versão; histórico nunca é reprocessado.
- Estornos posteriores a fechamento criam ajuste, sem reescrever rateio pago.
- O simulador aceita valores manuais; o fechamento real deve carregar valor pago, snapshot e atendimentos pelo backend dentro do caso de uso transacional.

## Arquivos e dados de pagamento

- A referência do corte de preferência é apenas texto (`preferredHaircutReference`): uma URL externa ou descrição, sem upload de imagem para o Firebase Storage.
- Não armazenar senha, OTP, PAN, CVV ou foto de cartão.
- A aplicação guarda somente método, bandeira e referência externa segura.
- Cobrança automática exigirá gateway externo e webhooks idempotentes.

## Operação

- Região inicial: `southamerica-east1`.
- Application Default Credentials no Cloud Run.
- Segredos em Secret Manager.
- Logs estruturados com correlation ID.
- Budget e alertas de custo obrigatórios antes da produção.
- App Check pode ser acrescentado como defesa adicional, sem substituir autenticação.
- Requisitos de retenção, acesso e eliminação precisam de revisão LGPD antes da entrada em produção.
