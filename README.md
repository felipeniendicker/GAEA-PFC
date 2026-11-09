# GAEA — Gestão e Acompanhamento de Estágios Acadêmicos

O GAEA é um sistema web para gestão e acompanhamento de estágios acadêmicos. O projeto centraliza o relacionamento entre instituição de ensino, aluno e supervisor da empresa, permitindo acompanhar processos, vínculos de supervisão, pendências, prazos, decisões e registros de auditoria.

O sistema busca reduzir o acompanhamento disperso por planilhas, mensagens e documentos isolados. Esta versão foi desenvolvida como Projeto Final de Curso (PFC) e preparada para a entrega acadêmica de 09/11/2026.

## Tecnologias utilizadas

### Frontend

- React 19.2.8;
- Vite 8.3.0;
- JavaScript, HTML e CSS;
- API `fetch` do navegador para comunicação com o backend.

### Backend

- Java 17;
- Spring Boot 4.1.1;
- Spring Web MVC;
- Spring Data JPA;
- Spring Security;
- Bean Validation;
- JWT com JJWT 0.12.6;
- BCrypt para proteção de senhas;
- Flyway para versionamento do esquema;
- MySQL 8 — versão 8.0.44 utilizada na homologação das migrações;
- H2 nos testes automatizados;
- integração com ViaCEP;
- suporte a SMTP configurável para os fluxos de verificação e convite por e-mail.

## Arquitetura

O GAEA utiliza uma arquitetura cliente-servidor:

1. o frontend React apresenta as áreas permitidas a cada perfil e consome a API por HTTP;
2. a API REST do Spring Boot autentica o usuário, valida as entradas e aplica as regras de negócio;
3. services concentram os fluxos de negócio e as verificações de vínculo;
4. repositories do Spring Data JPA acessam o MySQL;
5. migrations Flyway mantêm a evolução versionada do esquema.

No backend, os principais pacotes separam configuração, segurança, controllers, DTOs, entidades, repositories, services, exceções e auditoria. As respostas de negócio utilizam DTOs para evitar a exposição direta das entidades JPA.

A autenticação é stateless. Após o login, a API emite um JWT, enviado nas chamadas protegidas pelo cabeçalho `Authorization: Bearer <token>`. A autorização considera o perfil autenticado e, nos processos, também o aluno proprietário, a instituição responsável ou o vínculo ativo e confirmado do supervisor.

### Organização das pastas

