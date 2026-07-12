# Requisitos funcionais consolidados

## Perfis e permissões

| Funcionalidade | Administrativo | Barbeiro | Gerente |
|---|---:|---:|---:|
| Login, recuperação e dashboard | Sim | Sim | Sim |
| Visualizar planos, valores e percentuais | Sim | Sim | Sim |
| Alterar planos, valores e percentuais | Não | Não | Sim |
| Cadastrar e editar assinantes | Sim | Sim | Sim |
| Cancelar/excluir assinante com motivo | Sim | Sim | Sim |
| Registrar pagamentos e atendimentos | Sim | Sim | Sim |
| Indicadores resumidos | Sim | Sim | Sim |
| Relatórios detalhados e exportações | Não | Não | Sim |
| Logs de auditoria | Não | Não | Sim |
| Gestão de equipe e perfis | Não | Não | Sim |

O **assinante** é um cliente do clube e não uma conta autenticada do portal.

## Entidades

- Usuário da equipe.
- Plano e suas versões.
- Assinante.
- Assinatura.
- Ciclo de vigência de 30 dias.
- Pagamento/renovação.
- Atendimento.
- Rateio do ciclo.
- Cancelamento ou exclusão lógica.
- Referência textual do corte de preferência.
- Evento de auditoria.
- Execução de relatório e métricas do dashboard.

## Fluxos essenciais

### Acesso

1. Usuário convidado confirma e-mail e define sua senha.
2. E-mail/senha formam o método de autenticação.
3. O token enviado à API contém o perfil em custom claim.
4. Recuperação envia link ao e-mail para definição de uma nova senha.

O perfil nunca é escolhido num cadastro público. Apenas Gerente convida usuários e atribui perfil.

### Adesão e renovação

1. A equipe cadastra os dados do assinante e seleciona o plano.
2. A assinatura começa apenas depois do primeiro pagamento confirmado.
3. O pagamento cria um ciclo de 30 dias e o assinante fica ATIVO.
4. Sem renovação, no instante final da vigência ele passa a PENDENTE.
5. Novo pagamento cria outro ciclo e reativa um assinante pendente.
6. Preço e percentuais ficam congelados no ciclo; mudanças só afetam ciclos futuros.

### Atendimento e rateio

Cada atendimento guarda assinante, ciclo, barbeiro, data/hora, autor e situação. Para um ciclo:

```text
gerência = valor pago × percentual gerencial
fundo dos barbeiros = valor pago - gerência
barbeiro = fundo × atendimentos do barbeiro / atendimentos válidos do ciclo
```

Os centavos residuais usam o método do maior resto, com desempate por ID do barbeiro. A soma sempre é igual ao fundo. Sem atendimento, o fundo fica **não alocado** até existir uma decisão gerencial.

### Situações do assinante

```text
Primeiro pagamento → ATIVO
Fim da vigência sem renovação → PENDENTE
PENDENTE + pagamento → ATIVO
Pedido voluntário → CANCELADO
Exclusão administrativa → EXCLUÍDO
```

CANCELADO e EXCLUÍDO continuam pesquisáveis. Nenhum histórico é apagado fisicamente.

### Busca e manutenção

- Filtrar por ID, código/cartão do clube, mês de pagamento e barbeiro.
- Combinar filtros e ordenar por cadastro mais recente.
- Exigir motivo e confirmação para cancelar/excluir.
- Exibir último pagamento, ciclos, pagamentos e atendimentos.
- Aceitar telefone internacional no formato E.164.
- Exigir bandeira somente quando a forma de pagamento for cartão.
- Guardar a referência do corte de preferência como texto (URL externa ou descrição) no próprio assinante, sem usar o Firebase Storage.

### Relatórios e logs

- Indicadores resumidos no dashboard para a equipe.
- Filtros e exportações PDF/XLSX apenas para Gerente.
- Resumo diário com novas adesões e pagadores do mês.
- Auditoria append-only com ator, ação, alvo, data, motivo e resumo das mudanças.
- Desativação de usuário preserva seus logs e revoga suas sessões.

