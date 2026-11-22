# Base profissional de demonstração

O `ProfessionalDemoSeeder` cria uma base acadêmica coerente e idempotente para um
ambiente dedicado de demonstração. A carga fica desativada por padrão e não deve ser
usada em produção, homologação com dados reais ou banco compartilhado.

> A Universidade de Mogi das Cruzes (UMC) é a única instituição real citada. Sua
> presença compõe apenas um cenário acadêmico fictício e não indica parceria,
> autorização, endosso nem vínculo oficial com o GAEA. Todos os demais nomes,
> CNPJs, endereços, documentos e acontecimentos são sintéticos.

## Conteúdo da carga

- 3 instituições e 6 responsáveis institucionais;
- 24 alunos, 12 supervisores e 8 empresas fictícias;
- 36 processos: 9 pendentes, 9 em análise, 9 aprovados e 9 reprovados;
- 30 planos, 90 atividades planejadas e 90 registros de carga horária;
- 18 pendências, 24 prazos, 24 PDFs fictícios e 72 notificações;
- históricos de status e supervisão, auditoria e 6 solicitações de privacidade.

Os PDFs recebem a marca `GAEA - DOCUMENTO FICTICIO - SEM VALIDADE`. Os e-mails usam
exclusivamente o subdomínio reservado `professional-demo.gaea.example`, que não é
entregável na Internet.

O esquema atual não possui coluna de curso. Por isso, o curso de Felipe é representado
na matrícula `UMC-SI-2026-001`, em auditoria e no plano de atividades, sem migration
nova ou alteração das migrations V1–V12.

## Catálogo

### Instituições e responsáveis

| Instituição | Responsáveis |
|---|---|
| Universidade de Mogi das Cruzes (UMC) | Mariana Souto Albuquerque; Ricardo Nogueira Leal |
| Instituto Universitário Vale das Araucárias | Beatriz Fontes Guimarães; Leandro Pires Macedo |
| Faculdade Horizonte do Litoral | Renata Cavalcanti Moura; Sérgio Almeida Vasconcelos |

### Alunos

| Instituição | Alunos |
|---|---|
| UMC | Felipe Rafael Niendicker; Ana Clara Mendes Ribeiro; Bruno Henrique Valença; Camila Torres de Almeida; Daniel Augusto Ferreira; Eduarda Martins Siqueira; Gabriel Nunes Barros; Helena Costa Figueiredo |
| Instituto Universitário Vale das Araucárias | Isabela Rocha Monteiro; João Pedro Cardoso Lima; Larissa Gomes Peixoto; Marcos Vinícius Azevedo; Natália Freitas Campos; Otávio Moreira Santos; Paula Beatriz Rezende; Rafael Castro Diniz |
| Faculdade Horizonte do Litoral | Sabrina Lopes Amaral; Tiago Henrique Paiva; Valentina Moraes Teixeira; William Alves Pimenta; Yasmin Correia Bastos; Caio Eduardo Tavares; Luana Prado Carvalho; Mateus Ribeiro Neves |

### Empresas e supervisores

| Empresa fictícia | Supervisores |
|---|---|
| NexaVerde Sistemas Ltda. | Alice Monteiro Queiroz; André Luiz Portela |
| Ponte Clara Tecnologia Ltda. | Carolina Esteves Braga; Diego Fernandes Malta |
| Órbita Sul Dados Ltda. | Fernanda Ribeiro Luz; Gustavo Henrique Prado |
| Lumina Serra Software Ltda. | Juliana Castro Menezes; Lucas Pacheco Duarte |
| Cais do Vale Inovação Ltda. | Márcia Helena Couto |
| Horizonte Modular Tecnologia Ltda. | Pedro Augusto Salgado |
| Jatobá Cloud Serviços Ltda. | Roberta Vieira Nascimento |
| Brisa Norte Analytics Ltda. | Vinícius Ramos Toledo |

### Processos propostos