```text
GAEA-PFC/
├── frontend/
│   ├── src/
│   │   ├── App.jsx
│   │   └── App.css
│   ├── package.json
│   └── vite.config.js
├── src/
│   ├── main/
│   │   ├── java/Gaea/Api/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   └── test/java/Gaea/Api/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

As migrations disponíveis vão de V1 a V7 e contemplam o esquema inicial, instituições, convites de alunos, processos vinculados, verificação de e-mail, histórico de supervisão, pendências e prazos.

## Perfis e funcionalidades

### Instituição de ensino

- dashboard com totais, distribuição por status e percentual de aprovação dos processos da própria instituição;
- cadastro, consulta, edição, desativação e reativação de alunos vinculados à instituição;
- geração de convite para ativação segura da conta do aluno;
- consulta e análise dos processos de seus alunos;
- transições de status, decisões e reabertura de processos conforme as regras implementadas;
- registro, consulta e resolução de pendências;
- registro, consulta e atualização de prazos;
- consulta ao histórico completo dos processos;
- consulta aos registros de auditoria da própria instituição.

### Aluno

- dashboard limitado aos próprios processos;
- ativação da conta por convite e definição da própria senha;
- solicitação de novo estágio com dados da empresa, período e supervisor indicado;
- preenchimento de endereço com consulta ao ViaCEP;
- consulta dos próprios processos e da situação do vínculo do supervisor;
- substituição do supervisor indicado após recusa, preservando o histórico;
- consulta a histórico, pendências e prazos dos próprios processos.

### Empresa / Supervisor

- cadastro e verificação obrigatória do e-mail da conta;
- dashboard limitado aos processos com supervisão ativa e confirmada;
- consulta das solicitações de supervisão dirigidas ao e-mail verificado;
- confirmação ou recusa do vínculo, com justificativa obrigatória na recusa;
- consulta dos estágios supervisionados;
- consulta ao histórico, às pendências e aos prazos dos processos autorizados.

## Principais endpoints

Os endpoints abaixo representam os grupos efetivamente expostos pela API. Todos os endpoints protegidos exigem autenticação e continuam sujeitos às regras de perfil e vínculo.

| Grupo | Exemplos |
|---|---|
| Autenticação | `POST /api/auth/login`, `POST /api/auth/cadastro` |
| Ativação de aluno | `POST /api/ativacoes/aluno` |
| Verificação de e-mail | `POST /api/verificacoes-email/confirmar`, `POST /api/verificacoes-email/reenviar` |
| Gestão institucional de alunos | `/api/instituicoes/alunos` |
| Processos | `/api/processos`, `/api/processos/meus`, `/api/processos/{id}` |
| Histórico | `GET /api/processos/{id}/historico`, `GET /api/processos/{id}/historico-completo` |
| Supervisão | `/api/supervisao/solicitacoes` |
| Pendências | `/api/processos/{id}/pendencias` |
| Prazos | `/api/processos/{id}/prazos` |
| Dashboard | `GET /api/dashboard/resumo` |
| Auditoria | `GET /api/auditoria` |
| CEP | `GET /api/cep/{cep}` |

## Requisitos funcionais

A lista abaixo preserva os nomes e as descrições da seção de requisitos funcionais da monografia. A situação corresponde à auditoria da versão preparada para 09/11/2026.

| RF | Nome e descrição oficial | Situação na entrega |
|---|---|---|
| RF01 | **Autenticar usuário no sistema.** O sistema deverá permitir que usuários cadastrados realizem login utilizando e-mail e senha. | Implementado |
| RF02 | **Controlar acesso conforme o perfil do usuário.** O sistema deverá controlar o acesso às funcionalidades de acordo com o perfil do usuário autenticado. | Implementado |
| RF03 | **Cadastrar e gerenciar usuários.** O sistema deverá permitir o cadastro e o gerenciamento dos usuários que utilizarão o GAEA. | **Parcial** — há gestão institucional de alunos, convites e cadastro de supervisor, mas não uma gestão completa de todos os perfis. |
| RF04 | **Cadastrar processo de estágio.** O sistema deverá permitir o cadastro de um novo processo de estágio com as informações do aluno, da empresa e do período do estágio. | Implementado |
| RF05 | **Consultar e acompanhar processos de estágio.** O sistema deverá permitir a consulta dos processos de estágio e de sua situação atual. | Implementado |
| RF06 | **Atualizar situação do processo de estágio.** O sistema deverá permitir a atualização da situação do processo de estágio conforme sua análise. | Implementado |
| RF07 | **Enviar e gerenciar documentos do estágio.** O sistema deverá permitir o envio de documentos relacionados ao processo de estágio. | **Não implementado** |
| RF08 | **Analisar documentos do processo de estágio.** O sistema deverá permitir a análise dos documentos enviados para o processo de estágio. | **Não implementado** |
| RF09 | **Consultar endereço por CEP.** O sistema deverá consultar informações de endereço a partir do CEP informado pelo usuário. | Implementado com ressalva — o ViaCEP está integrado ao fluxo de solicitação do aluno, sem acesso direto pelo perfil Empresa/Supervisor. |
| RF10 | **Enviar notificações por e-mail.** O sistema deverá enviar notificações por e-mail sobre prazos, pendências e alterações relacionadas aos processos de estágio. | **Parcial** — existe infraestrutura de e-mail para ativação e verificação, mas não notificações completas de prazos, pendências e alterações. |
| RF11 | **Visualizar painel de acompanhamento.** O sistema deverá apresentar um painel com informações sobre o andamento dos processos de estágio. | Implementado |
| RF12 | **Registrar ações para auditoria.** O sistema deverá registrar ações realizadas pelos usuários para permitir o acompanhamento das alterações efetuadas. | Implementado |
| RF13 | **Consultar histórico do processo de estágio.** O sistema deverá permitir a consulta do histórico de informações e alterações relacionadas ao processo de estágio. | Implementado |
| RF14 | **Gerenciar dados pessoais e privacidade.** O sistema deverá permitir o tratamento dos dados pessoais dos usuários de acordo com suas permissões de acesso e as necessidades do processo de estágio. | Implementado no escopo auditado de autorização e minimização de acesso |
| RF15 | **Registrar pendência no processo de estágio.** O sistema deverá permitir o registro de pendências relacionadas a um processo de estágio. | Implementado |
| RF16 | **Consultar pendências do processo de estágio.** O sistema deverá permitir a consulta das pendências registradas em um processo de estágio. | Implementado |
| RF17 | **Controlar prazos do processo de estágio.** O sistema deverá permitir o registro e o acompanhamento de prazos relacionados ao processo de estágio. | Implementado |
| RF18 | **Consultar prazos do processo de estágio.** O sistema deverá permitir a consulta dos prazos registrados em um processo de estágio. | Implementado |
| RF19 | **Consultar registros de auditoria.** O sistema deverá permitir a consulta dos registros de auditoria gerados pelas ações realizadas no sistema. | Implementado |

Resumo da auditoria:

- 19 requisitos funcionais previstos;
- 15 implementados;
- 2 parciais: RF03 e RF10;
- 2 não implementados: RF07 e RF08;
- percentual comprovado: **78,95%** (`15 / 19 × 100`).

Requisitos parciais não foram contabilizados como concluídos.

## Segurança e privacidade

- autenticação stateless com JWT;
- senhas protegidas com BCrypt;
- validação de conta ativa e, quando aplicável, de instituição ativa;
- autorização no backend por perfil e vínculo com o processo;
- isolamento dos processos e registros de auditoria entre instituições;
- verificação de e-mail do supervisor por token aleatório, de uso único, com expiração e armazenamento somente do hash;
- ativação do aluno por convite com as mesmas propriedades de segurança;
- DTOs e Bean Validation nos fluxos de entrada;
- tratamento centralizado de erros, sem envio de detalhes internos nas respostas inesperadas;
- registro de ações relevantes em auditoria;
- aceite dos Termos de Uso e da Política de Privacidade nos fluxos de cadastro e ativação.

Esses mecanismos apoiam a proteção dos dados tratados pelo sistema, mas esta documentação **não afirma conformidade integral com a LGPD**. Uma avaliação completa ainda exigiria revisão jurídica, definição formal de bases legais, ciclo de retenção, atendimento aos direitos dos titulares e procedimentos operacionais de segurança.

## Como executar localmente

### Pré-requisitos

- JDK 17;
- MySQL 8;
- Node.js e npm compatíveis com Vite 8. A versão validada no ambiente da entrega foi Node.js 24.15.0 com npm 11.12.1;
- portas 8080 e 5174 disponíveis, ou configuração equivalente.

### Banco de dados

Crie um banco MySQL local para o GAEA. A configuração padrão espera:

```text
jdbc:mysql://localhost:3306/gaea
```

O projeto não armazena a senha do banco no repositório. Defina as credenciais por variáveis de ambiente. A evolução do esquema é feita pelas migrations em `src/main/resources/db/migration`.

> Antes de habilitar o Flyway em um banco com tabelas ou dados existentes, faça backup e confira a compatibilidade do esquema. Não use `clean` em bancos que devam ser preservados.

### Variáveis de ambiente

| Variável | Obrigatória | Finalidade |
|---|---:|---|
| `DB_PASSWORD` | Sim na configuração padrão | Senha do usuário MySQL configurado para a aplicação |
| `JWT_SECRET` | Sim | Chave de assinatura dos JWTs; deve ser longa, aleatória e mantida fora do repositório |
| `SPRING_DATASOURCE_URL` | Não | Sobrescreve a URL JDBC padrão |
| `SPRING_DATASOURCE_USERNAME` | Não | Sobrescreve o usuário JDBC padrão |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Não | Estratégia do Hibernate; a configuração do projeto usa `validate` por padrão |
| `FLYWAY_ENABLED` | Não | Habilita ou desabilita a execução do Flyway na configuração do projeto |
| `SPRING_FLYWAY_URL` | Quando Flyway usa conexão separada | URL JDBC usada pelo Flyway |
| `SPRING_FLYWAY_USER` | Quando Flyway usa conexão separada | Usuário de migração |
| `SPRING_FLYWAY_PASSWORD` | Quando Flyway usa conexão separada | Senha do usuário de migração |
| `CORS_ALLOWED_ORIGINS` | Não | Lista, separada por vírgulas, de origens permitidas |
| `FRONTEND_BASE_URL` | Não | URL usada para compor links destinados ao frontend |
| `EMAIL_ENABLED` | Não | Habilita o envio de e-mails; o padrão local é `false` |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | Quando e-mail está habilitado | Conexão com o servidor SMTP |

Exemplo de configuração temporária no PowerShell, sem registrar valores no projeto:

```powershell
$env:DB_PASSWORD = Read-Host 'Senha do MySQL' -MaskInput
$env:JWT_SECRET = Read-Host 'Chave JWT' -MaskInput
```

### Backend

Na raiz do repositório:

```powershell
.\mvnw.cmd spring-boot:run
```

Com a configuração padrão, a API fica disponível em `http://localhost:8080`.

