# Auditoria de segurança, autorização e LGPD

Data da revisão técnica: 23/11/2026. Referência inicial: `43fa109`.

Este documento registra evidências técnicas. Ele não é parecer jurídico e não declara
conformidade integral com a LGPD. Bases legais, prazos de retenção, tratamento de menores,
papéis de controlador/operador e canais formais do encarregado precisam ser aprovados pela
instituição e por assessoria jurídica.

## Resumo executivo

O isolamento de processos está centralizado em `ProcessoAutorizacaoService`: aluno acessa
somente o próprio processo, instituição somente processos de seu vínculo e supervisor somente
processos com vínculo confirmado e ativo. Documentos, planos, horas, históricos, pendências e
prazos reutilizam essa autorização por objeto. O perfil `ADMIN_GAEA` não recebe acesso a esses
recursos.

Foram endurecidos os fluxos de autenticação com confirmação de senha também no backend,
cadastro transacional dependente de SMTP, resposta de login sem cache, emissor e expiração JWT
configuráveis, logout com revogação da versão de credenciais e limitação de tentativas baseada
em HMAC. O token do frontend permanece somente em memória; não é gravado em `localStorage`.

O fluxo de privacidade passou a aceitar acesso, correção, exportação, exclusão elegível,
anonimização, revogação de consentimento e encerramento de conta. A administração pode assumir,
decidir e registrar evidência. Nenhuma exclusão ou anonimização é executada automaticamente.

## Achados e riscos

| Gravidade | Situação | Evidência ou tratamento |
|---|---|---|
| Alta | Atendimento LGPD antes limitado a dois tipos e sempre pendente | Corrigido pela V16 e endpoints administrativos específicos |
| Alta | Logout encerrava apenas o estado React; JWT continuava válido | Corrigido por `POST /api/auth/logout` e `credenciaisVersao` |
| Alta | Cadastro de supervisor podia permanecer sem e-mail de verificação quando SMTP falhava | Corrigido com verificação prévia e transação única |
| Alta | Não havia limitação de tentativas de login | Corrigido por identificador e origem protegidos com HMAC |
| Média | Limitador de login é local à instância | Pendente adotar armazenamento compartilhado antes de múltiplas réplicas |
| Média | Aceite de termos registra data, mas não versão do documento | Pendente definir e versionar juridicamente Termos e Política |
| Média | Auditoria armazena e-mail do ator sem política de retenção definida | Pendente política institucional de retenção e acesso |
| Média | PDFs em volume local dependem da segurança e backup do provedor | Pendente definir criptografia, backup, restauração e descarte |
| Média | ADMIN_GAEA não possui MFA | Melhoria futura recomendada antes de operação com dados reais |
| Média | V16 foi validada em H2/MySQL mode, não em MySQL real | Executar Testcontainers/MySQL descartável antes da publicação |
| Baixa | Tokens de ativação ficam no fragmento `#` da URL | Fragmentos não são enviados ao servidor, mas links devem continuar tratados como segredo |

## Inventário de dados pessoais

| Categoria | Dados | Finalidade técnica | Acesso | Retenção/base legal |
|---|---|---|---|---|
| Aluno | nome, e-mail, matrícula, instituição, senha BCrypt | conta e estágio | próprio aluno e instituição vinculada | definir juridicamente |
| Supervisor | nome, e-mail, verificação, vínculos | supervisão autorizada | próprio supervisor e participantes do processo | definir juridicamente |
| Instituição | CNPJ, nome, responsável, e-mail | aprovação e gestão acadêmica | ADMIN_GAEA e usuários vinculados conforme tela | definir juridicamente |
| Administrador | nome, e-mail, hash de senha, auditoria | aprovação e atendimento específico | bootstrap e administração mínima | definir juridicamente |
| Estágio | datas, empresa, aluno, status, histórico | acompanhamento acadêmico | autorização por objeto | provável obrigação acadêmica; validar |
| Documentos | PDF, nome, hash, tamanho, análise | comprovação do estágio | participantes autorizados | definir retenção e descarte |
| Autenticação | hashes de senha/token, expiração, origem/identificador em HMAC | segurança da conta | backend | retenção mínima necessária; definir prazo |
| Auditoria | ator, ação, recurso, data, instituição | responsabilização e segurança | instituição vinculada ou fluxo admin específico | definir prazo e minimização |
| Privacidade | tipo, descrição, titular, responsável, decisão, evidência | atendimento ao titular | titular e ADMIN_GAEA no módulo específico | definir prazo jurídico |

O sistema não deve tratar consentimento como base universal. Execução de contrato educacional,
obrigação legal/regulatória e legítimo interesse podem ser mais adequados em alguns fluxos;
essa classificação depende de decisão jurídica documentada.

