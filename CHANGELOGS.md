# Changelogs

## Unreleased

### Adicionado

- Seleção múltipla de prêmios no SelectCrate conforme o saldo de chaves e o custo por prêmio, com contador, desmarcação entre páginas e botão de cancelamento.
- Validação conjunta do inventário antes da entrega de itens nativos, incluindo empilhamento, quantidades, metadados e espaço liberado pelas chaves físicas consumidas.
- Revalidação na confirmação e proteção contra cliques repetidos, com restauração de inventário e chaves virtuais em falhas de aplicação dos itens.

### Alterado

- Controles do SelectCrate permanecem em slots distintos fora da área de prêmios, inclusive em menus menores.
- Comandos de recompensa são mantidos após a entrega de itens; os efeitos de plugins externos não oferecem validação de espaço nem reversão.

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
