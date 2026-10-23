# DragonMineZ 2.2 — Minecraft 1.21.1 / NeoForge

Este ramo porta o código da versão **2.2**, a partir do commit `d824a29c` de
[lueefr/dragonminez](https://github.com/lueefr/dragonminez), usando como referência
o porte da 2.1.3 de [AgentMelinda](https://github.com/AgentMelinda/dragonminez-1.21.1),
commit `7318093c`. A licença GPL-3.0 e os créditos originais são preservados.

## Requisitos

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.238** ou posterior da linha 21.1.
- GeckoLib **4.9.2** para NeoForge 1.21.1.
- TerraBlender **1.21.1-4.1.0.8** para NeoForge.
- Curios **9.5.1+1.21.1** para NeoForge.

Instale o JAR e essas três dependências na pasta `mods` de uma instalação NeoForge
1.21.1. Clientes e servidor devem usar a mesma compilação do DragonMineZ.
MariaDB e HikariCP estão incorporados ao JAR. JEI é opcional; Weapon Leveling e
Apotheosis são integrações opcionais.

## Compilar e validar

```sh
./gradlew runData
./gradlew build
./gradlew runGameTestServer
./gradlew runClient
```

O Java usado pelo Gradle precisa ser o 21. Neste computador, o script abaixo
mantém os caches dentro de `~/Downloads`, conforme solicitado:

```sh
./scripts/build-port.sh
```

Os recursos gerados em `src/generated/resources` fazem parte do código-fonte.
Depois de alterar os provedores de dados, execute `runData` e inclua o resultado.
O JAR fica em `build/libs/dragonminez-1.21.1-2.2.jar`.

## Mudanças do porte

- Migração Forge → NeoForge: eventos, registros, atributos, spawn, dimensões,
  networking, menus, entidades e integração com as bibliotecas atuais.
- Dados persistentes de jogador em attachments serializáveis NeoForge. O formato
  interno do DragonMineZ mantém os campos da 2.2, inclusive os pontos de treino
  de precisão dupla, cooldowns e sistemas de progressão.
- Itens adaptados aos data components: encantamentos, livros, poções, trims,
  bolsa de senzu, mapas e fogos. Os nove encantamentos têm definições reais
  no registro de datapack da 1.21.1.
- Receitas da Pattern Station mantêm o tamanho de até 6×6, com codecs de JSON
  e sincronização de rede próprios.
- Renderização, HUD, câmera, modelos e mixins adaptados às APIs da 1.21.1.
- Datapacks convertidos para os diretórios e formatos atuais, inclusive loot,
  receitas, avanços, tags e modificadores de bioma.
- Os formatos de configuração, quests e conteúdo editável continuam legíveis;
  os valores e funcionalidades da 2.2 são a origem do porte.

## Escopo da validação

A geração de dados e os testes de integração são executados com o mod carregado
pelo NeoForge. Os testes verificam os nove encantamentos e a criação de livros,
a passagem de uma receita de 6×6 pelos codecs de JSON/rede, o carregamento de
receitas/loot estáticos, a construção das entidades e a restauração dos atributos,
pontos de treino e cooldowns de um jogador de teste.

Resultados finais e caminhos dos logs locais: `PORT-VALIDATION.md`.
Essas verificações não equivalem a uma campanha completa de gameplay, nem
validam integrações opcionais com servidores de banco de dados reais.
O carregamento de mundos antigos Forge 1.20.1 e addons binários da versão antiga
não foi validado; os testes usam mundos novos separados.
