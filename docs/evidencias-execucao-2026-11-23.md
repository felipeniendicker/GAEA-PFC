# Evidências de execução — 23/11/2026

Este relatório registra somente resultados efetivamente observados no worktree
`gaea-v15-final`. Nenhum banco real, ambiente publicado ou serviço SMTP externo foi usado.

## Resumo verificável

| Verificação | Resultado |
|---|---|
| Maven `verify` | 240 testes, 0 falhas, 0 erros e 1 ignorado |
| Regra JaCoCo | Aprovada; mínimo de 50% de linhas aplicado no `verify` |
| Cobertura de linhas | 91,75% — 3.235 cobertas e 291 não cobertas |
| Cobertura de instruções | 90,71% — 15.347 cobertas e 1.572 não cobertas |
| Cobertura de branches | 71,21% — 799 cobertas e 323 não cobertas |
| Cobertura de métodos | 88,24% — 878 cobertos e 117 não cobertos |
| Cobertura de classes | 97,47% — 154 cobertas e 4 não cobertas |
| Migration V16 | Aplicada com sucesso em MySQL Community 8.0.44 descartável |
| Inicialização da aplicação | Concluída com Hibernate `validate` e healthcheck `UP` |
| Frontend | lint e build aprovados na auditoria atual |

O relatório HTML do JaCoCo é gerado localmente em `target/site/jacoco/index.html` e o XML
em `target/site/jacoco/jacoco.xml`. Esses artefatos são de build e não devem ser versionados.

## Validação MySQL 8 da V16

Foi criada uma instância MySQL Community 8.0.44 temporária em diretório e porta exclusivos.
O serviço `MySQL80` existente não foi consultado ou modificado. A aplicação foi iniciada com:

- schema vazio e exclusivo `gaea_migration_test`;
- Flyway habilitado, sem baseline automático e com `clean` desabilitado;
- `spring.jpa.hibernate.ddl-auto=validate`;
- bootstrap administrativo, seeder e e-mail desabilitados.

Resultados observados:

1. Flyway validou e aplicou V1–V16, todas com `success = 1`.
2. O schema terminou na versão 16.
3. Hibernate validou o mapeamento JPA contra o MySQL.
4. `/actuator/health` respondeu `UP`.
5. Foram confirmadas em `information_schema` as colunas `atualizado_em`,
   `responsavel_usuario_id`, `decisao`, `evidencia_execucao` e `versao`.
6. Foram confirmadas as duas constraints `CHECK`, a chave estrangeira do responsável e os
   dois índices adicionados pela V16.
7. A aplicação e o MySQL temporário foram encerrados normalmente e os diretórios temporários
   foram removidos.

O teste Testcontainers `MySqlFlywayIntegrationTests` foi atualizado para V16. Ele permanece o
único teste ignorado nesta máquina porque não há Docker disponível; a execução equivalente foi
realizada diretamente com o binário MySQL 8.0.44 instalado.

## Cobertura abaixo de 50% por classe

| Classe | Linhas | Avaliação |
|---|---:|---|
| `EmailConfig` | 18,18% | Criação condicional do adaptador SMTP; comportamento de negócio usa mocks |
| `EmailConfig$1` | 0% | Implementação anônima do SMTP real; exige servidor SMTP de teste ou Brevo autorizado |
| `GaeaApiApplication` | 33,33% | Classe de inicialização, sem regra de negócio |
| `InstituicaoAdminResponse` | 0% | DTO de mapeamento simples |
| `NotificacaoResponse` | 0% | DTO de mapeamento simples |
| `EnvioEmailAplicacao` | 0% | Interface/adaptador |
| `NotificacaoController` | 37,50% | Controller fino; o serviço de notificações possui testes |

O critério global de 50% foi superado. Novos testes foram concentrados em fluxos relevantes:
bootstrap e login administrativo, credenciais BCrypt e vínculos dos três perfis demonstrativos.

## Evidências dos quatro perfis

| Perfil | Evidência automatizada | Operações comprovadas |
|---|---|---|
| `ADMIN_GAEA` | `AdminBootstrapIntegrationTests`, `AdministracaoInstituicoesIntegrationTests`, `PrivacidadeAdminIntegrationTests` | bootstrap explícito, login, aprovação/rejeição institucional e atendimento de privacidade |
| `INSTITUICAO` | `SecurityIntegrationTests`, `GestaoAlunosIntegrationTests`, `RelatorioEstagioIntegrationTests` | login, isolamento institucional, alunos, processos, relatórios e auditoria |
| `ALUNO` | `ProfessionalDemoSeederIntegrationTests`, `SolicitacaoEstagioIntegrationTests` e testes de documentos/planos/horas | acesso demonstrativo, processo próprio, documentos, plano e carga horária |
| `EMPRESA` | `ProfessionalDemoSeederIntegrationTests`, `SupervisaoIntegrationTests` e testes de avaliação | acesso demonstrativo, verificação, vínculo confirmado, plano e horas |

Também foram comprovados JWT inválido/expirado, revogação no logout e redefinição, contas e
instituições inativas, manipulação de IDs, isolamento entre instituições e supervisão não
confirmada.

## Limites desta evidência

- Não houve teste real de entrega pelo Brevo.
- Não houve teste visual completo em navegador com um ambiente de homologação publicado.
- Exportação material, exclusão e anonimização LGPD permanecem aguardando decisões jurídicas e
  de retenção; somente o fluxo de solicitação, análise, decisão e evidência está implementado.
- A monografia ou catálogo oficial de RF/RNF não foi localizado; a matriz usa somente requisitos
  observáveis no código.
