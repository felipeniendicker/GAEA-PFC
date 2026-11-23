# Roteiro de homologação dos perfis

Use apenas um ambiente isolado, banco descartável e endereços reservados. Nunca habilite a carga
profissional em produção ou em banco que contenha dados reais.

## Provisionamento seguro

- `ADMIN_GAEA`: habilite uma única vez `ADMIN_BOOTSTRAP_ENABLED=true` e informe nome, e-mail,
  senha e confirmação por variáveis secretas. Após a criação, desabilite o bootstrap e remova a
  senha das variáveis.
- `INSTITUICAO`, `ALUNO` e `EMPRESA`: em demonstração autorizada, use o
  `ProfessionalDemoSeeder` com todas as confirmações e senhas fornecidas somente por variáveis
  de ambiente. O seeder permanece desabilitado por padrão.
- Alternativa manual: solicite e aprove uma instituição, crie o aluno pela conta institucional e
  cadastre o supervisor pelo fluxo público, usando Mailpit no ambiente local.

Nenhuma senha de homologação deve ser colocada neste documento, no Git ou em logs.

## Sequência segura para preparar os quatro acessos

Use um MySQL novo e exclusivo, sem qualquer dado real. As senhas indicadas abaixo são nomes de
variáveis; seus valores devem existir somente nos secrets do ambiente ou em um gerenciador de
senhas.

### Etapa A — carga demonstrativa, sem administrador

Inicie uma única réplica com o perfil `demo`, SMTP desabilitado e bootstrap administrativo
desabilitado:

```text
SPRING_PROFILES_ACTIVE=demo
EMAIL_ENABLED=false
ADMIN_BOOTSTRAP_ENABLED=false
PROFESSIONAL_DEMO_SEED_ENABLED=true
PROFESSIONAL_DEMO_EXPECTED_DATABASE=<nome exato do banco vazio>
PROFESSIONAL_DEMO_EXPECTED_ENVIRONMENT=<nome exato do ambiente>
PROFESSIONAL_DEMO_CONFIRMATION=GAEA_PROFESSIONAL_DEMO_ONLY
PROFESSIONAL_DEMO_PASSWORD_FELIPE=<secret exclusivo>
PROFESSIONAL_DEMO_PASSWORD_ALUNO=<secret exclusivo>
PROFESSIONAL_DEMO_PASSWORD_INSTITUICAO=<secret exclusivo>
PROFESSIONAL_DEMO_PASSWORD_SUPERVISOR=<secret exclusivo>
```

Após conferir as contagens, defina `PROFESSIONAL_DEMO_SEED_ENABLED=false`, reinicie e remova a
confirmação e as quatro senhas do ambiente. Não habilite o administrador na mesma inicialização:
o seeder recusa corretamente bases que já contenham usuários fora do domínio reservado.

### Etapa B — primeiro administrador

Com o seeder já desabilitado, faça uma única inicialização com:

```text
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_BOOTSTRAP_NAME=<nome do responsável pela homologação>
ADMIN_BOOTSTRAP_EMAIL=<e-mail reservado para homologação>
ADMIN_BOOTSTRAP_PASSWORD=<secret forte e exclusivo>
ADMIN_BOOTSTRAP_CONFIRMATION=GAEA_CREATE_INITIAL_ADMIN
```

Depois do primeiro login, defina `ADMIN_BOOTSTRAP_ENABLED=false`, reinicie e remova senha e
confirmação dos secrets. O bootstrap recusa criar um segundo administrador.

## Identidades disponíveis após a carga autorizada

| Perfil | Login de demonstração | Origem da senha |
|---|---|---|
| `ADMIN_GAEA` | valor de `ADMIN_BOOTSTRAP_EMAIL` | `ADMIN_BOOTSTRAP_PASSWORD`, removida após o bootstrap |
| `INSTITUICAO` | `mariana.albuquerque@professional-demo.gaea.example` | `PROFESSIONAL_DEMO_PASSWORD_INSTITUICAO` |
| `ALUNO` | `felipe.rafael.niendicker@professional-demo.gaea.example` | `PROFESSIONAL_DEMO_PASSWORD_FELIPE` |
| `EMPRESA` | `alice.queiroz@professional-demo.gaea.example` | `PROFESSIONAL_DEMO_PASSWORD_SUPERVISOR` |

Os endereços `professional-demo.gaea.example` são reservados e não recebem mensagens reais.
Mantenha `EMAIL_ENABLED=false` no dataset profissional. Para testar e-mail, use contas distintas
em um ambiente separado com Mailpit ou destinatários explicitamente autorizados.

## Testes manuais mínimos

1. Entre como ADMIN_GAEA; confirme que acessa apenas instituições e solicitações de privacidade,
   e recebe 403 em processos, documentos, alunos e relatórios.
2. Entre como cada instituição; confirme que alunos, processos, auditoria e relatórios não exibem
   registros de outra instituição mesmo alterando IDs nas URLs.
3. Entre como aluno; confirme acesso somente aos próprios processos e operações de criação.
4. Entre como supervisor sem vínculo; confirme listas vazias e 404 ao manipular IDs. Depois aceite
   um convite e valide apenas o processo confirmado.
5. Teste JWT ausente, adulterado, expirado e anterior ao logout/redefinição de senha.
6. Teste senha incorreta repetida até 429, sem comparar mensagens entre conta existente e ausente.
7. Teste cadastro com SMTP desabilitado; deve retornar 503 e não persistir conta incompleta.
8. Registre cada tipo de solicitação de privacidade; valide atribuição, decisão e evidência, sem
   executar exclusão real.
9. Envie PDF válido e arquivos inválidos; confirme download somente pelos participantes.
10. Exporte CSV com valores iniciados por `=`, `+`, `-` e `@`; confirme neutralização de fórmula.
