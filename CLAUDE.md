# CLAUDE.md

## Projeto
Joule Arc — monitor inteligente de bateria e carregamento para Android: monitora o sistema de carga o tempo todo (notificação persistente), mostra a corrente em tempo real, identifica quando o aparelho carrega mais rápido, estima a saúde da bateria e o que drena mais em primeiro e segundo plano. Público: global.

## Mantenedor
- Nome: rilclesaraiva · Contato: saraivarilcle@gmail.com
- Apoio: https://buymeacoffee.com/saraivarilcle · Licença: GPL-3.0
- País (assinatura): BR · Assinatura: `CN=Joule Arc, O=rilclesaraiva, C=BR`
- Segredos ficam em: Google Drive/Joule Arc/
- Repositório: `rilclesaraiva/Joule-Arc`

## Stack
- Stack: Kotlin + Jetpack Compose + Material 3 Expressive (`material3` 1.5.x, fixado em `gradle/libs.versions.toml`; no 1.4.0 as APIs Expressive são internas, então não voltar para 1.4)
- Build: `./gradlew assembleRelease` · Testes: `./gradlew testDebugUnitTest`
- Mapa: core → `app/src/main/java/com/joulearc/app/core` · data → `app/src/main/java/com/joulearc/app/data` · features → `app/src/main/java/com/joulearc/app/features`
- Versão: `app/build.gradle.kts` (`versionName`, `versionCode`) · lida no CI por `VERSION_CMD`
- Release: `./gradlew bundleRelease assembleRelease` → `.aab` e `.apk` de release
- Pacote (Android): `com.joulearc.app`, fixo · R8 ativo no release (sem ofuscação), nunca desligar
- Páginas legais: `docs/` (`privacy`, `terms`, `_config.yml`), com front matter para o Jekyll. URLs usadas no app em `core/SupportFooter.kt`. O mantenedor precisa ativar o Pages (Settings → Pages → branch `main`, pasta `/docs`).
- Ícone: `app/src/main/res/drawable/ic_launcher_foreground.xml` (+ background e monocromático), só muda se o mantenedor pedir
- minSdk 26 · targetSdk 36 · compileSdk 37 (exigido pelo material3 1.5) · AGP 9.1.x com Kotlin embutido (não aplicar `org.jetbrains.kotlin.android`) · Gradle 9.3.1

## Arquitetura
- `data/` expõe `BatteryRepository` (StateFlow de `BatterySnapshot`) e `BatteryReader` (lê `BatteryManager` e o broadcast `ACTION_BATTERY_CHANGED`).
- `data/` também tem `ChargeEstimator` (% por hora e tempo até cheia, a partir da corrente média e da capacidade estimada pelo contador de carga), `ChargeSessionTracker` (agrupa amostras em sessões), `SessionStore` (JSON em `filesDir/sessions.json`, sem Room de propósito) e `SessionRepository`.
- `features/monitor/` tem o `MonitorService` (serviço em primeiro plano, tipo `specialUse`, notificação persistente) e a tela ao vivo. `features/history/` tem a aba Histórico. A entrada (`MainActivity`) monta as abas.
- `features/usage/` trata a permissão especial de acesso ao uso e a aba Apps (ranking). A leitura (`AppUsageReader`, `queryAndAggregateUsageStats`) e o cálculo (`rankAppUsage`) ficam em `data/`. Segundo plano = tempo com serviço em primeiro plano; peso 0,5.
- `features/health/` tem a aba Saúde: `cycleCount` (`EXTRA_CYCLE_COUNT`, Android 14+, só se o fabricante informar) e capacidade estimada pelo contador de carga (`ChargeEstimator.capacityMah`). `BATTERY_PROPERTY_STATE_OF_HEALTH` NÃO é API pública (não compila): não usar. Não há API pública para a capacidade nominal, então a saúde é `estimateHealthPercent(capacidade estimada, capacidade nominal digitada pelo usuário)`, guardada em `SettingsRepository` (SharedPreferences).
- `data/ChargeInsights` compara sessões por fonte e por período do dia (só sessões de 5+ min que começaram abaixo de 80%; "melhor" só com 2+ opções e 2+ sessões).
- `features/tile/` (`BatteryTileService`) e `features/widget/` (`BatteryWidgetProvider`, RemoteViews, sem Glance). O serviço avisa o widget por broadcast explícito (`core/Actions.kt`), porque feature não importa feature.
- Sinal: `BatterySnapshot.signedCurrentMa`/`signedPowerMw` (negativo na bateria). `currentMa`/`powerMw` continuam positivos e são os usados pelas sessões e pela estimativa.
- `MainActivity` (entrada) monta as telas e liga as features por callbacks.
- Limite honesto: apps comuns não leem mAh por app (restrição do Android). O ranking de apps é uma **estimativa** a partir de tempo em primeiro/segundo plano, e a interface precisa dizer isso.
- Amostragem: 500 ms com o app visível, 2 s carregando com a tela apagada, 30 s fora disso. O serviço acorda na hora em qualquer broadcast de bateria (plugar, desplugar, mudança de nível). Nunca voltar a depender só de intervalo fixo longo.

