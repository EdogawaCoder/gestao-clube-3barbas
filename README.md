# Clube 3 Barbas

Aplicação web para administrar o programa de assinaturas da Barbearia 3 Barbas. A base inicial contém uma API Java segura, um portal web responsivo, configuração de Firebase Hosting e a primeira regra de negócio executável: o rateio proporcional por atendimentos.

## Arquitetura

```text
Navegador
  └─ Firebase Hosting (portal estático)
       └─ /api/** → Cloud Run (Spring Boot / Java 21)
                       ├─ Firebase Authentication + Identity Platform
                       └─ Cloud Firestore
```

O Firebase Hosting não executa uma JVM. Por isso, os arquivos do portal ficam no Hosting e a API Java é publicada no Cloud Run, usando o mesmo projeto Google Cloud/Firebase. O rewrite já está definido em `firebase.json` para `southamerica-east1`.

> Cloud Run exige um projeto com faturamento habilitado. Credenciais JSON não devem ser copiadas para o repositório ou para a imagem; no Cloud Run a aplicação usa Application Default Credentials.

## Tecnologias

- Java 21 e Spring Boot 4.1
- Firebase Admin SDK para Java
- Firebase Authentication por e-mail e senha
- Cloud Firestore
- Vite 8 e Firebase Web SDK
- Maven, JUnit 5 e AssertJ

## Estado atual

- Estrutura de produção para Hosting + Cloud Run.
- Autenticação por Firebase ID Token e RBAC por custom claim `role`.
- E-mail verificado e perfil de acesso exigidos também na API.
- Provisionamento administrativo do primeiro Gerente e dos demais usuários.
- Modo local isolado: cabeçalhos `X-Dev-*` são recusados fora do profile `local` e no Cloud Run.
- Simulação de rateio 60/40 com distribuição determinística dos centavos.
- Política de status ATIVO/PENDENTE/CANCELADO/EXCLUÍDO.
- Tela de login, primeiro acesso, recuperação, menus por perfil, health real, dashboard e simulador.
- Firestore fechado para acesso direto do navegador.
- Modelo de dados, requisitos, backlog e decisões em `docs/`.

Os cards operacionais do dashboard ainda exibem `—`: não há dados fictícios. Eles serão ligados às projeções do Firestore depois do fluxo de assinantes e pagamentos.

O endpoint atual é um **simulador**. O futuro fechamento financeiro não reutilizará valores enviados pelo navegador: carregará no servidor o pagamento confirmado, o snapshot de percentuais do ciclo e somente atendimentos válidos persistidos.

## Executar localmente

Pré-requisitos: Java 21, Maven 3.9+ e Node.js 22.12+.

Terminal 1 — API:

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Terminal 2 — portal:

```powershell
cd web
npm install
npm run dev
```

Abra `http://localhost:5173` e use **Acessar como Gerente**. Esse botão e os cabeçalhos de desenvolvimento não existem no build de produção.

Para testar a autenticação real, copie `web/.env.example` para `web/.env`, preencha a configuração pública do aplicativo web Firebase e não use o perfil `local` no backend.

## Verificações

```powershell
cd backend
mvn test

cd ..\web
npm run build:preview
```

`build:preview` valida o código sem exigir um projeto Firebase. O comando de produção `npm run build` falha de propósito se alguma variável `VITE_FIREBASE_*` estiver ausente, impedindo publicar um login inoperante.

## Habilitar a autenticação real

No projeto Firebase/Google Cloud:

1. Atualize o Firebase Authentication para **Authentication with Identity Platform**.
2. Habilite E-mail/Senha.
3. Crie um aplicativo Web e copie `web/.env.example` para `web/.env`, preenchendo os valores públicos.
4. Configure os domínios autorizados e o modelo de e-mail de verificação.
5. Para usar o Admin SDK localmente, crie um OAuth Client do tipo **Desktop** e execute `gcloud auth application-default login --client-id-file='C:\caminho\oauth-desktop.json'`. O client ID padrão do `gcloud` não é aceito pelo Firebase Authentication.

Provisione o primeiro Gerente:

```powershell
$projectId = 'SEU_FIREBASE_PROJECT_ID'
$env:GOOGLE_CLOUD_PROJECT = $projectId
$env:CLUBE_USER_EMAIL = 'gerente@exemplo.com'
$env:CLUBE_USER_NAME = 'Nome do gerente'
$env:CLUBE_USER_ROLE = 'GERENTE'

cd backend
mvn -B -ntp exec:java '-Dexec.mainClass=br.com.clube3barbas.bootstrap.ProvisionUserCommand'
```

