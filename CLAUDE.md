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
- Stack: Kotlin + Jetpack Compose + Material 3 Expressive (`material3` 1.4+, `MaterialExpressiveTheme`)
- Build: `./gradlew assembleRelease` · Testes: `./gradlew testDebugUnitTest`
- Mapa: core → `app/src/main/java/com/joulearc/app/core` · data → `app/src/main/java/com/joulearc/app/data` · features → `app/src/main/java/com/joulearc/app/features`
- Versão: `app/build.gradle.kts` (`versionName`, `versionCode`) · lida no CI por `VERSION_CMD`
- Release: `./gradlew bundleRelease assembleRelease` → `.aab` e `.apk` de release
- Pacote (Android): `com.joulearc.app`, fixo · R8 ativo no release (sem ofuscação), nunca desligar
- Ícone: `app/src/main/res/drawable/ic_launcher_foreground.xml` (+ background e monocromático), só muda se o mantenedor pedir
- minSdk 26 · targetSdk/compileSdk 36

## Arquitetura
- `data/` expõe `BatteryRepository` (StateFlow de `BatterySnapshot`) e `BatteryReader` (lê `BatteryManager` e o broadcast `ACTION_BATTERY_CHANGED`).
- `features/monitor/` tem o `MonitorService` (serviço em primeiro plano, tipo `specialUse`, notificação persistente) e a tela principal.
- `features/usage/` trata a permissão especial de acesso ao uso (`UsageStatsManager`) e, depois, o ranking de apps em primeiro e segundo plano.
- `MainActivity` (entrada) monta as telas e liga as features por callbacks.
- Limite honesto: apps comuns não leem mAh por app (restrição do Android). O ranking de apps é uma **estimativa** a partir de tempo em primeiro/segundo plano, e a interface precisa dizer isso.
- Amostragem: ~2 s carregando, ~30 s fora da carga, para poupar bateria.

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
  linkadas no README e no app. Ainda não criadas: entram antes de qualquer release público.
- Coleta nova (SDK, permissão, chamada de rede, cookie) → atualizar a política
  no mesmo commit.
- Nenhuma afirmação sem respaldo: o que README, loja e páginas legais dizem
  precisa ser verdade no código. Na dúvida, não afirmar.

## Apoio e contato
- README termina com a seção de apoio e contato, usando os dados de *Mantenedor*.
- No app: nunca citar doação ou pagamento. Só "Quer apoiar o projeto? Entre no
  repositório do projeto" com link para o repo.

## Contexto atual
- Foco: esqueleto + serviço de monitoramento com notificação persistente + leitura ao vivo.
- Pendentes: histórico de sessões (persistência), janelas de carga mais rápida, saúde da bateria, ranking de apps (acesso ao uso), páginas legais, tile/widget.
- Problemas conhecidos: nenhum build rodou ainda; a sessão que criou o esqueleto não compilou (sem acesso ao Google Maven). O primeiro CI é a primeira prova.
