# Roteiro de demonstração do PFC GAEA

Roteiro sugerido para uma demonstração de 12 a 15 minutos. Use apenas o ambiente isolado de
homologação e mantenha as quatro credenciais em um gerenciador de senhas, fora da apresentação,
do Git e dos logs.

## Preparação anterior à banca

1. Confirme `/actuator/health` com status `UP`.
2. Confirme que `PROFESSIONAL_DEMO_SEED_ENABLED=false` e `ADMIN_BOOTSTRAP_ENABLED=false`.
3. Teste previamente os quatro logins e abra uma janela anônima para cada perfil, se possível.
4. Deixe preparado o relatório JaCoCo em `target/site/jacoco/index.html`.
5. Não habilite SMTP real usando endereços `professional-demo.gaea.example`.

## Sequência principal

### 1. Contexto e segurança — 1 minuto

- Explique que o GAEA acompanha estágios acadêmicos com segregação por instituição e vínculo.
- Mostre o healthcheck sem detalhes internos.
- Informe que senhas são protegidas com BCrypt e o frontend não persiste JWT em armazenamento
  permanente do navegador.

### 2. Administrador GAEA — 2 minutos

- Entre como `ADMIN_GAEA`.
- Mostre o dashboard de solicitações institucionais e instituições existentes.
- Explique a aprovação com sigla e classificação fictícia administrativa.
- Abra as solicitações de privacidade, assuma uma análise e mostre decisão/evidência.
- Destaque que o administrador não possui acesso geral a processos e documentos acadêmicos.

### 3. Instituição — 3 minutos

- Entre com a conta institucional de demonstração.
- Mostre a sigla no cabeçalho e a Gestão de Alunos.
- Demonstre pesquisa, filtros, status, edição e convite sem revelar tokens na listagem.
- Abra os processos da própria instituição, pendências, prazos, relatórios e CSV.
- Explique que IDs de outra instituição são recusados pelo backend.

### 4. Aluno — 3 minutos

- Entre como Felipe usando o endereço reservado documentado.
- Mostre dashboard e somente os processos do próprio aluno.
- Abra um processo e apresente histórico, plano de atividades, carga horária, documentos,
  notificações e solicitação de privacidade.
- Não faça upload de documento real; use somente PDF fictício sem validade.

### 5. Supervisor/empresa — 2 minutos

- Entre como supervisor fictício.
- Mostre solicitações de supervisão e processos com vínculo confirmado.
- Demonstre a avaliação de plano ou carga horária em um registro preparado.
- Explique que um supervisor novo ou sem vínculo não consegue consultar dados do aluno.

### 6. Evidências de qualidade — 2 minutos

- Mostre o resumo dos 240 testes aprovados.
- Abra o relatório JaCoCo e destaque 91,75% de linhas, acima do mínimo de 50%.
- Mostre a evidência de V1–V16 em MySQL 8.0.44 e o healthcheck `UP`.
- Diferencie funcionalidades comprovadas de pendências jurídicas ou operacionais.

## Demonstrações negativas rápidas

- Tentar abrir uma rota protegida sem JWT: esperar `401`.
- Tentar usar perfil sem autorização: esperar `403`.
- Explicar que recursos alheios retornam resposta sem revelar dados.
- Mostrar que logout revoga o token atual.

## Plano de contingência

Se SMTP ou Internet estiver indisponível, use contas já provisionadas e mostre os testes
automatizados dos fluxos de verificação e recuperação. Não altere senhas ou banco durante a
banca. Se o ambiente publicado falhar, apresente o build local, o relatório JaCoCo e o relatório
de evidências, sem improvisar operações destrutivas.
