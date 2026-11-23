package Gaea.Api.seed;

import Gaea.Api.storage.ArmazenamentoDocumento;
import Gaea.Api.service.PoliticaSenhaService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "app.professional-demo-seed.enabled",
        havingValue = "true")
public class ProfessionalDemoSeeder implements ApplicationRunner {

    static final String CONFIRMACAO_EXIGIDA = "GAEA_PROFESSIONAL_DEMO_ONLY";
    static final String DOMINIO_EMAIL = "@professional-demo.gaea.example";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfessionalDemoSeeder.class);
    private static final String MARCADOR_DATASET = "GAEA_PROFESSIONAL_DEMO_V1";
    private static final String TIPO_PDF = "application/pdf";

    private static final List<InstituicaoSeed> INSTITUICOES = List.of(
            new InstituicaoSeed(
                    "UMC",
                    "Universidade de Mogi das Cruzes (UMC)",
                    gerarCnpj(95_100_000_001L),
                    false),
            new InstituicaoSeed(
                    "IUA",
                    "Instituto Universitário Vale das Araucárias",
                    gerarCnpj(95_200_000_001L),
                    true),
            new InstituicaoSeed(
                    "FHL",
                    "Faculdade Horizonte do Litoral",
                    gerarCnpj(95_300_000_001L),
                    true)
    );

    private static final List<ResponsavelSeed> RESPONSAVEIS = List.of(
            new ResponsavelSeed("Mariana Souto Albuquerque", "mariana.albuquerque", "UMC"),
            new ResponsavelSeed("Ricardo Nogueira Leal", "ricardo.leal", "UMC"),
            new ResponsavelSeed("Beatriz Fontes Guimarães", "beatriz.guimaraes", "IUA"),
            new ResponsavelSeed("Leandro Pires Macedo", "leandro.macedo", "IUA"),
            new ResponsavelSeed("Renata Cavalcanti Moura", "renata.moura", "FHL"),
            new ResponsavelSeed("Sérgio Almeida Vasconcelos", "sergio.vasconcelos", "FHL")
    );

    private static final List<AlunoSeed> ALUNOS = List.of(
            new AlunoSeed("Felipe Rafael Niendicker", "felipe.rafael.niendicker",
                    "UMC-SI-2026-001", "Sistemas de Informação", "UMC", true),
            new AlunoSeed("Ana Clara Mendes Ribeiro", "ana.clara.ribeiro",
                    "UMC-ADS-2026-002", "Análise e Desenvolvimento de Sistemas", "UMC", false),
            new AlunoSeed("Bruno Henrique Valença", "bruno.valenca",
                    "UMC-CC-2026-003", "Ciência da Computação", "UMC", false),
            new AlunoSeed("Camila Torres de Almeida", "camila.almeida",
                    "UMC-SI-2026-004", "Sistemas de Informação", "UMC", false),
            new AlunoSeed("Daniel Augusto Ferreira", "daniel.ferreira",
                    "UMC-ADS-2026-005", "Análise e Desenvolvimento de Sistemas", "UMC", false),
            new AlunoSeed("Eduarda Martins Siqueira", "eduarda.siqueira",
                    "UMC-SI-2026-006", "Sistemas de Informação", "UMC", false),
            new AlunoSeed("Gabriel Nunes Barros", "gabriel.barros",
                    "UMC-CC-2026-007", "Ciência da Computação", "UMC", false),
            new AlunoSeed("Helena Costa Figueiredo", "helena.figueiredo",
                    "UMC-SI-2026-008", "Sistemas de Informação", "UMC", false),
            new AlunoSeed("Isabela Rocha Monteiro", "isabela.monteiro",
                    "IUA-ES-2026-001", "Engenharia de Software", "IUA", false),
            new AlunoSeed("João Pedro Cardoso Lima", "joao.lima",
                    "IUA-SI-2026-002", "Sistemas de Informação", "IUA", false),
            new AlunoSeed("Larissa Gomes Peixoto", "larissa.peixoto",
                    "IUA-ADS-2026-003", "Análise e Desenvolvimento de Sistemas", "IUA", false),
            new AlunoSeed("Marcos Vinícius Azevedo", "marcos.azevedo",
                    "IUA-ES-2026-004", "Engenharia de Software", "IUA", false),
            new AlunoSeed("Natália Freitas Campos", "natalia.campos",
                    "IUA-SI-2026-005", "Sistemas de Informação", "IUA", false),
            new AlunoSeed("Otávio Moreira Santos", "otavio.santos",
                    "IUA-CC-2026-006", "Ciência da Computação", "IUA", false),
            new AlunoSeed("Paula Beatriz Rezende", "paula.rezende",
                    "IUA-ADS-2026-007", "Análise e Desenvolvimento de Sistemas", "IUA", false),
            new AlunoSeed("Rafael Castro Diniz", "rafael.diniz",
                    "IUA-ES-2026-008", "Engenharia de Software", "IUA", false),
            new AlunoSeed("Sabrina Lopes Amaral", "sabrina.amaral",
                    "FHL-SI-2026-001", "Sistemas de Informação", "FHL", false),
            new AlunoSeed("Tiago Henrique Paiva", "tiago.paiva",
                    "FHL-ADS-2026-002", "Análise e Desenvolvimento de Sistemas", "FHL", false),
            new AlunoSeed("Valentina Moraes Teixeira", "valentina.teixeira",
                    "FHL-CC-2026-003", "Ciência da Computação", "FHL", false),
            new AlunoSeed("William Alves Pimenta", "william.pimenta",
                    "FHL-SI-2026-004", "Sistemas de Informação", "FHL", false),
            new AlunoSeed("Yasmin Correia Bastos", "yasmin.bastos",
                    "FHL-ES-2026-005", "Engenharia de Software", "FHL", false),
            new AlunoSeed("Caio Eduardo Tavares", "caio.tavares",
                    "FHL-ADS-2026-006", "Análise e Desenvolvimento de Sistemas", "FHL", false),
            new AlunoSeed("Luana Prado Carvalho", "luana.carvalho",
                    "FHL-SI-2026-007", "Sistemas de Informação", "FHL", false),
            new AlunoSeed("Mateus Ribeiro Neves", "mateus.neves",
                    "FHL-CC-2026-008", "Ciência da Computação", "FHL", false)
    );

    private static final List<EmpresaSeed> EMPRESAS = List.of(
            new EmpresaSeed("NV", "NexaVerde Sistemas Ltda.", gerarCnpj(82_100_000_001L),
                    "08999991", "Avenida das Araucárias, 240 - endereço fictício, Mogi das Cruzes/SP"),
            new EmpresaSeed("PC", "Ponte Clara Tecnologia Ltda.", gerarCnpj(82_200_000_001L),
                    "08999992", "Rua do Mercado Digital, 85 - endereço fictício, Suzano/SP"),
            new EmpresaSeed("OS", "Órbita Sul Dados Ltda.", gerarCnpj(82_300_000_001L),
                    "08999993", "Alameda das Constelações, 310 - endereço fictício, São Paulo/SP"),
            new EmpresaSeed("LS", "Lumina Serra Software Ltda.", gerarCnpj(82_400_000_001L),
                    "08999994", "Rua das Nascentes, 144 - endereço fictício, Guarulhos/SP"),
            new EmpresaSeed("CV", "Cais do Vale Inovação Ltda.", gerarCnpj(82_500_000_001L),
                    "08999995", "Praça do Conhecimento, 52 - endereço fictício, Santos/SP"),
            new EmpresaSeed("HM", "Horizonte Modular Tecnologia Ltda.", gerarCnpj(82_600_000_001L),
                    "08999996", "Avenida Horizonte, 920 - endereço fictício, Campinas/SP"),
            new EmpresaSeed("JC", "Jatobá Cloud Serviços Ltda.", gerarCnpj(82_700_000_001L),
                    "08999997", "Rua Jatobá Branco, 177 - endereço fictício, Osasco/SP"),
            new EmpresaSeed("BN", "Brisa Norte Analytics Ltda.", gerarCnpj(82_800_000_001L),
                    "08999998", "Alameda Brisa Norte, 63 - endereço fictício, Barueri/SP")
    );

    private static final List<SupervisorSeed> SUPERVISORES = List.of(
            new SupervisorSeed("Alice Monteiro Queiroz", "alice.queiroz", "NV"),
            new SupervisorSeed("André Luiz Portela", "andre.portela", "NV"),
            new SupervisorSeed("Carolina Esteves Braga", "carolina.braga", "PC"),
            new SupervisorSeed("Diego Fernandes Malta", "diego.malta", "PC"),
            new SupervisorSeed("Fernanda Ribeiro Luz", "fernanda.luz", "OS"),
            new SupervisorSeed("Gustavo Henrique Prado", "gustavo.prado", "OS"),
            new SupervisorSeed("Juliana Castro Menezes", "juliana.menezes", "LS"),
            new SupervisorSeed("Lucas Pacheco Duarte", "lucas.duarte", "LS"),
            new SupervisorSeed("Márcia Helena Couto", "marcia.couto", "CV"),
            new SupervisorSeed("Pedro Augusto Salgado", "pedro.salgado", "HM"),
            new SupervisorSeed("Roberta Vieira Nascimento", "roberta.nascimento", "JC"),
            new SupervisorSeed("Vinícius Ramos Toledo", "vinicius.toledo", "BN")
    );

    private static final List<String> DESCRICOES_PENDENCIA = List.of(
            "Complementar o comprovante de matrícula do período letivo",
            "Revisar a descrição das atividades previstas no estágio",
            "Apresentar assinatura acadêmica no termo de compromisso",
            "Confirmar a compatibilidade da jornada com o horário do curso",
            "Atualizar os dados de acompanhamento do supervisor",
            "Anexar a versão revisada do cronograma de atividades"
    );

    private static final List<String> DESCRICOES_PRAZO = List.of(
            "Entrega do primeiro relatório de acompanhamento",
            "Revisão intermediária do plano de atividades",
            "Conferência da documentação acadêmica",
            "Avaliação conjunta entre aluno e supervisão",
            "Entrega do relatório semestral de estágio",
            "Atualização cadastral do processo acadêmico"
    );

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final ArmazenamentoDocumento armazenamentoDocumento;
    private final Environment environment;
    private final String bancoEsperado;
    private final String ambienteEsperado;
    private final String ambienteAtual;
    private final String confirmacao;
    private final boolean emailHabilitado;
    private final LocalDate dataReferencia;
    private final String senhaFelipe;
    private final String senhaAluno;
    private final String senhaInstituicao;
    private final String senhaSupervisor;
    private final PoliticaSenhaService politicaSenha;

    public ProfessionalDemoSeeder(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            ArmazenamentoDocumento armazenamentoDocumento,
            Environment environment,
            @Value("${app.professional-demo-seed.expected-database}") String bancoEsperado,
            @Value("${app.professional-demo-seed.expected-environment}") String ambienteEsperado,
            @Value("${RAILWAY_ENVIRONMENT_NAME:local-demo}") String ambienteAtual,
            @Value("${app.professional-demo-seed.confirmation}") String confirmacao,
            @Value("${app.email.enabled:false}") boolean emailHabilitado,
            @Value("${app.professional-demo-seed.reference-date}") LocalDate dataReferencia,
            @Value("${app.professional-demo-seed.password.felipe}") String senhaFelipe,
            @Value("${app.professional-demo-seed.password.aluno}") String senhaAluno,
            @Value("${app.professional-demo-seed.password.instituicao}") String senhaInstituicao,
            @Value("${app.professional-demo-seed.password.supervisor}") String senhaSupervisor,
            PoliticaSenhaService politicaSenha) {

        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.armazenamentoDocumento = armazenamentoDocumento;
        this.environment = environment;
        this.bancoEsperado = exigirTexto(bancoEsperado, "Banco esperado");
        this.ambienteEsperado = exigirTexto(ambienteEsperado, "Ambiente esperado");
        this.ambienteAtual = exigirTexto(ambienteAtual, "Ambiente atual");
        this.confirmacao = exigirTexto(confirmacao, "Confirmação da carga");
        this.emailHabilitado = emailHabilitado;
        this.dataReferencia = Objects.requireNonNull(dataReferencia, "Data de referência obrigatória");
        this.senhaFelipe = exigirSenha(senhaFelipe, "de Felipe");
        this.senhaAluno = exigirSenha(senhaAluno, "dos alunos");
        this.senhaInstituicao = exigirSenha(senhaInstituicao, "institucional");
        this.senhaSupervisor = exigirSenha(senhaSupervisor, "dos supervisores");
        this.politicaSenha = politicaSenha;
        this.politicaSenha.validar(senhaFelipe);
        this.politicaSenha.validar(senhaAluno);
        this.politicaSenha.validar(senhaInstituicao);
        this.politicaSenha.validar(senhaSupervisor);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validarAutorizacao(
                Arrays.asList(environment.getActiveProfiles()).contains("demo"),
                ambienteAtual,
                ambienteEsperado,
                confirmacao,
                emailHabilitado);
        validarBancoAlvo();
        validarBaseExclusiva();

        Map<String, InstituicaoDemo> instituicoes = criarInstituicoes();
        Map<String, UsuarioDemo> responsaveis = criarResponsaveis(instituicoes);
        Map<String, UsuarioDemo> alunos = criarAlunos(instituicoes);
        Map<String, UsuarioDemo> supervisores = criarSupervisores();
        List<ProcessoDemo> processos = criarProcessos(
                instituicoes, responsaveis, alunos, supervisores);

        criarPendencias(processos);
        criarPrazos(processos);
        criarPlanosECargas(processos);
        criarDocumentos(processos);
        criarNotificacoes(processos);
        criarSolicitacoesPrivacidade(alunos);
        validarResultado();

        LOGGER.info(
                "Carga acadêmica profissional {} concluída no banco {} e ambiente {}",
                MARCADOR_DATASET, bancoEsperado, ambienteAtual);
    }

    static void validarAutorizacao(
            boolean perfilDemoAtivo,
            String ambienteAtual,
            String ambienteEsperado,
            String confirmacao,
            boolean emailHabilitado) {

        if (!perfilDemoAtivo) {
            throw new IllegalStateException("Carga profissional recusada: perfil demo não está ativo");
        }
        if (!Objects.equals(ambienteEsperado, ambienteAtual)) {
            throw new IllegalStateException("Carga profissional recusada: ambiente não autorizado");
        }
        if (!CONFIRMACAO_EXIGIDA.equals(confirmacao)) {
            throw new IllegalStateException("Carga profissional recusada: confirmação inválida");
        }
        if (emailHabilitado) {
            throw new IllegalStateException("Carga profissional recusada: envio de e-mail deve estar desabilitado");
        }
    }

    private Map<String, InstituicaoDemo> criarInstituicoes() {
        Map<String, InstituicaoDemo> resultado = new LinkedHashMap<>();
        for (int indice = 0; indice < INSTITUICOES.size(); indice++) {
            int ordem = indice;
            InstituicaoSeed seed = INSTITUICOES.get(indice);
            long id = encontrarId(
                    "select id from instituicao where cnpj = ?",
                    seed.cnpj(),
                    () -> inserirRetornandoId(
                            "insert into instituicao (nome, cnpj, ativa, criada_em, sigla, ficticia) values (?, ?, ?, ?, ?, ?)",
                            seed.nome(), seed.cnpj(), true,
                            dataReferencia.atTime(8, 0).plusMinutes(ordem),
                            seed.codigo(), seed.ficticia()));
            exigirIgual(
                    seed.nome(),
                    jdbc.queryForObject("select nome from instituicao where id = ?", String.class, id),
                    "Instituição profissional conflitante");
            resultado.put(seed.codigo(), new InstituicaoDemo(seed, id));
        }
        return resultado;
    }

    private Map<String, UsuarioDemo> criarResponsaveis(
            Map<String, InstituicaoDemo> instituicoes) {

        String senha = passwordEncoder.encode(senhaInstituicao);
        Map<String, UsuarioDemo> resultado = new LinkedHashMap<>();
        for (int indice = 0; indice < RESPONSAVEIS.size(); indice++) {
            ResponsavelSeed seed = RESPONSAVEIS.get(indice);
            InstituicaoDemo instituicao = instituicoes.get(seed.instituicaoCodigo());
            String email = email(seed.emailLocal());
            long id = garantirUsuario(
                    seed.nome(), email, senha, "INSTITUICAO", instituicao.id(), null,
                    dataReferencia.atTime(8, 30).plusMinutes(indice));
            UsuarioDemo usuario = new UsuarioDemo(id, seed.nome(), email, "INSTITUICAO",
                    instituicao.id(), null, null);
            resultado.put(seed.instituicaoCodigo() + ":" + indice, usuario);
            registrarAuditoriaSeAusente(
                    email, "CARGA_ACADEMICA_FICTICIA",
                    avisoInstitucional(instituicao.seed()), instituicao.id(),
                    dataReferencia.atTime(8, 30).plusMinutes(indice));
        }
        return resultado;
    }

    private Map<String, UsuarioDemo> criarAlunos(Map<String, InstituicaoDemo> instituicoes) {
        String senhaCompartilhada = passwordEncoder.encode(senhaAluno);
        String senhaFelipeProtegida = passwordEncoder.encode(senhaFelipe);
        Map<String, UsuarioDemo> resultado = new LinkedHashMap<>();

        for (int indice = 0; indice < ALUNOS.size(); indice++) {
            AlunoSeed seed = ALUNOS.get(indice);
            InstituicaoDemo instituicao = instituicoes.get(seed.instituicaoCodigo());
            String email = email(seed.emailLocal());
            long id = garantirUsuario(
                    seed.nome(), email,
                    seed.acessoFelipe() ? senhaFelipeProtegida : senhaCompartilhada,
                    "ALUNO", instituicao.id(), seed.matricula(), null);
            UsuarioDemo aluno = new UsuarioDemo(
                    id, seed.nome(), email, "ALUNO", instituicao.id(), seed.matricula(), seed.curso());
            resultado.put(seed.emailLocal(), aluno);
            registrarAuditoriaSeAusente(
                    email, "VINCULO_ACADEMICO_FICTICIO",
                    "Curso: " + seed.curso() + "; matrícula: " + seed.matricula(),
                    instituicao.id(), dataReferencia.atTime(9, 0).plusMinutes(indice));
        }
        return resultado;
    }

    private Map<String, UsuarioDemo> criarSupervisores() {
        String senha = passwordEncoder.encode(senhaSupervisor);
        Map<String, UsuarioDemo> resultado = new LinkedHashMap<>();
        for (int indice = 0; indice < SUPERVISORES.size(); indice++) {
            SupervisorSeed seed = SUPERVISORES.get(indice);
            String email = email(seed.emailLocal());
            long id = garantirUsuario(
                    seed.nome(), email, senha, "EMPRESA", null, null,
                    dataReferencia.atTime(9, 30).plusMinutes(indice));
            resultado.put(seed.emailLocal(), new UsuarioDemo(
                    id, seed.nome(), email, "EMPRESA", null, null, null));
        }
        return resultado;
    }

    private List<ProcessoDemo> criarProcessos(
            Map<String, InstituicaoDemo> instituicoes,
            Map<String, UsuarioDemo> responsaveis,
            Map<String, UsuarioDemo> alunos,
            Map<String, UsuarioDemo> supervisores) {

        List<ProcessoDemo> resultado = new ArrayList<>();
        int numero = 0;
        for (InstituicaoSeed instituicaoSeed : INSTITUICOES) {
            InstituicaoDemo instituicao = instituicoes.get(instituicaoSeed.codigo());
            List<AlunoSeed> alunosDaInstituicao = ALUNOS.stream()
                    .filter(aluno -> aluno.instituicaoCodigo().equals(instituicaoSeed.codigo()))
                    .toList();
            List<UsuarioDemo> responsaveisDaInstituicao = responsaveis.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(instituicaoSeed.codigo() + ":"))
                    .map(Map.Entry::getValue)
                    .toList();

            for (int local = 0; local < 12; local++) {
                numero++;
                AlunoSeed alunoSeed = alunosDaInstituicao.get(local % alunosDaInstituicao.size());
                UsuarioDemo aluno = alunos.get(alunoSeed.emailLocal());
                EmpresaSeed empresa = EMPRESAS.get((numero - 1) % EMPRESAS.size());
                SupervisorSeed supervisorSeed = supervisorDaEmpresa(empresa.codigo(), numero);
                UsuarioDemo supervisor = supervisores.get(supervisorSeed.emailLocal());
                UsuarioDemo responsavel = responsaveisDaInstituicao.get(local % responsaveisDaInstituicao.size());
                String status = List.of("PENDENTE", "EM_ANALISE", "APROVADO", "REPROVADO")
                        .get(local % 4);
                boolean reaberto = local == 5;
                boolean supervisaoConfirmada = !"PENDENTE".equals(status) || local == 8;
                LocalDate inicio = dataReferencia.minusDays(150).plusDays(numero * 6L);
                LocalDate fim = inicio.plusMonths(6);
                LocalDateTime criadoEm = inicio.atTime(10, 0).minusDays(14);

                long processoId = encontrarProcesso(
                        aluno.email(), empresa.nome(), inicio,
                        () -> inserirRetornandoId("""
                                insert into processo_estagio
                                (nome_aluno, email_aluno, nome_empresa, cnpj_empresa,
                                 cep_empresa, endereco_empresa, aluno_usuario_id, instituicao_id,
                                 data_inicio, data_fim, status, versao)
                                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                                """,
                                aluno.nome(), aluno.email(), empresa.nome(), empresa.cnpj(),
                                empresa.cep(), empresa.endereco(), aluno.id(), instituicao.id(),
                                inicio, fim, status));

                long vinculoId = garantirVinculo(
                        processoId, supervisor, supervisaoConfirmada, criadoEm);
                garantirHistoricoSupervisao(
                        vinculoId, null, "PENDENTE", aluno.email(), criadoEm.plusMinutes(1));
                if (supervisaoConfirmada) {
                    garantirHistoricoSupervisao(
                            vinculoId, "PENDENTE", "CONFIRMADO", supervisor.email(),
                            criadoEm.plusMinutes(2));
                }
                criarHistoricoStatus(
                        processoId, status, reaberto, responsavel, criadoEm.plusMinutes(3));
                registrarAuditoriaSeAusente(
                        aluno.email(), "SOLICITACAO_ESTAGIO_FICTICIA",
                        "Processo acadêmico fictício " + processoId + " em " + empresa.nome(),
                        instituicao.id(), criadoEm);

                resultado.add(new ProcessoDemo(
                        numero, processoId, instituicao, aluno, responsavel, supervisor,
                        empresa, status, supervisaoConfirmada, reaberto, inicio, criadoEm));
            }
        }
        return resultado;
    }

    private void criarHistoricoStatus(
            long processoId,
            String status,
            boolean reaberto,
            UsuarioDemo responsavel,
            LocalDateTime instante) {

        if ("PENDENTE".equals(status)) {
            return;
        }
        garantirHistoricoStatus(
                processoId, "PENDENTE", "EM_ANALISE", "ALTERACAO_NORMAL",
                responsavel.email(), null, instante);
        if (reaberto) {
            garantirHistoricoStatus(
                    processoId, "EM_ANALISE", "APROVADO", "ALTERACAO_NORMAL",
                    responsavel.email(), null, instante.plusDays(4));
            garantirHistoricoStatus(
                    processoId, "APROVADO", "EM_ANALISE", "REABERTURA",
                    responsavel.email(),
                    "Revisão acadêmica solicitada para complementar a documentação fictícia",
                    instante.plusDays(8));
            return;
        }
        if ("APROVADO".equals(status) || "REPROVADO".equals(status)) {
            garantirHistoricoStatus(
                    processoId, "EM_ANALISE", status, "ALTERACAO_NORMAL",
                    responsavel.email(),
                    "REPROVADO".equals(status)
                            ? "Documentação acadêmica fictícia requer revisão"
                            : null,
                    instante.plusDays(4));
        }
    }

    private void criarPendencias(List<ProcessoDemo> processos) {
        for (int indice = 0; indice < 18; indice++) {
            ProcessoDemo processo = processos.get(indice * 2);
            String descricao = DESCRICOES_PENDENCIA.get(indice % DESCRICOES_PENDENCIA.size());
            boolean resolvida = indice < 9;
            LocalDateTime criadaEm = processo.criadoEm().plusDays(12);
            encontrarId(
                    "select id from pendencia_processo where processo_id = ? and descricao = ?",
                    processo.id(), descricao,
                    () -> inserirRetornandoId("""
                            insert into pendencia_processo
                            (processo_id, descricao, situacao, criada_em, resolvida_em,
                             responsavel_registro, versao)
                            values (?, ?, ?, ?, ?, ?, 0)
                            """,
                            processo.id(), descricao, resolvida ? "RESOLVIDA" : "ABERTA",
                            criadaEm, resolvida ? criadaEm.plusDays(3) : null,
                            processo.responsavel().email()));
        }
    }

    private void criarPrazos(List<ProcessoDemo> processos) {
        for (int indice = 0; indice < 24; indice++) {
            ProcessoDemo processo = processos.get(indice);
            String descricao = DESCRICOES_PRAZO.get(indice % DESCRICOES_PRAZO.size());
            LocalDateTime criadaEm = processo.criadoEm().plusDays(6);
            LocalDate vencimento = dataReferencia.minusDays(20).plusDays(indice * 3L);
            encontrarId(
                    "select id from prazo_processo where processo_id = ? and descricao = ?",
                    processo.id(), descricao,
                    () -> inserirRetornandoId("""
                            insert into prazo_processo
                            (processo_id, descricao, data_vencimento, criada_em, atualizada_em,
                             responsavel_registro, versao)
                            values (?, ?, ?, ?, ?, ?, 0)
                            """,
                            processo.id(), descricao, vencimento, criadaEm, criadaEm,
                            processo.responsavel().email()));
        }
    }

    private void criarPlanosECargas(List<ProcessoDemo> processos) {
        List<ProcessoDemo> confirmados = processos.stream()
                .filter(ProcessoDemo::supervisaoConfirmada)
                .toList();
        for (ProcessoDemo processo : confirmados) {
            long planoId = garantirPlano(processo);
            garantirAtividadesPlano(planoId, processo.aluno().curso());
            garantirCargasHorarias(processo);
        }
    }

    private long garantirPlano(ProcessoDemo processo) {
        List<Long> ids = jdbc.queryForList(
                "select id from plano_atividades where processo_id = ?",
                Long.class, processo.id());
        if (!ids.isEmpty()) {
            if (ids.size() != 1) {
                throw new IllegalStateException("Mais de um plano para o mesmo processo");
            }
            return ids.get(0);
        }

        String situacao = switch (processo.status()) {
            case "APROVADO" -> "APROVADO";
            case "REPROVADO" -> "REJEITADO";
            default -> "PENDENTE";
        };
        boolean avaliado = !"PENDENTE".equals(situacao);
        LocalDateTime criadoEm = processo.criadoEm().plusDays(5);
        return inserirRetornandoId("""
                insert into plano_atividades
                (processo_id, criado_por_usuario_id, carga_horaria_planejada, situacao,
                 criado_em, avaliado_por_usuario_id, avaliado_em,
                 justificativa_rejeicao, versao)
                values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                processo.id(), processo.aluno().id(), new BigDecimal("360.00"), situacao,
                criadoEm,
                avaliado ? processo.supervisor().id() : null,
                avaliado ? criadoEm.plusDays(2) : null,
                "REJEITADO".equals(situacao)
                        ? "Ajustar a distribuição das atividades acadêmicas fictícias"
                        : null);
    }

    private void garantirAtividadesPlano(long planoId, String curso) {
        if (contar("select count(*) from atividade_plano where plano_id = ?", planoId) > 0) {
            return;
        }
        List<String> atividades = List.of(
                "Aplicar conhecimentos do curso de " + curso + " em um projeto supervisionado",
                "Documentar requisitos, decisões técnicas e resultados de validação",
                "Participar de revisões de qualidade e apresentar a evolução do trabalho"
        );
        for (int indice = 0; indice < atividades.size(); indice++) {
            jdbc.update(
                    "insert into atividade_plano (plano_id, descricao, ordem) values (?, ?, ?)",
                    planoId, atividades.get(indice), indice + 1);
        }
    }

    private void garantirCargasHorarias(ProcessoDemo processo) {
        List<String> atividades = List.of(
                "Levantamento e organização dos requisitos da solução",
                "Implementação acompanhada de revisão técnica",
                "Documentação dos resultados e preparação da apresentação"
        );
        for (int indice = 0; indice < atividades.size(); indice++) {
            String atividade = atividades.get(indice);
            String hash = sha256(atividade.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            LocalDate dataAtividade = processo.inicio().plusDays(7L + indice * 8L);
            int duracao = 240 + indice * 60;
            if (contar("""
                    select count(*) from registro_carga_horaria
                    where processo_id = ? and data_atividade = ?
                      and duracao_minutos = ? and atividade_hash = ?
                    """, processo.id(), dataAtividade, duracao, hash) > 0) {
                continue;
            }

            String situacao = switch (processo.status()) {
                case "APROVADO" -> "APROVADO";
                case "REPROVADO" -> indice == 2 ? "REJEITADO" : "APROVADO";
                default -> "PENDENTE";
            };
            boolean avaliado = !"PENDENTE".equals(situacao);
            LocalDateTime criadoEm = dataAtividade.atTime(18, 0);
            jdbc.update("""
                    insert into registro_carga_horaria
                    (processo_id, registrado_por_usuario_id, data_atividade,
                     duracao_minutos, atividade, atividade_hash, situacao, criado_em,
                     avaliado_por_usuario_id, avaliado_em, justificativa_rejeicao, versao)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """,
                    processo.id(), processo.aluno().id(), dataAtividade,
                    duracao, atividade, hash, situacao, criadoEm,
                    avaliado ? processo.supervisor().id() : null,
                    avaliado ? criadoEm.plusDays(1) : null,
                    "REJEITADO".equals(situacao)
                            ? "Detalhar os resultados alcançados na atividade fictícia"
                            : null);
        }
    }

    private void criarDocumentos(List<ProcessoDemo> processos) {
        List<ProcessoDemo> elegiveis = processos.stream()
                .filter(ProcessoDemo::supervisaoConfirmada)
                .limit(24)
                .toList();
        for (ProcessoDemo processo : elegiveis) {
            String chave = UUID.nameUUIDFromBytes(
                    (MARCADOR_DATASET + ":documento:" + processo.numero())
                            .getBytes(StandardCharsets.UTF_8)) + ".pdf";
            if (contar("select count(*) from documento_processo where chave_armazenamento = ?", chave) > 0) {
                continue;
            }

            byte[] conteudo = gerarPdfFicticio(processo);
            armazenamentoDocumento.armazenar(chave, conteudo);
            removerArquivoSeTransacaoFalhar(chave);

            String situacao = switch (processo.status()) {
                case "APROVADO" -> "APROVADO";
                case "REPROVADO" -> "REPROVADO";
                default -> "PENDENTE";
            };
            boolean analisado = !"PENDENTE".equals(situacao);
            LocalDateTime enviadoEm = processo.criadoEm().plusDays(7);
            jdbc.update("""
                    insert into documento_processo
                    (processo_id, enviado_por_usuario_id, documento_anterior_id,
                     nome_original, chave_armazenamento, tipo_conteudo, tamanho_bytes,
                     hash_sha256, situacao, enviado_em, analisado_por_usuario_id,
                     analisado_em, justificativa_reprovacao, versao)
                    values (?, ?, null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """,
                    processo.id(), processo.aluno().id(),
                    "termo-compromisso-ficticio-" + processo.numero() + ".pdf",
                    chave, TIPO_PDF, conteudo.length, sha256(conteudo), situacao, enviadoEm,
                    analisado ? processo.responsavel().id() : null,
                    analisado ? enviadoEm.plusDays(2) : null,
                    "REPROVADO".equals(situacao)
                            ? "Documento fictício requer complementação acadêmica"
                            : null);
        }
    }

    private byte[] gerarPdfFicticio(ProcessoDemo processo) {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.beginText();
                conteudo.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                conteudo.newLineAtOffset(72, 720);
                conteudo.showText("GAEA - DOCUMENTO FICTICIO - SEM VALIDADE");
                conteudo.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                conteudo.newLineAtOffset(0, -32);
                conteudo.showText("Cenario academico profissional para demonstracao do sistema.");
                conteudo.newLineAtOffset(0, -20);
                conteudo.showText("Processo ficticio: " + processo.numero());
                conteudo.newLineAtOffset(0, -20);
                conteudo.showText("Todos os identificadores e dados empresariais sao sinteticos.");
                conteudo.endText();
            }
            documento.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o PDF fictício", exception);
        }
    }

    private void criarNotificacoes(List<ProcessoDemo> processos) {
        for (ProcessoDemo processo : processos) {
            garantirNotificacao(
                    processo.aluno().id(), processo.id(),
                    "ACOMPANHAMENTO_PROCESSO",
                    "Acompanhamento acadêmico disponível",
                    "Consulte o andamento do processo fictício em " + processo.empresa().nome(),
                    processo.criadoEm().plusDays(1),
                    "professional-demo:aluno:" + processo.numero(),
                    processo.numero() % 3 == 0);
            garantirNotificacao(
                    processo.responsavel().id(), processo.id(),
                    "ACOMPANHAMENTO_INSTITUCIONAL",
                    "Processo acadêmico para acompanhamento",
                    "O processo fictício de " + processo.aluno().nome() + " está disponível.",
                    processo.criadoEm().plusDays(2),
                    "professional-demo:instituicao:" + processo.numero(),
                    processo.numero() % 4 == 0);
        }
    }

    private void garantirNotificacao(
            long destinatarioId,
            long processoId,
            String tipo,
            String titulo,
            String mensagem,
            LocalDateTime criadaEm,
            String chave,
            boolean lida) {

        if (contar("""
                select count(*) from notificacao
                where destinatario_usuario_id = ? and chave_idempotencia = ?
                """, destinatarioId, chave) > 0) {
            return;
        }
        jdbc.update("""
                insert into notificacao
                (destinatario_usuario_id, tipo_evento, titulo, mensagem, criada_em,
                 lida, lida_em, processo_id, chave_idempotencia)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                destinatarioId, tipo, titulo, mensagem, criadaEm,
                lida, lida ? criadaEm.plusDays(1) : null, processoId, chave);
    }

    private void criarSolicitacoesPrivacidade(Map<String, UsuarioDemo> alunos) {
        List<UsuarioDemo> selecionados = alunos.values().stream().limit(6).toList();
        for (int indice = 0; indice < selecionados.size(); indice++) {
            UsuarioDemo aluno = selecionados.get(indice);
            String tipo = indice % 2 == 0 ? "ACESSO" : "CORRECAO";
            String descricao = indice % 2 == 0
                    ? "Solicitação fictícia de acesso aos dados acadêmicos da base de demonstração"
                    : "Solicitação fictícia de correção cadastral para validação do fluxo";
            if (contar("""
                    select count(*) from solicitacao_privacidade
                    where usuario_id = ? and tipo = ? and descricao = ?
                    """, aluno.id(), tipo, descricao) == 0) {
                jdbc.update("""
                        insert into solicitacao_privacidade
                        (usuario_id, tipo, descricao, situacao, criado_em, atualizado_em, versao)
                        values (?, ?, ?, 'PENDENTE', ?, ?, 0)
                        """,
                        aluno.id(), tipo, descricao,
                        dataReferencia.atTime(14, 0).plusDays(indice),
                        dataReferencia.atTime(14, 0).plusDays(indice));
            }
        }
    }

    private long garantirUsuario(
            String nome,
            String email,
            String senha,
            String perfil,
            Long instituicaoId,
            String matricula,
            LocalDateTime emailVerificadoEm) {

        List<Map<String, Object>> existentes = jdbc.queryForList("""
                select id, nome, perfil, instituicao_id, matricula
                from usuario where lower(trim(email)) = ?
                """, email.toLowerCase(Locale.ROOT));
        if (!existentes.isEmpty()) {
            if (existentes.size() != 1) {
                throw new IllegalStateException("E-mail profissional duplicado");
            }
            Map<String, Object> existente = existentes.get(0);
            exigirIgual(nome, existente.get("nome"), "Nome profissional conflitante");
            exigirIgual(perfil, existente.get("perfil"), "Perfil profissional conflitante");
            exigirIgual(instituicaoId, numeroNulo(existente.get("instituicao_id")),
                    "Instituição profissional conflitante");
            exigirIgual(matricula, existente.get("matricula"), "Matrícula profissional conflitante");
            return numero(existente.get("id"));
        }
        return inserirRetornandoId("""
                insert into usuario
                (nome, email, senha, perfil, instituicao_id, matricula, ativo, ativado,
                 termos_aceitos, termos_aceitos_em, email_verificado_em)
                values (?, ?, ?, ?, ?, ?, true, true, true, ?, ?)
                """,
                nome, email, senha, perfil, instituicaoId, matricula,
                dataReferencia.atStartOfDay(), emailVerificadoEm);
    }

    private long garantirVinculo(
            long processoId,
            UsuarioDemo supervisor,
            boolean confirmado,
            LocalDateTime criadoEm) {

        List<Long> existentes = jdbc.queryForList(
                "select id from vinculo_supervisor_processo where processo_id = ? and ativo = true",
                Long.class, processoId);
        if (!existentes.isEmpty()) {
            if (existentes.size() != 1) {
                throw new IllegalStateException("Mais de um vínculo ativo no processo profissional");
            }
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

    private void garantirHistoricoStatus(
            long processoId,
            String anterior,
            String novo,
            String tipo,
            String responsavel,
            String justificativa,
            LocalDateTime dataHora) {

        if (contar("""
                select count(*) from historico_status_processo
                where processo_id = ? and status_anterior = ?
                  and status_novo = ? and tipo_operacao = ?
                """, processoId, anterior, novo, tipo) == 0) {
            jdbc.update("""
                    insert into historico_status_processo
                    (processo_id, status_anterior, status_novo, usuario_responsavel,
                     data_hora, justificativa, tipo_operacao)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    processoId, anterior, novo, responsavel, dataHora, justificativa, tipo);
        }
    }

    private void garantirHistoricoSupervisao(
            long vinculoId,
            String anterior,
            String novo,
            String responsavel,
            LocalDateTime dataHora) {

        if (contar("""
                select count(*) from historico_vinculo_supervisor
                where vinculo_id = ? and situacao_nova = ?
                """, vinculoId, novo) == 0) {
            jdbc.update("""
                    insert into historico_vinculo_supervisor
                    (vinculo_id, situacao_anterior, situacao_nova,
                     usuario_responsavel, justificativa, data_hora)
                    values (?, ?, ?, ?, null, ?)
                    """,
                    vinculoId, anterior, novo, responsavel, dataHora);
        }
    }

    private void registrarAuditoriaSeAusente(
            String usuario,
            String acao,
            String recurso,
            long instituicaoId,
            LocalDateTime dataHora) {

        if (contar("""
                select count(*) from log_auditoria
                where usuario = ? and acao = ? and recurso = ? and instituicao_id = ?
                """, usuario, acao, recurso, instituicaoId) == 0) {
            jdbc.update("""
                    insert into log_auditoria
                    (usuario, acao, recurso, data_hora, instituicao_id)
                    values (?, ?, ?, ?, ?)
                    """,
                    usuario, acao, recurso, dataHora, instituicaoId);
        }
    }

    private long encontrarProcesso(
            String emailAluno,
            String empresa,
            LocalDate inicio,
            IdSupplier criacao) {

        List<Long> ids = jdbc.queryForList("""
                select id from processo_estagio
                where email_aluno = ? and nome_empresa = ? and data_inicio = ?
                """, Long.class, emailAluno, empresa, inicio);
        if (ids.size() > 1) {
            throw new IllegalStateException("Processo profissional duplicado");
        }
        return ids.isEmpty() ? criacao.get() : ids.get(0);
    }

    private long encontrarId(String sql, Object parametro, IdSupplier criacao) {
        List<Long> ids = jdbc.queryForList(sql, Long.class, parametro);
        if (ids.size() > 1) {
            throw new IllegalStateException("Chave profissional duplicada");
        }
        return ids.isEmpty() ? criacao.get() : ids.get(0);
    }

    private long encontrarId(
            String sql,
            Object parametro1,
            Object parametro2,
            IdSupplier criacao) {

        List<Long> ids = jdbc.queryForList(sql, Long.class, parametro1, parametro2);
        if (ids.size() > 1) {
            throw new IllegalStateException("Chave profissional duplicada");
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
            throw new IllegalStateException("O banco não retornou a chave gerada");
        }
        return chave.longValue();
    }

    private void validarBancoAlvo() {
        String atual = jdbc.execute((ConnectionCallback<String>) connection -> connection.getCatalog());
        if (atual == null || !atual.equalsIgnoreCase(bancoEsperado)) {
            throw new IllegalStateException("Carga profissional recusada: banco não autorizado");
        }
    }

    private void validarBaseExclusiva() {
        if (contar("select count(*) from usuario where email not like ?", "%" + DOMINIO_EMAIL) > 0) {
            throw new IllegalStateException(
                    "Carga profissional recusada: existem usuários fora do domínio reservado");
        }
        if (contar("""
                select count(*) from processo_estagio
                where email_aluno is null or email_aluno not like ?
                """, "%" + DOMINIO_EMAIL) > 0) {
            throw new IllegalStateException(
                    "Carga profissional recusada: existem processos não demonstrativos");
        }
        int instituicoesConhecidas = 0;
        for (InstituicaoSeed instituicao : INSTITUICOES) {
            instituicoesConhecidas += contar(
                    "select count(*) from instituicao where cnpj = ?", instituicao.cnpj());
        }
        if (contar("select count(*) from instituicao") != instituicoesConhecidas) {
            throw new IllegalStateException(
                    "Carga profissional recusada: existem instituições não autorizadas");
        }
    }

    private void validarResultado() {
        exigirMinimo(3, "select count(*) from instituicao");
        exigirMinimo(42, "select count(*) from usuario where email like ?", "%" + DOMINIO_EMAIL);
        exigirMinimo(36, "select count(*) from processo_estagio where email_aluno like ?", "%" + DOMINIO_EMAIL);
        exigirMinimo(18, "select count(*) from pendencia_processo");
        exigirMinimo(24, "select count(*) from prazo_processo");
        exigirMinimo(30, "select count(*) from plano_atividades");
        exigirMinimo(90, "select count(*) from atividade_plano");
        exigirMinimo(90, "select count(*) from registro_carga_horaria");
        exigirMinimo(24, "select count(*) from documento_processo");
        exigirMinimo(72, "select count(*) from notificacao");
        exigirMinimo(6, "select count(*) from solicitacao_privacidade");
    }

    private void exigirMinimo(int minimo, String sql, Object... parametros) {
        int encontrado = contar(sql, parametros);
        if (encontrado < minimo) {
            throw new IllegalStateException(
                    "Carga profissional incompleta. Mínimo=" + minimo + ", encontrado=" + encontrado);
        }
    }

    private int contar(String sql, Object... parametros) {
        Integer total = jdbc.queryForObject(sql, Integer.class, parametros);
        return total == null ? 0 : total;
    }

    private void removerArquivoSeTransacaoFalhar(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    armazenamentoDocumento.remover(chave);
                }
            }
        });
    }

    private static SupervisorSeed supervisorDaEmpresa(String empresaCodigo, int numeroProcesso) {
        List<SupervisorSeed> candidatos = SUPERVISORES.stream()
                .filter(supervisor -> supervisor.empresaCodigo().equals(empresaCodigo))
                .toList();
        return candidatos.get((numeroProcesso - 1) % candidatos.size());
    }

    private static String avisoInstitucional(InstituicaoSeed instituicao) {
        if ("UMC".equals(instituicao.codigo())) {
            return "Cenário acadêmico fictício; a presença da UMC não representa vínculo ou endosso oficial";
        }
        return "Instituição integralmente fictícia da base acadêmica profissional";
    }

    private static String email(String parteLocal) {
        return parteLocal + DOMINIO_EMAIL;
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível", exception);
        }
    }

    private static String gerarCnpj(long baseNumerica) {
        String base = String.format(Locale.ROOT, "%012d", baseNumerica);
        if (base.length() != 12) {
            throw new IllegalArgumentException("Base de CNPJ inválida");
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

    private static String exigirSenha(String senha, String descricao) {
        if (senha == null || senha.length() < 12) {
            throw new IllegalArgumentException("Senha " + descricao + " ausente ou insegura");
        }
        return senha;
    }

    private static String exigirTexto(String valor, String descricao) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(descricao + " obrigatório");
        }
        return valor.trim();
    }

    private static void exigirIgual(Object esperado, Object encontrado, String mensagem) {
        if (!Objects.equals(esperado, encontrado)) {
            throw new IllegalStateException(mensagem);
        }
    }

    private static long numero(Object valor) {
        return ((Number) valor).longValue();
    }

    private static Long numeroNulo(Object valor) {
        return valor == null ? null : numero(valor);
    }

    @FunctionalInterface
    private interface IdSupplier {
        long get();
    }

    private record InstituicaoSeed(String codigo, String nome, String cnpj, boolean ficticia) {
    }

    private record ResponsavelSeed(String nome, String emailLocal, String instituicaoCodigo) {
    }

    private record AlunoSeed(
            String nome,
            String emailLocal,
            String matricula,
            String curso,
            String instituicaoCodigo,
            boolean acessoFelipe) {
    }

    private record EmpresaSeed(
            String codigo,
            String nome,
            String cnpj,
            String cep,
            String endereco) {
    }

    private record SupervisorSeed(String nome, String emailLocal, String empresaCodigo) {
    }

    private record InstituicaoDemo(InstituicaoSeed seed, long id) {
    }

    private record UsuarioDemo(
            long id,
            String nome,
            String email,
            String perfil,
            Long instituicaoId,
            String matricula,
            String curso) {
    }

    private record ProcessoDemo(
            int numero,
            long id,
            InstituicaoDemo instituicao,
            UsuarioDemo aluno,
            UsuarioDemo responsavel,
            UsuarioDemo supervisor,
            EmpresaSeed empresa,
            String status,
            boolean supervisaoConfirmada,
            boolean reaberto,
            LocalDate inicio,
            LocalDateTime criadoEm) {
    }
}
