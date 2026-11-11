# CLAUDE.md

## Projeto
Joule Arc — monitor inteligente de bateria e carregamento para Android: monitora o sistema de carga o tempo todo (notificação persistente), mostra a corrente em tempo real, identifica quando o aparelho carrega mais rápido, estima a saúde da bateria e o que drena mais em primeiro e segundo plano. Público: global.
- Tipo: público · Público-alvo: global
- Nome nos arquivos: `joule-arc` (vistoria: `joule-arc_vX.Y.Z-build<n>.apk`; release: `joule-arc_vX.Y.Z.apk`; título da Release: "Joule Arc vX.Y.Z")

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
- Versionamento: SemVer (regra; o número é calculado pelo [Unreleased], nunca escolhido): só correções (*Fixed*, *Security*) → PATCH; algo novo ou alterado compatível (*Added*, *Changed*, *Deprecated*) → MINOR; quebra de compatibilidade (*Removed*) → MAJOR. *Known issues* sozinho não gera versão. Trocar o sistema de versionamento só com pedido explícito do mantenedor.
- Release: `./gradlew assembleRelease` → só o `.apk`, publicado como `joule-arc_vX.Y.Z.apk` (renomeado pelo CI); o build para vistoria de todo push é `joule-arc_vX.Y.Z-build<n>.apk`, com o artefato do Actions de mesmo nome. Sem `.aab`: o app não vai para a Play Store.
- Pacote (Android): `com.joulearc.app`, fixo · R8 ativo no release (sem ofuscação), nunca desligar
- Páginas legais: `docs/` (`privacy`, `terms`, `_config.yml`), com front matter para o Jekyll. URLs usadas no app em `core/Links.kt`. Pages já ativado pelo mantenedor.
- Ícone: `app/src/main/res/drawable/ic_launcher_foreground.xml` (+ background e monocromático), só muda se o mantenedor pedir. O atual é o símbolo elétrico de pilha (placas curta/longa alternadas, − e +), desenhado à mão em vetor a partir das imagens do mantenedor; `ic_app_logo.xml` é a versão com fundo usada na página Sobre
- minSdk 26 · targetSdk 36 · compileSdk 37 (exigido pelo material3 1.5) · AGP 9.1.x com Kotlin embutido (não aplicar `org.jetbrains.kotlin.android`) · Gradle 9.3.1

## Arquitetura
Detalhes e decisões em [`ARCHITECTURE.md`](ARCHITECTURE.md): **ler antes de mexer no código** e atualizar no mesmo commit quando uma decisão mudar. O que nunca pode ser quebrado:
- Animações são do app, nunca do sistema: `MainActivity` usa `setContent(parent = appRecomposer())`; não trocar por `setContent` comum.
- Saúde: referência é `getBatteryCapacity()` (valor do sistema), sem campo para digitar; `BATTERY_PROPERTY_STATE_OF_HEALTH` não é API pública, não usar.
- Amostragem: nunca voltar a depender só de intervalo fixo longo; o serviço acorda em qualquer broadcast de bateria.
- Rede: só a consulta de versão e a foto do desenvolvedor no GitHub; mudou rede ou coleta, atualizar `docs/privacy.md` e o README no mesmo commit.
- Limite honesto: o ranking por app é **estimativa** (o Android não dá mAh por app) e a interface precisa dizer isso.

## Regras de estrutura
- Toda página que abre outra guarda a própria rolagem em `MainActivity` (`ScrollState`/`LazyListState` com `rememberSaveable`, como `homeScroll`, `usageList`, `historyList`, `settingsScroll`) e a recebe por parâmetro, para voltar ao mesmo ponto. Página nova que abre outra já nasce assim; revisar ao criar qualquer página. Vale também para o que mais deve ser lembrado ao voltar (aba, filtro, seleção).
- Configuração nova entra sozinha na categoria a que pertence em `SettingsScreen` e se organiza ali dentro; só cria categoria nova se nenhuma servir. Vale também para as existentes: nada solto fora de categoria. Texto da categoria nos três idiomas.
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
- A versão é calculada pela regra de versionamento. Número pedido fora da regra:
  explicar o número certo e esperar a confirmação com ele.
- Sem confirmação: nada de bump de versão ou mover [Unreleased].
- A IA nunca cria tag nem Release: o commit `chore(release): vX.Y.Z` na main basta,
  e o CI cria a tag e publica quando vê versão nova com seção no CHANGELOG.
- Toda atualização vai para os artefatos do Actions (build para vistoria) e o Claude
  avisa com o link. O mantenedor vistoria e só então pede o release público.
- Depois da confirmação e antes do commit de release, **revisão com contexto limpo**: a IA abre
  um subagente (ou sessão nova) que lê só o `CLAUDE.md` e o diff desde o último release e procura
  falhas de segurança, quebra das regras de estrutura e itens do CHANGELOG que não estão no código.
  Os achados vão para o mantenedor, que decide entre lançar assim ou corrigir antes (o que exige
  novo build e nova vistoria).
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
