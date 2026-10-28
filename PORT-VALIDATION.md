# Validação do porte 2.2 → NeoForge 1.21.1

Verificações locais realizadas em 22–23/10/2026, com Java 21 e NeoForge 21.1.238.
Código em `~/Downloads/dragonminez`, ramo `port/2.2-neoforge-1.21.1`.

| Verificação | Resultado | Evidência em `~/Downloads/dragonminez-port-tools` |
| --- | --- | --- |
| Compilação Java e empacotamento final | `BUILD SUCCESSFUL` | `build-final.log` |
| Geração dos dados da 2.2 | Todos os provedores concluídos | `build-final.log`, `datagen-4.log` |
| GameTests no servidor | 5 testes obrigatórios passaram | `gametest-4.log` |
| GameTests carregando o JAR final | 5 testes obrigatórios passaram | `gametest-packaged-3.log` |
| Cliente: criação de personagem | Mundo novo, conexão, tela e modelos renderizados, encerramento normal | `client-8.log` |
| Cliente: criação pelo pacote C2S | Servidor aceitou e sincronizou; HUD e personagem em terceira pessoa renderizados | `client-9.log` |
| Cliente: mundo normal com geração padrão | Terreno e dimensões carregados, esferas iniciais em Earth/Namek, criação de personagem, renderização e salvamento normal | `client-10.log` |
| JSONs de idioma | Helper `scripts/check_lang.sh` passou | Execução local com `jq` |
| Ferramentas de release existentes | 7 testes Python passaram | `release-tests.log` |
| Inspeção do JAR | ZIP íntegro, bytecode Java 21, metadados NeoForge, enum extension, 9 encantamentos, receitas e JarJar presentes | Inspeção local do arquivo final |

Os cinco GameTests verificam:

1. Construção de todos os tipos de entidade do namespace `dragonminez` (mais de 100).
2. Presença das receitas estáticas de cozimento e das tabelas de loot de entidades.
3. Registro dos nove encantamentos e aplicação em livros configurados.
4. Receita de 6×6 após ida e volta pelos codecs de JSON e rede, incluindo rejeição de grade incompleta.
5. Restauração dos atributos, cooldown e pontos de treino grandes sem perda de precisão no attachment de jogador.

O JAR final foi carregado diretamente como mod pelo servidor de GameTests,
sem os diretórios de classes do projeto no classpath. O launcher identificou
`dragonminez-1.21.1-2.2.jar`, versão 2.2. Os cinco testes passaram novamente.
MariaDB e HikariCP estão em `META-INF/jarjar` com os metadados de dependência.

SHA-256 do JAR final:

```text
3bfd6c521e3f058408c864fcbb4ea9f2b17e6d1609052fecd1dbe7fed4efc7c9
```

## Imagens

- Criação de personagem: `run/screenshots/2026-10-22_23.49.49.png`.
- HUD em mundo plano: `run/screenshots/2026-10-22_23.53.43.png`.
- HUD e personagem em mundo normal: `run/screenshots/2026-10-22_23.56.39.png`.

Os clientes foram executados em display virtual separado, com renderização por
software. O harness local de automação fica em `dragonminez-port-tools` e foi
explicitamente conferido como **ausente do JAR distribuído**. O teste plano usa
buscas distantes desativadas; o teste de mundo normal usa as configurações padrão.

## Limites

- Não foi feita uma campanha completa de gameplay, teste multiplayer prolongado,
  migração de mundos Forge 1.20.1 nem validação binária de addons antigos.
- Integrações opcionais com JEI, Apotheosis, Weapon Leveling, WorldGuard e um
  banco MariaDB real não receberam testes funcionais completos.
- Permanecem avisos de assets incompletos da base 2.2: modelos de alguns itens
  de roupas especiais e flores em vaso, além de referências de modelos do lótus.
  A base original mantém comentada a geração de quatro desses conjuntos de roupa.
- As receitas de Pattern Station funcionam pelo menu próprio; o recipe book
  vanilla ainda avisa que não conhece essa categoria personalizada.
- O ambiente local não possui a biblioteca nativa do narrador; isso gerou um
  aviso no cliente, sem impedir carregamento, renderização ou encerramento.

Não houve commit, push ou publicação no GitHub. As alterações do repositório
estão staged para revisão, conforme a instrução em `AI/Agents.md`.

## Combat visuals and customization — 2026-10-27

Validation for `fix/1.21.1-combat-visual-customization`, based on main `547af94f`:

- Java 21: `./gradlew build runGameTestServer -PincludeClientOnlyDevMods=false --no-daemon --console=plain` passed: 27 unit tests and all 37 required GameTests. New coverage checks offset-camera convergence, lock-on precedence and release, disk/beam/laser direction, the aim packet codec, contextual conditions and invalid configuration rejection.
- Disposable flat-world client with Sodium 0.8.13, Iris 1.8.14 beta 1, Complementary Unbound r5.9.3 and Dynamic Crosshair 9.11 passed. The API registered and produced `RANGED_WEAPON` in third person; intermediate 3D mesh weights were observed during form changes; 98 visible/bloom ki task executions retained depth testing with `GL_LEQUAL`. The aura shaders compiled successfully.
- Better Modlist 21.1.5: REPLACE, INSERT, ICON and unmodified menu layouts passed rectangle-overlap checks; clicking Mods opened its ModsScreen. This client also starts without Dynamic Crosshair or Iris installed.
- These client checks use Xvfb/Mesa software rendering and disposable worlds. They do not establish compatibility with every GPU, shader pack or optional-mod version.
- Validation logs: `/tmp/dmz-combat-visual-final-validation.log`, `/tmp/dmz-combat-visual-client-04.log`, `/tmp/dmz-combat-menu-client-01.log`. External client harnesses live in `dragonminez-port-tools/combat-visual-smoke` and `combat-menu-smoke`; harness classes and optional-mod API classes are absent from the distribution JAR.
- The aim packet is appended to the existing packet registry; network revision is now 2. Clients and servers must use the updated build together.

User configuration examples are in `docs/CONTEXTUAL-CONDITIONS.md` and
`docs/AURA-MESH-TRANSITIONS.md`. No wiki files or SeedGen/LOD code were changed.
