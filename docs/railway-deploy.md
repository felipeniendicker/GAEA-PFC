# Deploy do GAEA no Railway

Este guia publica o GAEA como três serviços no mesmo projeto e ambiente Railway:

- `Frontend`: React/Vite servido por Node;
- `Backend`: Spring Boot;
- `MySQL`: banco privado gerenciado pelo template Railway.

Não adicione senhas, dumps ou arquivos `.env` ao repositório. Configure segredos na aba
**Variables** do serviço e sele os valores sensíveis quando possível.

## 1. Verificações antes da publicação

Na raiz do repositório:

```bash
./mvnw test
./mvnw clean package
cd frontend
npm ci
npm run lint
npm run build
```

O teste MySQL com Testcontainers é ignorado automaticamente quando Docker não está
disponível. Antes da publicação definitiva, execute-o em um ambiente com Docker para
validar as migrations no MySQL real.

Use um banco vazio no primeiro deploy. Não restaure o backup local em produção sem um
plano separado de migração e validação de dados.

## 2. Criar os serviços

Crie um projeto Railway vazio e adicione os serviços `Frontend`, `Backend` e `MySQL`.
Conecte `Frontend` e `Backend` ao mesmo repositório Git.

| Serviço | Root Directory | Build Command | Start Command |
|---|---|---|---|
| Backend | `/` | `./mvnw -DskipTests clean package` | `java -jar target/gaea-api-0.0.1-SNAPSHOT.jar` |
| Frontend | `/frontend` | `npm ci && npm run build` | `npm start` |

Gere domínios públicos para `Frontend` e `Backend`. O MySQL deve permanecer privado;
não habilite TCP Proxy/Public Access para a comunicação da aplicação.

Configure os healthchecks:

| Serviço | Healthcheck Path |
|---|---|
| Backend | `/actuator/health` |
| Frontend | `/` |

O Railway fornece `PORT` automaticamente. O backend e o servidor estático do frontend
escutam em `0.0.0.0` usando essa porta.

## 3. Variáveis do backend

Adicione ao serviço `Backend`:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}

FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false
FLYWAY_CLEAN_DISABLED=true
FLYWAY_VALIDATE_ON_MIGRATE=true
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false

JWT_SECRET=<segredo aleatório com pelo menos 32 bytes>
JWT_ISSUER=gaea-api
JWT_EXPIRATION_MINUTES=60
LOGIN_EMAIL_LIMIT=5
LOGIN_ORIGIN_LIMIT=20
LOGIN_WINDOW_MINUTES=15
LOGIN_MAX_KEYS=10000
CORS_ALLOWED_ORIGINS=https://${{Frontend.RAILWAY_PUBLIC_DOMAIN}}
FRONTEND_BASE_URL=https://${{Frontend.RAILWAY_PUBLIC_DOMAIN}}

DOCUMENTOS_STORAGE_TIPO=local
DOCUMENTOS_STORAGE_DIRETORIO=/data/documentos
```

Não defina `PORT` manualmente, salvo se houver uma necessidade operacional específica.
`JWT_SECRET` deve ser estável entre deploys; trocá-lo encerra todas as sessões existentes.
Mantenha também `JWT_ISSUER` estável. Os limites de login são aplicados por instância;
se o backend ganhar múltiplas réplicas, substitua o limitador em memória por um mecanismo
compartilhado antes de considerar a proteção distribuída completa.
Use um valor diferente por ambiente e nunca reutilize a senha do banco.

`CORS_ALLOWED_ORIGINS` aceita uma lista separada por vírgulas. Use origens completas,
sem barra final, por exemplo:

```text
CORS_ALLOWED_ORIGINS=https://app.exemplo.com,https://gaea.up.railway.app
```

Não use `*` em produção.

## 4. Variável do frontend

Adicione ao serviço `Frontend` antes do build:

```text
VITE_API_URL=https://${{Backend.RAILWAY_PUBLIC_DOMAIN}}
```

Variáveis `VITE_*` são incorporadas ao bundle durante `npm run build`. Alterar a URL do
backend exige um novo deploy do frontend. O navegador precisa usar o domínio público
HTTPS do backend; domínios `*.railway.internal` não são acessíveis pelo cliente React.

## 5. Volume de documentos

Anexe um volume ao serviço `Backend` com mount path `/data`. Os PDFs ficam em
`/data/documentos`, enquanto seus metadados permanecem no MySQL.

Sem o volume, arquivos gravados no filesystem são efêmeros e podem desaparecer em
redeploys. Faça backup do volume conforme a criticidade dos documentos. Serviços com
volume não devem ser configurados com múltiplas réplicas escrevendo no mesmo diretório.

## 6. E-mail transacional

No ambiente `GAEA-PRODUCTION` do Railway, configure estas variáveis no serviço do backend.

```text
EMAIL_ENABLED=true
EMAIL_PROVIDER=brevo
BREVO_API_KEY=<chave da API do Brevo>
SMTP_FROM=<endereço de remetente verificado no Brevo>
FRONTEND_BASE_URL=https://${{Frontend.RAILWAY_PUBLIC_DOMAIN}}
```

Sele `BREVO_API_KEY` no Railway. Caso o frontend use domínio próprio, mantenha sua URL
pública HTTPS em `FRONTEND_BASE_URL`. O backend usa a API HTTPS do Brevo com timeout de
conexão de 3 segundos e de resposta de 5 segundos.

Para enviar e-mails localmente pelo Mailpit, use `EMAIL_ENABLED=true` e
`EMAIL_PROVIDER=smtp`. O SMTP local usa `localhost:1025` por padrão.

## 7. Primeiro deploy

1. Inicie o MySQL e confirme que as variáveis privadas estão disponíveis.
2. Publique o backend com Flyway habilitado.
3. Verifique nos logs a aplicação das migrations V1 a V17 e o status `UP` em
   `/actuator/health`.
4. Publique o frontend com `VITE_API_URL` definida.
5. Teste CORS, cadastro, confirmação de e-mail, login dos três perfis e os fluxos
   autorizados de cada perfil.
6. Envie um PDF, faça o download, redeploye o backend e confirme que o arquivo continua
   disponível no volume.

## 8. Checklist operacional

- MySQL sem acesso público;
- `JWT_SECRET` e `BREVO_API_KEY` selados;
- `JPA_SHOW_SQL=false`;
- Flyway habilitado e Hibernate em `validate`;
- CORS restrito ao domínio do frontend;
- links de e-mail usando HTTPS do frontend;
- volume montado em `/data` e incluído na política de backup;
- healthchecks dos dois serviços retornando HTTP 2xx;
- dumps SQL, logs e `.env` ausentes do commit e da imagem.

## Configuração como código

Não há `railway.toml` neste repositório de propósito. Frontend e backend usam o mesmo
repositório com root directories diferentes, e um arquivo único na raiz poderia aplicar
comandos ao serviço errado. As configurações acima devem ser definidas separadamente em
cada serviço Railway. Se futuramente forem criados arquivos por serviço, configure
explicitamente o caminho de cada arquivo no Railway.
