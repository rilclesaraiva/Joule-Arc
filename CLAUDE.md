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
- Release: `./gradlew assembleRelease` → só o `.apk`, publicado como `joule-arc_vX.Y.Z.apk` (renomeado pelo CI). Sem `.aab`: o app não vai para a Play Store.
- Pacote (Android): `com.joulearc.app`, fixo · R8 ativo no release (sem ofuscação), nunca desligar
- Páginas legais: `docs/` (`privacy`, `terms`, `_config.yml`), com front matter para o Jekyll. URLs usadas no app em `core/Links.kt`. Pages já ativado pelo mantenedor.
- Ícone: `app/src/main/res/drawable/ic_launcher_foreground.xml` (+ background e monocromático), só muda se o mantenedor pedir. O atual é o símbolo elétrico de pilha (placas curta/longa alternadas, − e +), desenhado à mão em vetor a partir das imagens do mantenedor; `ic_app_logo.xml` é a versão com fundo usada na página Sobre
- minSdk 26 · targetSdk 36 · compileSdk 37 (exigido pelo material3 1.5) · AGP 9.1.x com Kotlin embutido (não aplicar `org.jetbrains.kotlin.android`) · Gradle 9.3.1

## Arquitetura
- `data/` expõe `BatteryRepository` (StateFlow de `BatterySnapshot`) e `BatteryReader` (lê `BatteryManager` e o broadcast `ACTION_BATTERY_CHANGED`).
- `data/` também tem `ChargeEstimator` (% por hora e tempo até cheia, a partir da corrente média e da capacidade estimada pelo contador de carga), `ChargeSessionTracker` (agrupa amostras em sessões), `SessionStore` (JSON em `filesDir/sessions.json`, sem Room de propósito) e `SessionRepository`.
- Navegação: só existe a **home** (`features/home/HomeScreen`) e páginas que ela abre (histórico, sessão, consumo, uso por app, configurações). Nada de abas nem pager. A pilha fica em `MainActivity` como texto (`home|history|session:<startMs>`), com `BackHandler` e `AnimatedContent`. O que é leitura do momento fica na home; o que é análise ou ajuste abre página; detalhes de uma leitura abrem em `ModalBottomSheet` (`ReadingSheet`); a seção Dispositivo abre uma folha única (`DeviceSheet`); tocar num app em Uso por app abre `features/usage/AppUsageScreen` (rota `app:<pacote>`). **Sem barras superiores fixas:** o título de cada página rola com o conteúdo (`core/PageTitle`; na home o título e a engrenagem ficam no começo da rolagem) e só o botão de voltar (`core/BackButton`, círculo levemente transparente) fica fixo sobre a tela. A rolagem da home e da lista de Uso por app é guardada em `MainActivity` (`homeScroll`, `usageList`) para voltar ao mesmo ponto. Configurações são listas sem categorias (`core/RowGroup`, `RowIcon`); a única categoria é **Sobre** (rota `about`, `features/settings/AboutScreen`: desenvolvedor, projeto, legal, convite ao repositório). Não há rodapé na home. O botão de configurações da home e o de voltar são `core/FloatingCircleButton`. Voltar preditivo (também no botão de voltar da tela, via `goBack`; página atual desliza com o dedo sobre a anterior, desenhada por baixo via `renderPage`): `PredictiveBackHandler` em `MainActivity` (opção `SettingsRepository.predictiveBack`, ligada por padrão; desligada usa `BackHandler`), com `enableOnBackInvokedCallback` no manifesto. Botões de redefinir usam `rememberDoubleTapConfirm` (dois toques em 1 s; o primeiro mostra um aviso).
- Home: `HeroCard` (mostrador bidirecional da corrente, %/h, tempo até encher ou restante, máximo/mínimo registrados), aviso âmbar, `CapacityCard`, `ReadingGrid` (mini mostradores com rótulo), cards de navegação, seção do aparelho.
- `features/monitor/` tem o `MonitorService` (serviço em primeiro plano, tipo `specialUse`, notificação persistente, lembretes de carga e de bateria fraca via `data/AlertTracker`). `SettingsRepository.persistentNotification` (ligada por padrão): desligada, o serviço não vira foreground (sem notificação), roda só com o app aberto (`MainActivity` o inicia por `LaunchedEffect`) e para sozinho depois (`shouldStopQuietly`); `BootReceiver` só o inicia se estiver ligada. Android exige notificação em serviço de primeiro plano, por isso não existe "serviço contínuo sem notificação". `features/history/` tem a página de histórico e `SessionDetailScreen` (curvas de corrente e nível, `ChargeSession.points`, só as 30 sessões mais novas guardam curva). `features/settings/` tem a página de configurações.
- Estado de energia: `BatterySnapshot.powerState` (CHARGING, DISCHARGING, FULL, PLUGGED_DRAINING, PLUGGED_PAUSED). `CHECKING` é o intervalo de 45 s (`AmberGate`) em que uma leitura estranha com carregador ainda não vira aviso. Os dois últimos são o estado **âmbar** (carregador conectado sem carregar ou "roubando" carga). `pluggedDischarging` vem do status DISCHARGING com carregador ou do sinal da corrente, que é aprendido por aparelho em `CurrentSignLearner` (por votos; só confia no sinal depois de ver os dois sinais e uma descarga na bateria). `isCharging` = `powerState == CHARGING`.
- Avaliações: `data/ReadingRatings` (`Scales`, `Band`, `RatingLabel`) com `core/Tone` (cores fixas verde/azul/âmbar/vermelho em `statusPalette()`). As faixas são regras práticas, não medições. Na tela cada faixa tem a mesma largura (`Scale.fraction`).
- Dados não públicos, sempre com alternativa: o que o carregador fornece vem dos extras `max_charging_current`/`max_charging_voltage` do broadcast de bateria (podem não existir); a capacidade do dispositivo vem de `com.android.internal.os.PowerProfile` por reflexão (`DeviceInfo`, pode falhar), usando `getAveragePower("battery.capacity")` e NÃO `getBatteryCapacity()` (que no Motorola edge 60 neo devolve 4079, valor ajustado, e não os 5000 nominais) e o usuário pode digitar a dele em Configurações.
- Configurações e valores registrados ficam em `SettingsRepository` (SharedPreferences). Bateria de duas células dobra corrente e contador no `BatteryReader`. Idioma: só pelo Android (idioma por app nas configurações do sistema, Android 13+, `xml/locales_config.xml` com en, es, pt-BR; ou idioma do sistema). O app NÃO tem seletor de idioma interno.
- `features/usage/` trata a permissão especial de acesso ao uso e a página de uso por app (`BatteryUsageScreen`, inspirada na tela de bateria do Android: alternância 24 h / 14 dias, gráfico de nível por hora ou de consumo por dia, lista por app com ícone). A leitura (`AppUsageReader`, via `queryEvents` recortado na janela, NÃO `queryAndAggregateUsageStats`, que não recorta) e os cálculos (`UsageEventAggregator`, `rankAppUsage`) ficam em `data/`. Segundo plano = tempo com serviço em primeiro plano; peso 0,5. Apps do sistema (FLAG_SYSTEM sem FLAG_UPDATED_SYSTEM_APP, ou sem ícone de launcher, que o app nem enxerga) ficam ocultos por padrão e o menu de três pontos da página os mostra; o ranking e os % incluem todos, o filtro é só de exibição. Tocar numa barra dos gráficos seleciona hora ou dia (`selectedDay`/`selectedHour` em `MainActivity`, que recortam a janela de `buildUsageReport`; tocar de novo volta ao geral; segurar mostra a hora). No AMOLED o item selecionado usa `surfaceContainerHighest` mais claro. O % por app = parcela × bateria gasta no período (`drainedPercent` do histórico de nível), por isso a janela é recortada no início do histórico.
- `data/DrainStats` (`computeDrainStats`) calcula %/h com tela ligada e apagada e por período do dia a partir do histórico de nível (só intervalos sem carregador, sem buracos de mais de 2 h, mínimo de 30 min por estado e 1 h por período); `features/drain/DrainScreen` é a página Consumo (mAh/h = %/h × capacidade estimada; autonomia = nível ÷ %/h). `LevelSample` guarda `screenOn` (lido por `PowerManager.isInteractive`; o serviço acorda também em `SCREEN_ON/OFF`).
- Exportar dados: `data/DataExport` escreve um JSON único em streaming (`JsonWriter`) para o `Uri` escolhido em `CreateDocument` (sem permissão de armazenamento) lançado em `MainActivity`; Configurações só recebe o callback `onExport`. Inclui dispositivo, bateria, fontes de capacidade, histórico de nível, sessões com curva, consumo e uso por app (período, por dia, por hora das últimas 24 h). Campo novo no export: atualizar `docs/privacy.md` e o README no mesmo commit.
- `data/LevelHistoryRepository` grava o nível da bateria (14 dias, `levels.json`) quando o nível ou o carregador muda ou a cada 10 min; `data/LevelCharts` calcula nível por hora, consumo por dia, bateria gasta no período e última carga a 100%.
- UI: `core/Gauges` (`BidirectionalGauge`, `MiniGauge`, `RangeBar`), `core/BarChart`, `core/LineChart` e `core/PillToggle` desenham tudo sem dependência. Ícones são vetores em `res/drawable/ic_*.xml` (sem biblioteca de ícones).
- **Animações são do app, nunca do sistema:** `MainActivity` usa `setContent(parent = appRecomposer())` (`core/Motion.kt`), um Recomposer com `MotionDurationScale` fixo em 1x (ignora a escala de animação do sistema) e relógio pausado quando a activity para. Durações em `AppMotion`. Não trocar por `setContent` comum.
- Saúde: `cycleCount` (`EXTRA_CYCLE_COUNT`, Android 14+, só se o fabricante informar) e capacidade estimada pelo contador de carga (`ChargeEstimator.capacityMah`). `BATTERY_PROPERTY_STATE_OF_HEALTH` NÃO é API pública (não compila): não usar. `ChargeEstimator` só amostra a capacidade quando o nível muda (o contador varia contínuo e o nível só em inteiros; amostrar sempre gera dente de serra). A saúde é `estimateHealthPercent(capacidade estimada, capacidade do dispositivo)`, com a capacidade do dispositivo digitada ou a do sistema.
- `data/ChargeInsights` compara sessões por fonte e por período do dia (só sessões de 5+ min que começaram abaixo de 80%; "melhor" só com 2+ opções e 2+ sessões).
- `features/tile/` (`BatteryTileService`) e `features/widget/` (`BatteryWidgetProvider`, RemoteViews, sem Glance). O serviço avisa o widget por broadcast explícito (`core/Actions.kt`), porque feature não importa feature. Rótulos de estado usam `powerState`.
- Unidade da corrente: `CurrentUnitLearner` (em `BatteryReader.unitLearner`, guardada em prefs) aprende se o aparelho reporta µA (3 leituras ≥ 20 000); só então divide tudo por 1000, para que 15 000 µA virem 15 mA e não 15 A. `LevelSample` guarda corrente, tensão e temperatura (colunas extras em `levels.json`, opcionais).
- Sinal: `BatterySnapshot.signedCurrentMa`/`signedPowerMw` (negativo quando a corrente sai da bateria, com ou sem carregador). `currentMa`/`powerMw` continuam positivos e são os usados pelas sessões e pela estimativa. `ChargeEstimator` estima tempo até encher (carregando) e tempo restante (descarregando).
- Rede (única): `data/updates/GitHubUpdatesRepository` pergunta a `api.github.com/repos/rilclesaraiva/Joule-Arc/releases/latest` (ETag e resposta guardados em prefs `updates`; consulta ao abrir do zero, no máximo a cada 10 min ao voltar; hora só marcada quando o GitHub respondeu; 404 = sem release ainda). `Version.kt` compara número a número. O aviso é `features/updates/UpdateScreen` em tela cheia sobre a pilha ("Agora não" vale até reabrir; sino adia 7/30 dias ou pula a versão; Configurações tem verificação manual e avisos). O app nunca instala: "Baixar" abre o navegador. `data/updates/DeveloperAvatar` baixa a foto do GitHub para `filesDir/github/developer_avatar` (arquivo temporário, ETag, 1x por semana, 10 min se não há foto) e a página Sobre a mostra. Permissão INTERNET só para isso; `allowBackup=false` mantém tudo fora do backup. Mudou rede ou coleta: atualizar `docs/privacy.md` e o README no mesmo commit.
- Telas vão até a barra de status: o conteúdo rola por baixo das barras do sistema (`windowInsetsPadding` dentro da rolagem, `core/insetsPadding` nas listas). Títulos de página ficam deslocados do botão de voltar.
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
- Textos do app: inglês, espanhol e português (`values/`, `values-es/` e `values-pt-rBR/`). Chave nova entra nos três e em `locales_config.xml` se for idioma novo.

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
- Foco: vistoria em aparelho real da home única (v0.1, ainda sem release). Já vistoriada pelo mantenedor: funciona; corrigidos layout estável, rolagem, escala do medidor, AMOLED, aviso âmbar com atraso.
- Pendentes: revisar as páginas legais antes do release público.
- Problemas conhecidos: nada vistoriado num aparelho real ainda. O CI passa (testes + release com R8); a sessão não compila localmente (sem acesso ao Google Maven), então o CI é a prova do build.
