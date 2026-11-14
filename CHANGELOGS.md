# Changelogs

## Unreleased

### Adicionado

- Anúncio de resgate nativo reutiliza `Crate.BroadCast` e `Crate.OpeningBroadCast`: textos com `%reward%` ou `%prize%` são enviados após a entrega, com placeholders do vencedor, sem comandos por prêmio. Textos de abertura mantêm o comportamento anterior; devoluções por recompensa alternativa não anunciam o prêmio original.

- Limite configurável de prêmios simultâneos em `Crate.SelectCrate.MaxSelections`, com padrão 5, contador e revalidação na confirmação.

- SelectCrate aceita `RequiredKeys` por prêmio, com prioridade sobre o custo global e cobrança da soma das escolhas. A interface mostra os custos individuais e o orçamento total.

- SelectCrate bloqueia prêmios conforme `BlackListed-Permissions`, com ícone configurável em `SelectCrate.BlockedPrize` (`STRUCTURE_VOID` por padrão) e revalidação antes da entrega.

- Seleção múltipla de prêmios no SelectCrate conforme o saldo de chaves e o custo por prêmio, com contador, desmarcação entre páginas e botão de cancelamento.
- Validação conjunta do inventário antes da entrega de itens nativos, incluindo empilhamento, quantidades, metadados e espaço liberado pelas chaves físicas consumidas.
- Revalidação na confirmação e proteção contra cliques repetidos, com restauração de inventário e chaves virtuais em falhas de aplicação dos itens.

### Alterado

- Controles do SelectCrate permanecem em slots distintos fora da área de prêmios, inclusive em menus menores.
- Comandos de recompensa são mantidos após a entrega de itens; os efeitos de plugins externos não oferecem validação de espaço nem reversão.

### Resolvido

- `Glowing: true` usa o brilho nativo do Paper sem adicionar encantamentos artificiais nem ocultar os encantamentos reais dos itens Oraxen/ItemEdit. `HideItemFlags` e `Flags` explícitos continuam sendo respeitados.

- Integração Oraxen/ItemEdit preserva os componentes originais do nome e da lore na entrega e na prévia, incluindo fontes, degradês e decorações; substituições explícitas da caixa continuam sendo aplicadas. O SelectCrate mantém a lore formatada ao acrescentar o custo e os avisos de seleção.

- A primeira construção de um item Oraxen/ItemEdit usa o item resolvido pela integração, evitando retornar o material de fallback na prévia ou entrega de prêmios.

- A abertura do SelectCrate por caixa física, menu e comandos não exige um slot vazio; a confirmação continua validando se todos os itens cabem após o débito das chaves físicas.

- Escolhas do SelectCrate que se tornam bloqueadas liberam o limite de seleção antes de novas escolhas ou navegação.
- Cliques repetidos em Confirmar não resgatam automaticamente as escolhas restantes após um lote ser rejeitado pela blacklist.

- Fechar ou trocar o menu durante a preparação dos itens invalida a confirmação capturada, preservando uma nova sessão aberta pela integração.

- Confirmação do SelectCrate rejeita permissões de abertura revogadas e menus cujos prêmios foram alterados ou reordenados.
- Falhas em comandos ou fogos não impedem o evento de recebimento de itens já entregues no SelectCrate.

## 2.0.0

### Mudanças incompatíveis

- Minecraft/Paper 26.2 e Java 25 passam a ser requisitos. O `api-version` agora é `26.2`, então servidores anteriores não carregam esta versão do plugin.

### Adicionado

- Paginação no SelectCrate com botões para avançar e voltar, mantendo o prêmio selecionado ao trocar de página.

### Alterado

- A seleção no SelectCrate usa o item configurado em `SelectionMarker`, sem modificar o item de exibição do prêmio.
- O nome do vidro decorativo do SelectCrate respeita `Crate.SelectCrate.Glass.Name`.
- Atualizado o `runServer` para iniciar Paper 26.2.
- Atualizados Adventure para 5.2.0, NBT-API para 2.16.0 e Oraxen para 1.207.0.
- Atualizados Gradle para 9.8.2 e Shadow para 9.6.1; o JAR distribuído agora é gerado diretamente pelo Shadow.