| # | Instituição | Aluno | Empresa | Status |
|---:|---|---|---|---|
| 1 | UMC | Felipe Rafael Niendicker | NexaVerde Sistemas Ltda. | PENDENTE |
| 2 | UMC | Ana Clara Mendes Ribeiro | Ponte Clara Tecnologia Ltda. | EM_ANALISE |
| 3 | UMC | Bruno Henrique Valença | Órbita Sul Dados Ltda. | APROVADO |
| 4 | UMC | Camila Torres de Almeida | Lumina Serra Software Ltda. | REPROVADO |
| 5 | UMC | Daniel Augusto Ferreira | Cais do Vale Inovação Ltda. | PENDENTE |
| 6 | UMC | Eduarda Martins Siqueira | Horizonte Modular Tecnologia Ltda. | EM_ANALISE |
| 7 | UMC | Gabriel Nunes Barros | Jatobá Cloud Serviços Ltda. | APROVADO |
| 8 | UMC | Helena Costa Figueiredo | Brisa Norte Analytics Ltda. | REPROVADO |
| 9 | UMC | Felipe Rafael Niendicker | NexaVerde Sistemas Ltda. | PENDENTE |
| 10 | UMC | Ana Clara Mendes Ribeiro | Ponte Clara Tecnologia Ltda. | EM_ANALISE |
| 11 | UMC | Bruno Henrique Valença | Órbita Sul Dados Ltda. | APROVADO |
| 12 | UMC | Camila Torres de Almeida | Lumina Serra Software Ltda. | REPROVADO |
| 13 | Instituto Universitário Vale das Araucárias | Isabela Rocha Monteiro | Cais do Vale Inovação Ltda. | PENDENTE |
| 14 | Instituto Universitário Vale das Araucárias | João Pedro Cardoso Lima | Horizonte Modular Tecnologia Ltda. | EM_ANALISE |
| 15 | Instituto Universitário Vale das Araucárias | Larissa Gomes Peixoto | Jatobá Cloud Serviços Ltda. | APROVADO |
| 16 | Instituto Universitário Vale das Araucárias | Marcos Vinícius Azevedo | Brisa Norte Analytics Ltda. | REPROVADO |
| 17 | Instituto Universitário Vale das Araucárias | Natália Freitas Campos | NexaVerde Sistemas Ltda. | PENDENTE |
| 18 | Instituto Universitário Vale das Araucárias | Otávio Moreira Santos | Ponte Clara Tecnologia Ltda. | EM_ANALISE |
| 19 | Instituto Universitário Vale das Araucárias | Paula Beatriz Rezende | Órbita Sul Dados Ltda. | APROVADO |
| 20 | Instituto Universitário Vale das Araucárias | Rafael Castro Diniz | Lumina Serra Software Ltda. | REPROVADO |
| 21 | Instituto Universitário Vale das Araucárias | Isabela Rocha Monteiro | Cais do Vale Inovação Ltda. | PENDENTE |
| 22 | Instituto Universitário Vale das Araucárias | João Pedro Cardoso Lima | Horizonte Modular Tecnologia Ltda. | EM_ANALISE |
| 23 | Instituto Universitário Vale das Araucárias | Larissa Gomes Peixoto | Jatobá Cloud Serviços Ltda. | APROVADO |
| 24 | Instituto Universitário Vale das Araucárias | Marcos Vinícius Azevedo | Brisa Norte Analytics Ltda. | REPROVADO |
| 25 | Faculdade Horizonte do Litoral | Sabrina Lopes Amaral | NexaVerde Sistemas Ltda. | PENDENTE |
| 26 | Faculdade Horizonte do Litoral | Tiago Henrique Paiva | Ponte Clara Tecnologia Ltda. | EM_ANALISE |
| 27 | Faculdade Horizonte do Litoral | Valentina Moraes Teixeira | Órbita Sul Dados Ltda. | APROVADO |
| 28 | Faculdade Horizonte do Litoral | William Alves Pimenta | Lumina Serra Software Ltda. | REPROVADO |
| 29 | Faculdade Horizonte do Litoral | Yasmin Correia Bastos | Cais do Vale Inovação Ltda. | PENDENTE |
| 30 | Faculdade Horizonte do Litoral | Caio Eduardo Tavares | Horizonte Modular Tecnologia Ltda. | EM_ANALISE |
| 31 | Faculdade Horizonte do Litoral | Luana Prado Carvalho | Jatobá Cloud Serviços Ltda. | APROVADO |
| 32 | Faculdade Horizonte do Litoral | Mateus Ribeiro Neves | Brisa Norte Analytics Ltda. | REPROVADO |
| 33 | Faculdade Horizonte do Litoral | Sabrina Lopes Amaral | NexaVerde Sistemas Ltda. | PENDENTE |
| 34 | Faculdade Horizonte do Litoral | Tiago Henrique Paiva | Ponte Clara Tecnologia Ltda. | EM_ANALISE |
| 35 | Faculdade Horizonte do Litoral | Valentina Moraes Teixeira | Órbita Sul Dados Ltda. | APROVADO |
| 36 | Faculdade Horizonte do Litoral | William Alves Pimenta | Lumina Serra Software Ltda. | REPROVADO |