## Matriz de autorização dos endpoints

`Objeto` significa que o serviço também verifica proprietário, vínculo confirmado ou instituição,
não apenas a role declarada no filtro.

| Método e rota | Perfil | Serviço/controle | Objeto/teste |
|---|---|---|---|
| POST `/api/auth/cadastro` | Público, somente EMPRESA no DTO | `UsuarioService`, verificação de e-mail | perfil bloqueado por validação |
| POST `/api/auth/login` | Público | BCrypt, limitador, conta/instituição ativa | resposta genérica |
| POST `/api/auth/logout` | Autenticado | revoga versão de credenciais | teste de JWT anterior |
| POST `/api/auth/senha/recuperacao` | Público | `RecuperacaoSenhaService` | enumeração protegida e limites |
| POST `/api/auth/senha/redefinicao` | Público com token | token hash, uso único, 15 min | testes válido/expirado/reutilizado |
| POST `/api/ativacoes/aluno` | Público com convite | `ConviteAlunoService` | token, e-mail e instituição |
| POST `/api/verificacoes-email/confirmar` | Público com token | `VerificacaoEmailService` | token hash e uso único |
| POST `/api/verificacoes-email/reenviar` | EMPRESA | `VerificacaoEmailService` | próprio principal |
| POST `/api/instituicoes/solicitacoes` | Público | limite, CNPJ/e-mail únicos, SMTP | testes administrativos |
| POST `/api/instituicoes/solicitacoes/verificar-email` | Público com token | fluxo institucional | token de uso único |
| POST `/api/instituicoes/ativacoes` | Público com token | fluxo institucional | conta aprovada e inativa |
| GET `/api/admin/instituicoes/dashboard` | ADMIN_GAEA | `SolicitacaoInstituicaoService` | sem acesso a processos |
| GET `/api/admin/instituicoes` | ADMIN_GAEA | lista administrativa | metadados institucionais |
| GET `/api/admin/instituicoes/solicitacoes` | ADMIN_GAEA | lista solicitações | autorização por role |
| PUT `/api/admin/instituicoes/solicitacoes/{id}/analise` | ADMIN_GAEA | transição e lock | idempotência testada |
| POST `/api/admin/instituicoes/solicitacoes/{id}/aprovar` | ADMIN_GAEA | transação, unicidade e convite | concorrência/duplicidade testadas |
| POST `/api/admin/instituicoes/solicitacoes/{id}/rejeitar` | ADMIN_GAEA | justificativa e auditoria | teste de autorização |
| GET `/api/admin/privacidade/solicitacoes` | ADMIN_GAEA | lista específica | não concede processos/documentos |
| PUT `/api/admin/privacidade/solicitacoes/{id}/analise` | ADMIN_GAEA | atribuição e lock | teste positivo/negativo |
| POST `/api/admin/privacidade/solicitacoes/{id}/concluir` | ADMIN_GAEA atribuído | decisão e evidência obrigatórias | teste de evidência |
| POST `/api/admin/privacidade/solicitacoes/{id}/rejeitar` | ADMIN_GAEA atribuído | justificativa e auditoria | transição controlada |
| GET/POST `/api/privacidade/solicitacoes` | ALUNO, INSTITUICAO, EMPRESA | próprios pedidos | filtro por `usuario.id` |
| GET `/api/dashboard/resumo` | ALUNO, INSTITUICAO, EMPRESA | `DashboardService` | consultas segregadas |
| GET/POST/PUT/PATCH `/api/instituicoes/alunos/**` | INSTITUICAO | `AlunoInstituicaoService` | instituição do principal |
| POST `/api/processos` | ALUNO | `ProcessoEstagioService` | principal define aluno/instituição |
| GET `/api/processos/meus` | ALUNO | lista própria | `aluno.id` |
| GET `/api/processos` | INSTITUICAO, EMPRESA | lista segregada | instituição ou supervisor confirmado |
| PUT `/api/processos/{id}/status` | INSTITUICAO | gestão institucional | Objeto |
| POST `/api/processos/{id}/reabrir` | INSTITUICAO | gestão institucional | Objeto |
| PUT `/api/processos/{id}/supervisor` | ALUNO | `SupervisaoService` | processo do aluno |
| GET `/api/processos/{id}/historico` | ALUNO, INSTITUICAO, EMPRESA | autorização central | Objeto |
| GET `/api/processos/{id}/historico-completo` | ALUNO, INSTITUICAO, EMPRESA | autorização central | Objeto |
| GET/POST/PUT `/api/processos/{id}/pendencias/**` | leitura participantes; escrita INSTITUICAO | serviço de pendências | Objeto |
| GET/POST/PUT `/api/processos/{id}/prazos/**` | leitura participantes; escrita INSTITUICAO | serviço de prazos | Objeto |
| GET/POST/PUT `/api/processos/{id}/documentos/**` | leitura participantes; envio ALUNO; análise INSTITUICAO | valida PDF e armazenamento | Objeto e IDs compostos |
| GET/POST/PUT `/api/processos/{id}/plano-atividades/**` | leitura participantes; criação ALUNO; avaliação EMPRESA | serviço de plano | Objeto |
| GET/POST/PUT `/api/processos/{id}/cargas-horarias/**` | leitura participantes; criação ALUNO; avaliação EMPRESA | serviço de horas | Objeto |
| GET/PUT `/api/notificacoes/**` | ALUNO, INSTITUICAO, EMPRESA | destinatário autenticado | processo ainda visível |
| GET `/api/supervisao/solicitacoes` | EMPRESA verificada | `SupervisaoService` | e-mail do principal |
| POST `/api/supervisao/solicitacoes/{id}/confirmar` | EMPRESA verificada | lock e vínculo | convite do e-mail |
| POST `/api/supervisao/solicitacoes/{id}/recusar` | EMPRESA verificada | justificativa e lock | convite do e-mail |
| GET `/api/relatorios/estagios` | INSTITUICAO | instituição do principal | segregado |
| GET `/api/relatorios/estagios/csv` | INSTITUICAO | mesma consulta, CSV protegido contra fórmula | segregado |
| GET `/api/auditoria` | INSTITUICAO | logs da própria instituição | segregado |
| GET `/api/cep/{cep}` | ALUNO, INSTITUICAO, EMPRESA | ViaCEP com timeout | sem dados acadêmicos |
| GET `/actuator/health` | Público | apenas health sem detalhes | configuração restritiva |