O comando cria uma senha aleatória que não é exibida. O novo usuário começa por **Esqueci minha senha**, define sua própria senha, entra no portal e confirma o e-mail. Perfis aceitos: `GERENTE`, `ADMINISTRATIVO` e `BARBEIRO`. Em usuário existente, o comando atualiza o claim e revoga as sessões antigas para a mudança valer imediatamente.

O uso local acima segue a configuração especial exigida pelo [Firebase Admin SDK com credenciais de usuário final](https://firebase.google.com/docs/admin/setup#testing_with_gcloud_end_user_credentials). Em produção, use sempre a identidade de serviço do Cloud Run; não distribua chaves JSON de service account.

## Usar o Render em vez do Cloud Run

O `Dockerfile` na raiz do repositório permite hospedar a API em qualquer serviço baseado em contêiner, incluindo o [Render](https://render.com) — útil para evitar a exigência de faturamento habilitado que o Cloud Run impõe. O Firestore continua funcionando normalmente nesse cenário e continua gratuito no plano Spark do Firebase; a diferença é que, fora do Cloud Run, não existe identidade automática (Application Default Credentials), então a API precisa receber explicitamente uma chave de conta de serviço:

1. No [Console do Firebase](https://console.firebase.google.com), abra o projeto → **Configurações do projeto** → **Contas de serviço** → **Gerar nova chave privada**. Isso baixa um arquivo JSON.
2. Confirme que existe um banco **Cloud Firestore** criado no projeto (Build → Firestore Database → Criar banco de dados, modo Nativo).
3. No painel do serviço no Render: **Environment → Secret Files** → adicione um arquivo (por exemplo `firebase-service-account.json`) colando o conteúdo do JSON baixado. O Render monta esse arquivo em `/etc/secrets/firebase-service-account.json`.
4. Ainda em **Environment → Environment Variables**, adicione:
   - `GOOGLE_APPLICATION_CREDENTIALS=/etc/secrets/firebase-service-account.json`
   - `GOOGLE_CLOUD_PROJECT=<SEU_FIREBASE_PROJECT_ID>`
   - `FIREBASE_ENABLED=true`
5. Salve; o Render reimplanta automaticamente.
6. Verifique acessando `https://<seu-servico>.onrender.com/api/v1/health/firestore`: `{"firestore":"UP"}` confirma a credencial e o banco; `"DOWN"` indica problema de credencial ou banco ainda não criado; `"DISABLED"` indica que `FIREBASE_ENABLED` não está `true`.

Nunca commite a chave JSON baixada no repositório — ela concede acesso administrativo ao projeto inteiro.

## Publicação

Depois de configurar o Firestore, instalar `gcloud`/Firebase CLI e preencher `web/.env`:

```powershell
$projectId = 'SEU_FIREBASE_PROJECT_ID'
$serviceAccount = "clube-3-barbas-api@$projectId.iam.gserviceaccount.com"
gcloud config set project $projectId

gcloud iam service-accounts create clube-3-barbas-api --project $projectId --display-name 'Clube 3 Barbas API'
gcloud projects add-iam-policy-binding $projectId --member "serviceAccount:$serviceAccount" --role roles/firebaseauth.admin
gcloud projects add-iam-policy-binding $projectId --member "serviceAccount:$serviceAccount" --role roles/datastore.user

gcloud run deploy clube-3-barbas-api --project $projectId --source backend --region southamerica-east1 --service-account $serviceAccount --allow-unauthenticated --set-env-vars FIREBASE_ENABLED=true

cd web
npm ci
npm run build
cd ..
firebase deploy --project $projectId --only firestore,hosting
```

Usar o mesmo `$projectId` nos dois CLIs evita publicar o Hosting e o Cloud Run em projetos diferentes. `--allow-unauthenticated` permite que o Hosting alcance o serviço; os endpoints continuam protegidos pelo Firebase ID Token na aplicação.

## Documentação do projeto

- [Requisitos funcionais](docs/requisitos-funcionais.md)
- [Arquitetura e segurança](docs/arquitetura.md)
- [Modelo Firestore](docs/modelo-firestore.md)
- [Backlog do MVP](docs/backlog-mvp.md)
- [Decisões pendentes](docs/decisoes-pendentes.md)

Referências técnicas oficiais: [Firebase Hosting com Cloud Run](https://firebase.google.com/docs/hosting/cloud-run), [Admin SDK Java](https://firebase.google.com/docs/admin/setup), [Firebase Authentication](https://firebase.google.com/docs/auth) e [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html).
# gestao-clube-3barbas