Para uma instalação nova e vazia, as migrations podem ser executadas de forma controlada habilitando `FLYWAY_ENABLED=true`. O projeto usa `ddl-auto=validate`; o Hibernate valida o esquema, mas não substitui as migrations.

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

O Vite utiliza a porta `5174` e encaminha chamadas relativas a `/api` para `http://localhost:8080`. Para usar outro backend sem alterar arquivos permanentes:

```powershell
$env:VITE_PROXY_TARGET = 'http://127.0.0.1:8080'
npm run dev
```

Também é possível definir `VITE_API_URL` para que o frontend use uma URL absoluta. Nesse caso, a origem do frontend precisa constar em `CORS_ALLOWED_ORIGINS` no backend.

Endereços padrão:

- frontend: `http://localhost:5174`;
- backend: `http://localhost:8080`.

## Testes e verificações

### Backend

Na raiz do projeto:

```powershell
.\mvnw.cmd test
```

A última auditoria local registrou **146 testes de backend aprovados**. Uma parte dessa contagem correspondia ao teste de integração do `DemoDataSeeder`, mantido em arquivo não rastreado naquele workspace; portanto, um checkout contendo somente os arquivos versionados pode executar **145 testes**. O resultado deve ser confirmado no ambiente usado para a avaliação.

Não há relatório de cobertura percentual configurado, por isso este README não declara um índice de cobertura.

