# Backlog do MVP

## Entrega 0 — fundação (concluída)

- [x] Arquitetura Hosting + Cloud Run.
- [x] Projeto Spring Boot Java 21.
- [x] Autenticação por ID Token e RBAC base.
- [x] Portal responsivo com login e navegação.
- [x] Primeiro acesso com confirmação de e-mail.
- [x] Provisionamento administrativo inicial e menus filtrados por perfil.
- [x] Regra de rateio e testes automatizados.
- [x] Política de status do assinante.
- [x] Regras fechadas de Firestore e índices iniciais.
- [x] Documentação de requisitos e dados.

## Entrega 1 — assinantes e adesão (P0)

- [ ] Repositório Firestore e transações.
- [ ] CRUD de planos versionados.
- [ ] CRUD de assinantes com telefone E.164 e nacionalidade ISO.
- [ ] Busca estruturada e paginação.
- [ ] Exclusão/cancelamento lógico com motivo.
- [ ] Primeiro pagamento criando assinatura e ciclo.
- [ ] Auditoria de todas as mutações.
- [ ] Formulários, grids, modais e testes de integração no emulador.

## Entrega 2 — operação do ciclo (P0)

- [ ] Renovação idempotente.
- [ ] Registro/anulação de atendimento no ciclo correto.
- [ ] Histórico de pagamentos e atendimentos.
- [ ] Status ATIVO/PENDENTE por vigência.
- [ ] Fechamento do rateio e lançamentos de ajuste.
- [ ] Dashboard com métricas reais.

## Entrega 3 — gestão de equipe e acesso (P0/P1)

- [x] Provisionamento seguro do primeiro Gerente por comando administrativo.
- [x] Custom claim de função no Auth.
- [x] Verificação obrigatória do e-mail durante o primeiro acesso.
- [ ] Convite de equipe pela interface do Gerente.
- [ ] Sincronização do perfil complementar em `users/{uid}`.
- [ ] Gestão de equipe e revogação de sessões.
- [ ] Testes de permissão por endpoint e perfil.

## Entrega 4 — arquivos e relatórios (P1/P2)

- [ ] Campo de texto para a referência de corte de preferência (URL ou descrição).
- [ ] Painel gerencial de filtros.
- [ ] Worker assíncrono para PDF/XLSX.
- [ ] Entrega do arquivo por endpoint autenticado sob demanda.
- [ ] Resumo diário e acumulado mensal.
- [ ] Exportação de logs antes de desativar usuário.

## Qualidade transversal

- [ ] Emulator Suite no CI.
- [ ] Testes de concorrência/idempotência financeira.
- [ ] Logs estruturados, métricas e alertas.
- [ ] Readiness com dependências e smoke test pós-deploy.
- [ ] Backup e restauração testados.
- [ ] Política LGPD, retenção e termos operacionais.
- [ ] Budget e alertas de custos.
