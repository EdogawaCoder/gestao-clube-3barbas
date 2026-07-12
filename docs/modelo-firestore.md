# Modelo Firestore

## Convenções

- Código e nomes dos campos em inglês; textos da interface em português.
- Valores monetários em centavos (`long`).
- Percentuais em pontos-base (`long`).
- Datas em `Timestamp` UTC.
- Dados derivados no documento principal são cache e devem ser reconciliados.
- Pagamentos, ciclos, atendimentos e logs não são arrays dentro do assinante.
- A referência de corte de preferência é o campo `String preferredHaircutReference` no próprio assinante (URL externa ou descrição textual); nenhuma imagem é enviada ao Firebase Storage.

## Coleções

| Coleção | Responsabilidade |
|---|---|
| `users/{uid}` | Perfil da equipe, contato, função, estado e autoria. O ID é o UID do Auth. |
| `staffDirectory/{uid}` | Projeção mínima de nome/função/ativo para seletores. |
| `plans/{planId}` | Configuração atual do plano e versão vigente. |
| `planVersions/{id}` | Snapshot imutável de nome, serviços, preço, duração e percentuais. |
| `subscribers/{subscriberId}` | Dados cadastrais, preferências (inclui `preferredHaircutReference`), plano atual, status e autoria. |
| `subscriptions/{subscriptionId}` | Vínculo histórico entre assinante e plano. |
| `cycles/{cycleId}` | Ciclo de 30 dias, pagamento e snapshot financeiro. |
| `payments/{paymentId}` | Valor, método, bandeira, estado, data, ciclo e idempotência. |
| `paymentEvents/{eventId}` | Eventos imutáveis do pagamento. |
| `attendances/{attendanceId}` | Atendimento válido/anulado ligado ao ciclo e barbeiro. |
| `cycleBarberStats/{cycle_barber}` | Projeção server-only de atendimentos válidos. |
| `shareAllocations/{cycle_recipient}` | Rateio HOUSE/BARBER, fração, valor, versão e estado. |
| `auditLogs/{logId}` | Evento imutável com ator, ação, alvo, motivo e resumo redigido. |
| `reportRuns/{reportId}` | Filtros, formato, estado e expiração; o arquivo é transmitido diretamente pela API sob demanda. |
| `dailyMetrics/{yyyy-MM-dd}` | Métricas prontas para dashboard e resumo diário. |
| `monthlyMetrics/{yyyy-MM}` | Acumulados mensais reconciliáveis. |
| `outbox/{eventId}` | Trabalho externo idempotente: e-mail e integrações. |
| `uniqueKeys/{namespace_hash}` | Reserva transacional de código/cartão e outras unicidades. |

## Status

Separar:

- `lifecycleStatus`: `ACTIVE`, `CANCELED`, `DELETED`.
- `paymentStatusCache`: `CURRENT`, `PENDING`.

O status exibido segue a prioridade:

1. `DELETED` → EXCLUÍDO.
2. `CANCELED` → CANCELADO.
3. Ativo e `now < paidThroughAt` → ATIVO.
4. Ativo e prazo encerrado → PENDENTE.

A aplicação sempre recalcula usando `paidThroughAt`; um job apenas mantém o cache consultável.

## Transações críticas

### Primeiro pagamento

Criar pagamento confirmado, assinatura e primeiro ciclo; atualizar assinante; gravar auditoria e outbox na mesma transação.

### Renovação

Usar chave de idempotência, criar ciclo com ID determinístico e atualizar `paidThroughAt`. Ciclo antecipado inicia no fim do atual; ciclo atrasado inicia no pagamento.

### Atendimento

Validar estado e janela do ciclo; criar atendimento; atualizar `cycleBarberStats`. Anulação preserva o documento e desfaz a projeção.

### Rateio

Reconciliar atendimentos, adquirir versão/lock do ciclo, calcular maior resto, gravar alocações idempotentes e fechar o ciclo. Ajustes posteriores são novos lançamentos.

### Desativação de usuário

Usar outbox: solicitar exportação de logs, gerar arquivo, desabilitar Auth, revogar tokens e marcar `DISABLED`. Chamadas ao Auth não participam de uma transação Firestore.

## Busca

Firestore não oferece busca arbitrária “contém” nem joins:

- ID: leitura direta.
- Código/cartão: igualdade normalizada e `uniqueKeys`.
- Nome: campo normalizado e busca por prefixo.
- Mês: intervalo em `payments`, seguido de hidratação dos assinantes pela API.
- Barbeiro: `preferredBarberId` para preferência e `attendances.barberId` para atendimento; são filtros diferentes.
- Busca fuzzy/livre, se necessária, exigirá projeção de busca ou serviço externo.