## Regras de estrutura
- Feature não importa outra feature; o compartilhado sobe para core/data.
- Navegação entre features passa pela entrada (rotas registradas lá).
- Banco, APIs e arquivos só via data. Core sem regra de negócio.
- Seguir as convenções da stack. Nenhuma dependência nova sem justificar.

## Fluxo de trabalho
- Uma tarefa por vez, mudanças pequenas. Não refatorar fora do escopo.
- Tudo direto na `main`: sem branches (`wip/`, `feature/`…) e sem PR. Cada push
  gera o build para vistoria. Se o ambiente exigir commit ao parar, é na `main`.
- Rodar build e testes antes do push quando o ambiente permitir. Se a sessão não
  consegue compilar (ex: nuvem sem SDK ou sem acesso às dependências), o CI é a
  única prova, e a IA diz isso em vez de afirmar que testou.
- Só dizer que terminou quando o CI daquele commit passar no GitHub.
  CI vermelho é corrigido antes de qualquer tarefa nova.
- Registrar toda mudança em [Unreleased] no CHANGELOG.
- Preferir remover a acumular: o que não é usado sai.
- Na dúvida, perguntar. Nunca inventar API.
- Bloqueio de segurança do ambiente (proxy, permissão, leitura de credencial) nunca
  é contornado: o passo vai para o CI ou para o mantenedor.

## Release (regra absoluta)
- Só com confirmação explícita do mantenedor citando a versão
  ("confirmo release v1.2.0"), na conversa.
  "Ok", "beleza" ou silêncio não contam.
- Sem confirmação: nada de bump de versão ou mover [Unreleased].
- A IA nunca cria tag nem Release: o commit `chore(release): vX.Y.Z` na main basta,
  e o CI cria a tag e publica quando vê versão nova com seção no CHANGELOG.
- Toda atualização vai para os artefatos do Actions (build para vistoria) e o Claude
  avisa com o link. O mantenedor vistoria e só então pede o release público.
- Entre a vistoria e o commit de release, só mudam versão e CHANGELOG; mudou código, nova vistoria.

## Segredos
- Nunca commitar chaves, keystores, senhas ou .env.
- Secrets de assinatura: a IA gera e envia os arquivos direto ao mantenedor, sem nunca
  ler ou exibir o conteúdo. O mantenedor guarda no local definido em *Mantenedor*.
  Nunca no repo, nunca escritos no texto do chat.
- Cadastrar secrets no GitHub é sempre tarefa manual do mantenedor.

## Idioma
- README e CHANGELOG: inglês (projeto global).
- O README sempre tem a seção de transparência sobre IA. Nunca remover.
- Textos do app: inglês e português (`values/` e `values-pt-rBR/`).

## Projeto público (se for usado por terceiros)
- Páginas legais em `docs/` (privacidade, termos), publicadas no GitHub Pages e
  linkadas no README e no app. Criadas em `docs/`; mantê-las verdadeiras com o código.
- Coleta nova (SDK, permissão, chamada de rede, cookie) → atualizar a política
  no mesmo commit.
- Nenhuma afirmação sem respaldo: o que README, loja e páginas legais dizem
  precisa ser verdade no código. Na dúvida, não afirmar.

## Apoio e contato
- README termina com a seção de apoio e contato, usando os dados de *Mantenedor*.
- No app: nunca citar doação ou pagamento. Só "Quer apoiar o projeto? Entre no
  repositório do projeto" com link para o repo.

## Contexto atual
- Foco: primeira vistoria em aparelho real de tudo o que existe (v0.1, ainda sem release).
- Pendentes: gráficos das sessões de carga. Ativar o GitHub Pages. Revisar as páginas legais antes do release público.
- Problemas conhecidos: nada vistoriado num aparelho real ainda. O CI passa (testes + release com R8); a sessão não compila localmente (sem acesso ao Google Maven), então o CI é a prova do build.
