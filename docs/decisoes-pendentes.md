# Decisões pendentes

As escolhas abaixo alteram regras financeiras ou permissões e precisam de validação do responsável pelo Clube antes da respectiva entrega.

1. **Relatórios:** todos veem o painel resumido e apenas Gerente abre relatórios detalhados/exporta? Esta é a premissa adotada.
2. **Parcela de 60%:** pertence à empresa (`HOUSE`) ou deve ser dividida entre usuários Gerente? A base usa `HOUSE`.
3. **Percentuais:** 60/40 são fixos ou valores padrão editáveis por Gerente? A modelagem permite versioná-los.
4. **Ciclo sem atendimento:** os 40% ficam não alocados, vão para a empresa ou seguem outra regra? A base mantém não alocado.
5. **Ciclo:** são 30 × 24 horas ou 30 datas civis? A base usa 30 × 24 horas e intervalo `[início, fim)`.
6. **Renovação antecipada:** começa após a vigência atual ou imediatamente? A premissa é começar após a atual.
7. **Cancelamento durante o ciclo:** mantém o ciclo pago, faz pró-rata ou estorna? Ainda sem premissa financeira.
8. **Exclusão de assinante:** todos os perfis podem efetuar, como descrito, ou somente Gerente? A base preserva a permissão descrita, sempre com motivo e auditoria.
9. **“Card”:** significa código/carteirinha do Clube? A modelagem adotou `clubCardCode`.
10. **Filtro por barbeiro:** deve buscar barbeiro preferido, barbeiro que atendeu ou oferecer os dois? A modelagem separa os dois.
11. **Pagamento recorrente:** é apenas controle manual da renovação ou cobrança automática? Cobrança automática exige gateway externo.
12. **Relatório diário:** canal, horário, destinatários e formato ainda precisam ser definidos.
13. **Referência de corte:** passa a ser um campo de texto (`preferredHaircutReference`), sem upload de imagem nem Firebase Storage; o formato aceito (URL externa e/ou descrição livre) e o limite de tamanho do texto ainda precisam ser definidos.
14. **Meio centavo entre parcelas:** hoje a gerência usa `HALF_UP` e o fundo recebe o restante. É preciso ratificar essa política financeira antes do primeiro fechamento real.
