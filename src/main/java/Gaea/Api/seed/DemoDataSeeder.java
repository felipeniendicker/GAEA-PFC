package Gaea.Api.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String EMAIL_DOMAIN = "@demo.gaea.example";
    private static final String EMPRESA_PREFIXO = "Empresa Demonstracao GAEA ";
    private static final String PENDENCIA_PREFIXO = "Pendencia demonstrativa GAEA ";
    private static final String PRAZO_PREFIXO = "Prazo demonstrativo GAEA ";
    private static final LocalDateTime INSTANTE_BASE = LocalDateTime.of(2026, 10, 1, 8, 0);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String bancoEsperado;
    private final String senhaInstituicao;
    private final String senhaAluno;
    private final String senhaSupervisor;

    public DemoDataSeeder(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo-seed.expected-database}") String bancoEsperado,
            @Value("${app.demo-seed.password.instituicao}") String senhaInstituicao,
            @Value("${app.demo-seed.password.aluno}") String senhaAluno,
            @Value("${app.demo-seed.password.supervisor}") String senhaSupervisor) {

        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.bancoEsperado = exigirTexto(bancoEsperado, "Banco esperado");
        this.senhaInstituicao = exigirSenha(senhaInstituicao, "institucional");
        this.senhaAluno = exigirSenha(senhaAluno, "de aluno");
        this.senhaSupervisor = exigirSenha(senhaSupervisor, "de supervisor");
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validarBancoAlvo();
        LegadoSnapshot legadoAntes = capturarLegado();

        String hashInstituicao = passwordEncoder.encode(senhaInstituicao);
        String hashAluno = passwordEncoder.encode(senhaAluno);
        String hashSupervisor = passwordEncoder.encode(senhaSupervisor);

        List<InstituicaoDemo> instituicoes = criarInstituicoes(hashInstituicao);
        List<UsuarioDemo> alunos = criarAlunos(instituicoes, hashAluno);
        List<UsuarioDemo> supervisores = criarSupervisores(hashSupervisor);
        List<ProcessoDemo> processos = criarProcessos(instituicoes, alunos, supervisores);
        criarPendencias(processos);
        criarPrazos(processos);

        validarResultado(legadoAntes);
        LOGGER.info("Carga demonstrativa GAEA concluida e validada no banco {}", bancoEsperado);
    }

    private List<InstituicaoDemo> criarInstituicoes(String hashSenha) {
        List<InstituicaoDemo> resultado = new ArrayList<>();
        for (int numero = 1; numero <= 3; numero++) {
            int indiceInstituicao = numero;
            String nome = "Instituicao Demonstracao GAEA " + indiceInstituicao;
            String cnpj = gerarCnpj(91_000_000_000L + indiceInstituicao);
            long instituicaoId = encontrarId(
                    "select id from instituicao where cnpj = ?", cnpj,
                    () -> inserirRetornandoId(
                            "insert into instituicao (nome, cnpj, ativa, criada_em) values (?, ?, ?, ?)",
                            nome, cnpj, true, INSTANTE_BASE.plusMinutes(indiceInstituicao)));
            exigirIgual(nome,
                    jdbc.queryForObject("select nome from instituicao where id = ?", String.class, instituicaoId),
                    "Instituicao demonstrativa conflitante");

            String email = String.format(Locale.ROOT,
                    "responsavel%02d%s", indiceInstituicao, EMAIL_DOMAIN);
            long responsavelId = garantirUsuario(
                    "Responsavel Demonstracao GAEA " + indiceInstituicao, email, hashSenha,
                    "INSTITUICAO", instituicaoId, null, null);
            registrarAuditoriaSeAusente(
                    email, "CARGA_DEMONSTRACAO_INSTITUICAO",
                    "Instituicao demonstrativa " + indiceInstituicao, instituicaoId,
                    INSTANTE_BASE.plusMinutes(indiceInstituicao));
            resultado.add(new InstituicaoDemo(
                    indiceInstituicao, instituicaoId, responsavelId, email));
        }
        return resultado;
    }

    private List<UsuarioDemo> criarAlunos(List<InstituicaoDemo> instituicoes, String hashSenha) {
        List<UsuarioDemo> alunos = new ArrayList<>();
        for (InstituicaoDemo instituicao : instituicoes) {
            for (int local = 1; local <= 40; local++) {
                int global = (instituicao.numero() - 1) * 40 + local;
                String email = String.format(Locale.ROOT,
                        "aluno.i%02d.%03d%s", instituicao.numero(), local, EMAIL_DOMAIN);
                String matricula = String.format(Locale.ROOT,
                        "GAEA-DEMO-I%02d-A%03d", instituicao.numero(), local);
                long id = garantirUsuario(
                        String.format(Locale.ROOT, "Aluno Demonstracao GAEA %03d", global),
                        email, hashSenha, "ALUNO", instituicao.id(), matricula, null);
                registrarAuditoriaSeAusente(
                        instituicao.responsavelEmail(), "CARGA_DEMONSTRACAO_USUARIO",
                        "Aluno demonstrativo " + matricula, instituicao.id(),
                        INSTANTE_BASE.plusMinutes(10L + global));
                alunos.add(new UsuarioDemo(global, id, email, instituicao.id()));
            }
        }
        return alunos;
    }

    private List<UsuarioDemo> criarSupervisores(String hashSenha) {
        List<UsuarioDemo> supervisores = new ArrayList<>();
        for (int numero = 1; numero <= 25; numero++) {
            String email = String.format(Locale.ROOT, "supervisor%03d%s", numero, EMAIL_DOMAIN);
            long id = garantirUsuario(
                    String.format(Locale.ROOT, "Supervisor Demonstracao GAEA %03d", numero),
                    email, hashSenha, "EMPRESA", null, null,
                    INSTANTE_BASE.plusMinutes(200L + numero));
            supervisores.add(new UsuarioDemo(numero, id, email, null));
        }
        return supervisores;
    }

    private List<ProcessoDemo> criarProcessos(
            List<InstituicaoDemo> instituicoes,
            List<UsuarioDemo> alunos,
            List<UsuarioDemo> supervisores) {

        List<ProcessoDemo> processos = new ArrayList<>();
        int global = 0;
        for (InstituicaoDemo instituicao : instituicoes) {
            for (int local = 1; local <= 60; local++) {
                global++;
                int indiceGlobal = global;
                UsuarioDemo aluno = alunos.get((instituicao.numero() - 1) * 40 + (local - 1) % 40);
                UsuarioDemo supervisor = supervisores.get((indiceGlobal - 1) % supervisores.size());
                String status = statusDoProcesso(local);
                String empresa = String.format(Locale.ROOT,
                        "%sI%02d-P%03d", EMPRESA_PREFIXO, instituicao.numero(), local);
                LocalDate inicio = LocalDate.of(2026, 11, 10).plusDays(indiceGlobal);
                LocalDate fim = inicio.plusMonths(6);
                long processoId = encontrarProcesso(aluno.email(), empresa,
                        () -> inserirRetornandoId("""
                                insert into processo_estagio
                                (nome_aluno, email_aluno, nome_empresa, cnpj_empresa,
                                 cep_empresa, endereco_empresa, aluno_usuario_id, instituicao_id,
                                 data_inicio, data_fim, status, versao)
                                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                                """,
                                nomeUsuario(aluno.id()), aluno.email(), empresa,
                                gerarCnpj(81_000_000_000L + indiceGlobal), "00000000",
                                "Endereco ficticio para demonstracao GAEA " + indiceGlobal,
                                aluno.id(), instituicao.id(), inicio, fim, status));

                LocalDateTime criadoEm = INSTANTE_BASE.plusDays(indiceGlobal).plusHours(1);
                registrarAuditoriaSeAusente(
                        aluno.email(), "SOLICITACAO_ESTAGIO",
                        "Processo de estagio " + processoId, instituicao.id(), criadoEm);

                boolean confirmado = !"PENDENTE".equals(status);
                long vinculoId = garantirVinculo(processoId, supervisor, confirmado, criadoEm);
                garantirHistoricoSupervisao(
                        vinculoId, null, "PENDENTE", aluno.email(), criadoEm.plusMinutes(1));
                if (confirmado) {
                    garantirHistoricoSupervisao(
                            vinculoId, "PENDENTE", "CONFIRMADO", supervisor.email(),
                            criadoEm.plusMinutes(2));
                    registrarAuditoriaSeAusente(
                            supervisor.email(), "CONFIRMACAO_SUPERVISAO",
                            "Vinculo " + vinculoId + " do processo " + processoId,
                            instituicao.id(), criadoEm.plusMinutes(2));
                    garantirTransicoes(processoId, status, instituicao, criadoEm.plusMinutes(3));
                }
                processos.add(new ProcessoDemo(
                        indiceGlobal, processoId, instituicao, status));
            }
        }
        return processos;
    }

    private long garantirVinculo(
            long processoId, UsuarioDemo supervisor, boolean confirmado, LocalDateTime criadoEm) {

        List<Long> existentes = jdbc.queryForList(
                "select id from vinculo_supervisor_processo where processo_id = ? and ativo = true",
                Long.class, processoId);
        if (existentes.size() > 1) {
            throw new IllegalStateException("Mais de um vinculo ativo no processo " + processoId);
        }
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        return inserirRetornandoId("""
                insert into vinculo_supervisor_processo
                (processo_id, email_convidado, supervisor_usuario_id, situacao,
                 justificativa_recusa, criado_em, respondido_em, versao, ativo)
                values (?, ?, ?, ?, null, ?, ?, 0, true)
                """,
                processoId, supervisor.email(), confirmado ? supervisor.id() : null,
                confirmado ? "CONFIRMADO" : "PENDENTE", criadoEm,
                confirmado ? criadoEm.plusMinutes(2) : null);
    }

    private void garantirTransicoes(
            long processoId, String statusFinal, InstituicaoDemo instituicao, LocalDateTime inicio) {

        garantirHistoricoStatus(processoId, "PENDENTE", "EM_ANALISE",
                instituicao.responsavelEmail(), inicio);
        registrarAuditoriaSeAusente(
                instituicao.responsavelEmail(), "ALTERACAO_STATUS",
                "Processo " + processoId + ": PENDENTE -> EM_ANALISE",
                instituicao.id(), inicio);
        if ("APROVADO".equals(statusFinal) || "REPROVADO".equals(statusFinal)) {
            garantirHistoricoStatus(processoId, "EM_ANALISE", statusFinal,
                    instituicao.responsavelEmail(), inicio.plusMinutes(1));
            registrarAuditoriaSeAusente(
                    instituicao.responsavelEmail(), "ALTERACAO_STATUS",
                    "Processo " + processoId + ": EM_ANALISE -> " + statusFinal,
                    instituicao.id(), inicio.plusMinutes(1));
        }
    }

    private void criarPendencias(List<ProcessoDemo> processos) {
        List<ProcessoDemo> elegiveis = processos.stream()
                .filter(processo -> !"PENDENTE".equals(processo.status()))
                .limit(60)
                .toList();
        for (int indice = 0; indice < elegiveis.size(); indice++) {
            ProcessoDemo processo = elegiveis.get(indice);
            String descricao = PENDENCIA_PREFIXO + String.format(Locale.ROOT, "%03d", indice + 1);
            boolean resolvida = indice < 30;
            LocalDateTime criadaEm = INSTANTE_BASE.plusDays(220).plusMinutes(indice);
            long id = encontrarId(
                    "select id from pendencia_processo where processo_id = ? and descricao = ?",
                    processo.id(), descricao,
                    () -> inserirRetornandoId("""
                            insert into pendencia_processo
                            (processo_id, descricao, situacao, criada_em, resolvida_em,
                             responsavel_registro, versao)
                            values (?, ?, ?, ?, ?, ?, 0)
                            """,
                            processo.id(), descricao, resolvida ? "RESOLVIDA" : "ABERTA",
                            criadaEm, resolvida ? criadaEm.plusDays(2) : null,
                            processo.instituicao().responsavelEmail()));
            registrarAuditoriaSeAusente(
                    processo.instituicao().responsavelEmail(), "REGISTRO_PENDENCIA",
                    "Pendencia " + id + " do processo " + processo.id(),
                    processo.instituicao().id(), criadaEm);
            if (resolvida) {
                registrarAuditoriaSeAusente(
                        processo.instituicao().responsavelEmail(), "RESOLUCAO_PENDENCIA",
                        "Pendencia " + id + " do processo " + processo.id(),
                        processo.instituicao().id(), criadaEm.plusDays(2));
            }
        }
    }

    private void criarPrazos(List<ProcessoDemo> processos) {
        for (int indice = 0; indice < 80; indice++) {
            ProcessoDemo processo = processos.get(indice);
            String descricao = PRAZO_PREFIXO + String.format(Locale.ROOT, "%03d", indice + 1);
            LocalDateTime criadaEm = INSTANTE_BASE.plusDays(230).plusMinutes(indice);
            LocalDate vencimento = indice < 40
                    ? LocalDate.of(2026, 11, 1).minusDays(indice)
                    : LocalDate.of(2026, 11, 15).plusDays(indice - 40L);
            long id = encontrarId(
                    "select id from prazo_processo where processo_id = ? and descricao = ?",
                    processo.id(), descricao,
                    () -> inserirRetornandoId("""
                            insert into prazo_processo
                            (processo_id, descricao, data_vencimento, criada_em, atualizada_em,
                             responsavel_registro, versao)
                            values (?, ?, ?, ?, ?, ?, 0)
                            """,
                            processo.id(), descricao, vencimento, criadaEm, criadaEm,
                            processo.instituicao().responsavelEmail()));
            registrarAuditoriaSeAusente(
                    processo.instituicao().responsavelEmail(), "REGISTRO_PRAZO",
                    "Prazo " + id + " do processo " + processo.id(),
                    processo.instituicao().id(), criadaEm);
        }
    }

    private long garantirUsuario(
            String nome, String email, String hashSenha, String perfil,
            Long instituicaoId, String matricula, LocalDateTime emailVerificadoEm) {

        List<Long> ids = jdbc.queryForList(
                "select id from usuario where lower(trim(email)) = ?", Long.class,
                email.toLowerCase(Locale.ROOT));
        if (!ids.isEmpty()) {
            if (ids.size() != 1) {
                throw new IllegalStateException("E-mail demonstrativo duplicado: " + email);
            }
            String perfilExistente = jdbc.queryForObject(
                    "select perfil from usuario where id = ?", String.class, ids.get(0));
            exigirIgual(perfil, perfilExistente, "Perfil demonstrativo conflitante");
            return ids.get(0);
        }
        return inserirRetornandoId("""
                insert into usuario
                (nome, email, senha, perfil, instituicao_id, matricula, ativo, ativado,
                 termos_aceitos, termos_aceitos_em, email_verificado_em)
                values (?, ?, ?, ?, ?, ?, true, true, true, ?, ?)
                """,
                nome, email, hashSenha, perfil, instituicaoId, matricula,
                INSTANTE_BASE, emailVerificadoEm);
    }

    private void garantirHistoricoStatus(
            long processoId, String anterior, String novo,
            String responsavel, LocalDateTime dataHora) {

        Integer total = jdbc.queryForObject("""
                select count(*) from historico_status_processo
                where processo_id = ? and status_anterior = ? and status_novo = ?
                """, Integer.class, processoId, anterior, novo);
        if (total != null && total == 0) {
            jdbc.update("""
                    insert into historico_status_processo
                    (processo_id, status_anterior, status_novo, usuario_responsavel,
                     data_hora, justificativa, tipo_operacao)
                    values (?, ?, ?, ?, ?, null, 'ALTERACAO_NORMAL')
                    """, processoId, anterior, novo, responsavel, dataHora);
        }
    }

    private void garantirHistoricoSupervisao(
            long vinculoId, String anterior, String novo,
            String responsavel, LocalDateTime dataHora) {

        Integer total = jdbc.queryForObject("""
                select count(*) from historico_vinculo_supervisor
                where vinculo_id = ? and situacao_nova = ?
                """, Integer.class, vinculoId, novo);
        if (total != null && total == 0) {
            jdbc.update("""
                    insert into historico_vinculo_supervisor
                    (vinculo_id, situacao_anterior, situacao_nova,
                     usuario_responsavel, justificativa, data_hora)
                    values (?, ?, ?, ?, null, ?)
                    """, vinculoId, anterior, novo, responsavel, dataHora);
        }
    }

    private void registrarAuditoriaSeAusente(
            String usuario, String acao, String recurso,
            long instituicaoId, LocalDateTime dataHora) {

        Integer total = jdbc.queryForObject("""
                select count(*) from log_auditoria
                where usuario = ? and acao = ? and recurso = ? and instituicao_id = ?
                """, Integer.class, usuario, acao, recurso, instituicaoId);
        if (total != null && total == 0) {
            jdbc.update("""
                    insert into log_auditoria
                    (usuario, acao, recurso, data_hora, instituicao_id)
                    values (?, ?, ?, ?, ?)
                    """, usuario, acao, recurso, dataHora, instituicaoId);
        }
    }

    private long encontrarProcesso(String emailAluno, String empresa, IdSupplier criacao) {
        List<Long> ids = jdbc.queryForList("""
                select id from processo_estagio
                where email_aluno = ? and nome_empresa = ?
                """, Long.class, emailAluno, empresa);
        if (ids.size() > 1) {
            throw new IllegalStateException("Processo demonstrativo duplicado: " + empresa);
        }
        return ids.isEmpty() ? criacao.get() : ids.get(0);
    }

    private long encontrarId(String sql, Object parametro, IdSupplier criacao) {
        List<Long> ids = jdbc.queryForList(sql, Long.class, parametro);
        if (ids.size() > 1) {
            throw new IllegalStateException("Chave demonstrativa duplicada");
        }
        return ids.isEmpty() ? criacao.get() : ids.get(0);
    }

    private long encontrarId(
            String sql, Object parametro1, Object parametro2, IdSupplier criacao) {
        List<Long> ids = jdbc.queryForList(sql, Long.class, parametro1, parametro2);
        if (ids.size() > 1) {
            throw new IllegalStateException("Chave demonstrativa duplicada");
        }
        return ids.isEmpty() ? criacao.get() : ids.get(0);
    }

    private long inserirRetornandoId(String sql, Object... parametros) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);
            for (int indice = 0; indice < parametros.length; indice++) {
                statement.setObject(indice + 1, parametros[indice]);
            }
            return statement;
        }, keyHolder);
        Number chave = keyHolder.getKey();
        if (chave == null) {
            throw new IllegalStateException("O banco nao retornou a chave gerada");
        }
        return chave.longValue();
    }

    private String nomeUsuario(long usuarioId) {
        return jdbc.queryForObject(
                "select nome from usuario where id = ?", String.class, usuarioId);
    }

    private void validarBancoAlvo() {
        String atual = jdbc.execute((ConnectionCallback<String>) connection -> connection.getCatalog());
        if (atual == null || !atual.equalsIgnoreCase(bancoEsperado)) {
            throw new IllegalStateException(
                    "Carga demonstrativa recusada. Banco atual diferente do banco autorizado.");
        }
    }

    private LegadoSnapshot capturarLegado() {
        return new LegadoSnapshot(
                contar("select count(*) from usuario where email not like ?", "%" + EMAIL_DOMAIN),
                contar("select count(*) from processo_estagio where nome_empresa not like ?", EMPRESA_PREFIXO + "%"),
                contar("""
                        select count(*) from historico_status_processo h
                        join processo_estagio p on p.id = h.processo_id
                        where p.nome_empresa not like ?
                        """, EMPRESA_PREFIXO + "%"),
                contar("select count(*) from log_auditoria where instituicao_id is null"));
    }

    private void validarResultado(LegadoSnapshot legadoAntes) {
        exigirContagem(3, "select count(*) from instituicao where nome like 'Instituicao Demonstracao GAEA %'");
        exigirContagem(3, "select count(*) from usuario where perfil = 'INSTITUICAO' and email like ?", "%" + EMAIL_DOMAIN);
        exigirContagem(120, "select count(*) from usuario where perfil = 'ALUNO' and email like ?", "%" + EMAIL_DOMAIN);
        exigirContagem(25, "select count(*) from usuario where perfil = 'EMPRESA' and email like ?", "%" + EMAIL_DOMAIN);
        exigirContagem(180, "select count(*) from processo_estagio where nome_empresa like ?", EMPRESA_PREFIXO + "%");
        exigirContagem(60, "select count(*) from pendencia_processo where descricao like ?", PENDENCIA_PREFIXO + "%");
        exigirContagem(80, "select count(*) from prazo_processo where descricao like ?", PRAZO_PREFIXO + "%");
        for (String status : List.of("PENDENTE", "EM_ANALISE", "APROVADO", "REPROVADO")) {
            exigirContagem(45, """
                    select count(*) from processo_estagio
                    where nome_empresa like ? and status = ?
                    """, EMPRESA_PREFIXO + "%", status);
        }
        exigirContagem(0, """
                select count(*) from processo_estagio p
                join usuario u on u.id = p.aluno_usuario_id
                where p.nome_empresa like ? and p.instituicao_id <> u.instituicao_id
                """, EMPRESA_PREFIXO + "%");
        exigirContagem(0, """
                select count(*) from vinculo_supervisor_processo v
                join processo_estagio p on p.id = v.processo_id
                where p.nome_empresa like ? and v.situacao = 'CONFIRMADO'
                  and (v.supervisor_usuario_id is null or v.ativo is not true)
                """, EMPRESA_PREFIXO + "%");
        exigirContagem(0, """
                select count(*) from (
                    select v.processo_id from vinculo_supervisor_processo v
                    join processo_estagio p on p.id = v.processo_id
                    where p.nome_empresa like ? and v.ativo = true
                    group by v.processo_id having count(*) > 1
                ) duplicados
                """, EMPRESA_PREFIXO + "%");

        LegadoSnapshot legadoDepois = capturarLegado();
        if (!legadoAntes.equals(legadoDepois)) {
            throw new IllegalStateException("A carga alterou as contagens dos registros legados");
        }

        for (int instituicao = 1; instituicao <= 3; instituicao++) {
            String email = String.format(Locale.ROOT,
                    "responsavel%02d%s", instituicao, EMAIL_DOMAIN);
            exigirContagem(60, """
                    select count(*) from processo_estagio p
                    join usuario u on u.instituicao_id = p.instituicao_id
                    where u.email = ? and p.nome_empresa like ?
                    """, email, EMPRESA_PREFIXO + "%");
        }
    }

    private int contar(String sql, Object... parametros) {
        Integer total = jdbc.queryForObject(sql, Integer.class, parametros);
        return total == null ? 0 : total;
    }

    private void exigirContagem(int esperada, String sql, Object... parametros) {
        int encontrada = contar(sql, parametros);
        if (encontrada != esperada) {
            throw new IllegalStateException(
                    "Contagem demonstrativa invalida. Esperada=" + esperada
                            + ", encontrada=" + encontrada);
        }
    }

    static String gerarCnpj(long baseNumerica) {
        String base = String.format(Locale.ROOT, "%012d", baseNumerica);
        if (base.length() != 12) {
            throw new IllegalArgumentException("Base de CNPJ invalida");
        }
        int primeiro = digitoCnpj(base, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int segundo = digitoCnpj(base + primeiro,
                new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return base + primeiro + segundo;
    }

    private static int digitoCnpj(String numeros, int[] pesos) {
        int soma = 0;
        for (int indice = 0; indice < pesos.length; indice++) {
            soma += Character.digit(numeros.charAt(indice), 10) * pesos[indice];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private String statusDoProcesso(int numeroLocal) {
        if (numeroLocal <= 15) return "PENDENTE";
        if (numeroLocal <= 30) return "EM_ANALISE";
        if (numeroLocal <= 45) return "APROVADO";
        return "REPROVADO";
    }

    private static String exigirSenha(String senha, String tipo) {
        if (senha == null || senha.length() < 12) {
            throw new IllegalArgumentException("Senha " + tipo + " ausente ou insegura");
        }
        return senha;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " obrigatorio");
        }
        return valor.trim();
    }

    private static void exigirIgual(Object esperado, Object encontrado, String mensagem) {
        if (!esperado.equals(encontrado)) {
            throw new IllegalStateException(mensagem);
        }
    }

    @FunctionalInterface
    private interface IdSupplier {
        long get();
    }

    private record InstituicaoDemo(
            int numero, long id, long responsavelId, String responsavelEmail) {
    }

    private record UsuarioDemo(int numero, long id, String email, Long instituicaoId) {
    }

    private record ProcessoDemo(
            int numero, long id, InstituicaoDemo instituicao, String status) {
    }

    private record LegadoSnapshot(
            int usuarios, int processos, int historicosStatus, int logsSemInstituicao) {
    }
}