### Frontend

```powershell
cd frontend
npm run lint
npm run build
```

O frontend não possui atualmente uma suíte própria de testes automatizados. Os comandos acima verificam regras estáticas e a geração do pacote de produção; os principais fluxos também foram validados manualmente durante a homologação.

### Verificação de formatação do Git

```powershell
git diff --check
```

## Limitações conhecidas e próximas etapas

- implementar envio, armazenamento e gerenciamento de documentos do estágio (RF07);
- implementar análise institucional dos documentos (RF08);
- completar o gerenciamento de todos os perfis de usuário (RF03);
- ampliar as notificações por e-mail para prazos, pendências e alterações dos processos (RF10);
- disponibilizar a consulta de CEP diretamente nos fluxos pertinentes ao perfil Empresa/Supervisor;
- realizar revisão jurídica e operacional de privacidade e LGPD;
- fortalecer a gestão de segredos e a configuração por ambiente antes de uma implantação pública;
- acrescentar testes automatizados do frontend e relatório formal de cobertura;
- consolidar instruções operacionais de backup, restauração e migração para ambientes externos.

## Entrega acadêmica

- **Projeto:** PFC — GAEA;
- **data da entrega:** 09/11/2026;
- **branch de avaliação:** `entrega0911`.

Para avaliar a versão:

1. selecione a branch `entrega0911`;
2. configure um MySQL 8 e as variáveis de ambiente, sem adicionar segredos ao repositório;
3. aplique as migrations V1 a V7 em um banco compatível e execute o backend;
4. inicie o frontend pela porta 5174;
5. execute os testes e as verificações descritas neste README;
6. valide os fluxos com contas de teste dos três perfis, sem utilizar dados pessoais reais.

Credenciais de demonstração não são publicadas neste documento. Quando necessárias para a avaliação, devem ser entregues por canal separado e seguro.