## Matriz funcional e de requisitos

Nenhuma monografia ou catálogo oficial de RF/RNF foi localizado nas worktrees pesquisadas.
Por isso, códigos oficiais não foram inventados. A vinculação abaixo usa módulos observáveis e
deve ser reconciliada quando o documento acadêmico for fornecido.

| Código oficial | Descrição observada | Implementação | Teste/evidência | Pendência | Prioridade |
|---|---|---|---|---|---|
| Não localizado | Autenticação dos quatro perfis | JWT, BCrypt, ativação, bootstrap | testes de segurança | MFA admin | Alta |
| Não localizado | Segregação entre instituições | autorização central e consultas por instituição | integrações de segurança | manter regressão | Crítica |
| Não localizado | Gestão de alunos | cadastro, convite, edição e situação | `GestaoAlunosIntegrationTests` | homologação visual | Média |
| Não localizado | Aprovação institucional | solicitação, e-mail, análise, ativação | testes administrativos | operação SMTP | Alta |
| Não localizado | Processo de estágio | fluxo, status, reabertura, histórico | testes de regras | matriz oficial | Alta |
| Não localizado | Documentos/planos/horas | serviços e telas integrados | testes de autorização | política de retenção | Alta |
| Não localizado | Relatórios | filtros e CSV | testes de instituição | homologação CSV | Média |
| Não localizado | Direitos do titular | solicitação e atendimento controlado | testes da V16 | execução real de exportação/anonimização | Alta |
| Não localizado | Disponibilidade/produção | Railway, healthcheck, volume | build e testes | teste MySQL real/restore | Alta |

## Brevo, SMTP e Supabase

O backend usa `JavaMailSender` e é compatível com o SMTP relay do Brevo sem dependência de SDK.
Configure host, porta, usuário, chave SMTP, remetente validado, autenticação e STARTTLS somente
nas variáveis do ambiente. Nenhuma chave deve ser versionada. Os testes usam mock/Mailpit e não
enviam mensagens externas. Não foi realizado teste real com Brevo nesta auditoria.

Não existe integração Supabase no código. O MySQL permanece como banco principal e documentos
usam armazenamento local configurável. Supabase Storage pode ser avaliado futuramente, mas exige
projeto separado para chaves de serviço apenas no backend, políticas de bucket, URLs assinadas,
retenção, residência de dados, custos e migração. Nenhuma dependência Supabase foi adicionada.

## Decisões pendentes

1. Aprovar bases legais e prazos de retenção por categoria.
2. Definir versão formal de Termos e Política e registrar essa versão nos novos aceites.
3. Definir quem pode concluir pedidos de exclusão/anonimização e qual confirmação reforçada usar.
4. Implementar exportação autenticada e execução transacional de anonimização somente após 1–3.
5. Validar V16 em MySQL descartável e planejar backup/rollback antes de produção.
6. Decidir MFA para `ADMIN_GAEA` e armazenamento compartilhado do rate limit se houver réplicas.