## Ativação segura no Railway

1. Crie um ambiente Railway separado e claramente nomeado, por exemplo
   `demonstracao`. Use um MySQL novo, vazio e exclusivo; não restaure o backup SQL.
2. Anexe um volume persistente ao backend e mantenha
   `DOCUMENTOS_STORAGE_DIRETORIO` apontando para o ponto de montagem.
3. Faça um backup recuperável do banco, confirme que não existem dados reais e
   aplique normalmente as migrations V1–V12 com `FLYWAY_ENABLED=true`.
4. Defina temporariamente as variáveis abaixo no backend. As quatro senhas devem ser
   fortes, diferentes de credenciais reais e armazenadas apenas nos secrets do Railway.

```text
SPRING_PROFILES_ACTIVE=demo
EMAIL_ENABLED=false
PROFESSIONAL_DEMO_SEED_ENABLED=true
PROFESSIONAL_DEMO_EXPECTED_DATABASE=<nome exato de MYSQLDATABASE>
PROFESSIONAL_DEMO_EXPECTED_ENVIRONMENT=<nome exato exibido por RAILWAY_ENVIRONMENT_NAME>
PROFESSIONAL_DEMO_CONFIRMATION=GAEA_PROFESSIONAL_DEMO_ONLY
PROFESSIONAL_DEMO_REFERENCE_DATE=2026-11-21
PROFESSIONAL_DEMO_PASSWORD_FELIPE=<secret forte>
PROFESSIONAL_DEMO_PASSWORD_ALUNO=<secret forte>
PROFESSIONAL_DEMO_PASSWORD_INSTITUICAO=<secret forte>
PROFESSIONAL_DEMO_PASSWORD_SUPERVISOR=<secret forte>
```

5. Inicie uma única réplica do backend. A aplicação recusa a carga se o perfil,
   ambiente, banco, confirmação ou bloqueio de e-mail não coincidirem, e também
   recusa bancos com usuários, processos ou instituições alheios ao conjunto.
6. Verifique as contagens acima e autentique Felipe com
   `felipe.rafael.niendicker@professional-demo.gaea.example` e o valor do secret
   `PROFESSIONAL_DEMO_PASSWORD_FELIPE`.
7. Imediatamente após a validação, altere
   `PROFESSIONAL_DEMO_SEED_ENABLED=false`, remova as quatro variáveis de senha e a
   frase de confirmação, e faça novo deploy do backend. Mantenha
   `EMAIL_ENABLED=false` nesse ambiente.

Executar novamente com as mesmas referências não duplica registros. Ainda assim, a
idempotência não substitui as travas: a carga não deve permanecer habilitada nem ser
executada com mais de uma réplica simultânea.
