import SolicitacoesPrivacidade from '../SolicitacoesPrivacidade'
import { API } from '../../services/api'
import { formatarData } from '../../utils/apresentacao'

export function PaginaPrivacidade({ documentosLegais, usuario, token }) {
  return (
          <main className="conteudo pagina-publica-cadastro">
            <header className="cabecalho">
              <p className="cabecalho-contexto">LGPD</p>
              <h2>Política de Privacidade</h2>
              <p className="cabecalho-descricao">
                {documentosLegais
                  ? `Versão ${documentosLegais.versaoPoliticaPrivacidade} — ${formatarData(documentosLegais.politicaPrivacidadePublicadaEm)}`
                  : 'Versão vigente'}
              </p>
            </header>

            <article className="formulario documento-lgpd">
              <div className="secao-cabecalho">
                <h3>Política de Privacidade do GAEA</h3>
                <p>
                  Informações sobre o uso e a proteção de dados pessoais no
                  sistema, conforme a Lei nº 13.709/2018 (Lei Geral de Proteção
                  de Dados Pessoais — LGPD).
                </p>
              </div>

              <section className="documento-secao">
                <h3>1. Sobre esta política</h3>
                <p>
                  Esta Política apresenta como os dados pessoais são utilizados
                  no GAEA, sistema acadêmico voltado à gestão e ao acompanhamento
                  de processos de estágio.
                </p>
              </section>

              <section className="documento-secao documento-destaque">
                <h3>2. Dados tratados pelo GAEA</h3>
                <p>De acordo com as funcionalidades atuais, o sistema trata:</p>
                <ul>
                  <li>nome, e-mail e perfil de acesso (ALUNO, EMPRESA ou INSTITUICAO);</li>
                  <li>senha protegida por hash BCrypt, sem armazenamento em texto puro;</li>
                  <li>nome e e-mail do aluno relacionados ao processo de estágio;</li>
                  <li>nome e CNPJ da empresa;</li>
                  <li>datas de início e fim e status do processo de estágio;</li>
                  <li>CEP e dados de endereço consultados por meio do ViaCEP;</li>
                  <li>usuário, ação, recurso e data/hora dos registros de auditoria;</li>
                  <li>aceite dos Termos e da Política de Privacidade e sua data/hora.</li>
                </ul>
              </section>

              <section className="documento-secao documento-destaque">
                <h3>3. Finalidades</h3>
                <p>Os dados são utilizados para:</p>
                <ul>
                  <li>criar e identificar a conta do usuário;</li>
                  <li>autenticar o usuário e controlar o acesso conforme seu perfil;</li>
                  <li>cadastrar e acompanhar processos de estágio;</li>
                  <li>identificar o aluno e a empresa envolvidos no processo;</li>
                  <li>consultar o endereço da empresa a partir do CEP;</li>
                  <li>registrar ações importantes para rastreabilidade e segurança;</li>
                  <li>registrar o aceite dos Termos e da Política de Privacidade.</li>
                </ul>
              </section>

              <section className="documento-secao documento-destaque">
                <h3>4. Tratamento e segurança</h3>
                <p>
                  Os dados são recebidos e persistidos pelo backend e pelo banco
                  de dados do GAEA. As senhas são protegidas com BCrypt, a
                  autenticação utiliza JWT e a autorização por perfil é
                  verificada no backend. O sistema também mantém registros de
                  auditoria e se comunica com o ViaCEP para consultar endereços.
                </p>
              </section>

              <section className="documento-secao">
                <h3>5. Serviço externo ViaCEP</h3>
                <p>
                  O GAEA envia ao ViaCEP somente o CEP necessário para consultar
                  o endereço. Senhas, tokens de autenticação e dados completos
                  do processo de estágio não são enviados ao serviço.
                </p>
              </section>

              <section className="documento-secao">
                <h3>6. Retenção</h3>
                <p>
                  Os dados são mantidos enquanto forem necessários às
                  funcionalidades e à finalidade acadêmica do sistema. O projeto
                  ainda não possui exclusão automática por prazo. Em um ambiente
                  real, regras específicas de retenção deverão observar as
                  finalidades e as obrigações aplicáveis.
                </p>
              </section>

              <section className="documento-secao documento-destaque">
                <h3>7. Seus direitos</h3>
                <p>Conforme aplicável, o titular pode solicitar:</p>
                <ul>
                  <li>confirmação da existência de tratamento;</li>
                  <li>acesso aos seus dados;</li>
                  <li>correção de dados incompletos, inexatos ou desatualizados;</li>
                  <li>informações sobre o tratamento realizado;</li>
                  <li>eliminação de dados tratados com consentimento, quando aplicável e observadas as hipóteses legais;</li>
                  <li>revogação do consentimento, quando ele for a base aplicável.</li>
                </ul>
              </section>

              <section className="documento-secao">
                <h3>8. Contato</h3>
                <p>
                  Dúvidas e solicitações relacionadas à privacidade podem ser
                  enviadas ao canal de contato do projeto GAEA:
                  {' '}feliperafaelniendicker@gmail.com.
                </p>
              </section>

              <section className="documento-secao">
                <h3>9. Atualização</h3>
                <p>
                  Esta Política poderá ser atualizada conforme o desenvolvimento
                  do projeto. A versão disponível no sistema deve ser consultada
                  para conhecer o conteúdo vigente.
                </p>
              </section>
            </article>
            {usuario?.perfil === 'ADMIN_GAEA' && (
              <section className="formulario documento-lgpd" aria-labelledby="privacidade-admin-titulo">
                <h3 id="privacidade-admin-titulo">Atendimento administrativo de privacidade</h3>
                <p>
                  O perfil administrador não registra solicitações como titular nesta tela.
                  Consulte, assuma e decida os pedidos no módulo administrativo de Solicitações
                  de privacidade, disponível na página de Instituições.
                </p>
              </section>
            )}
            {usuario && usuario.perfil !== 'ADMIN_GAEA' && (
              <SolicitacoesPrivacidade apiBase={API} token={token} />
            )}
          </main>
  )
}

export function PaginaTermos({ documentosLegais }) {
  return (
          <main className="conteudo">
            <header className="cabecalho">
              <p className="cabecalho-contexto">GAEA</p>
              <h2>Termos de Uso e Termo de Aceite</h2>
              <p className="cabecalho-descricao">
                {documentosLegais
                  ? `Versão ${documentosLegais.versaoTermosUso} — ${formatarData(documentosLegais.termosUsoPublicadoEm)}`
                  : 'Versão vigente'}
              </p>
            </header>

            <article className="formulario documento-lgpd">
              <div className="secao-cabecalho">
                <h3>Termo de Aceite do GAEA</h3>
                <p>
                  Condições para utilização do sistema acadêmico GAEA.
                </p>
              </div>

              <section className="documento-secao">
                <h3>1. Sobre o GAEA</h3>
                <p>
                  O GAEA é um projeto acadêmico destinado à gestão e ao
                  acompanhamento de processos de estágio.
                </p>
              </section>

              <section className="documento-secao">
                <h3>2. Perfis de usuário</h3>
                <p>
                  O sistema possui os perfis ALUNO, EMPRESA e INSTITUICAO.
                  As funcionalidades e permissões disponíveis dependem do perfil
                  associado à conta.
                </p>
              </section>

              <section className="documento-secao">
                <h3>3. Conta e credenciais</h3>
                <p>
                  O usuário deve informar dados corretos, utilizar suas próprias
                  credenciais e não compartilhar sua senha ou a conta com outras
                  pessoas.
                </p>
              </section>

              <section className="documento-secao">
                <h3>4. Utilização do sistema</h3>
                <p>
                  O GAEA deve ser utilizado para suas finalidades acadêmicas.
                  Não é permitido tentar acessar áreas sem autorização, usar
                  credenciais de terceiros, inserir informações intencionalmente
                  falsas ou comprometer a segurança do sistema.
                </p>
              </section>

              <section className="documento-secao">
                <h3>5. Processos de estágio</h3>
                <p>
                  Conforme o perfil, o usuário poderá cadastrar, consultar ou
                  atualizar informações dos processos. O aluno pode consultar
                  somente os processos relacionados ao e-mail de sua conta.
                </p>
              </section>

              <section className="documento-secao">
                <h3>6. Registros de auditoria</h3>
                <p>
                  Ações importantes podem ser registradas com o usuário
                  responsável, a ação realizada, o recurso relacionado e a
                  data/hora, permitindo rastreabilidade e apoio à segurança.
                </p>
              </section>

              <section className="documento-secao">
                <h3>7. Uso do ViaCEP</h3>
                <p>
                  O GAEA utiliza o ViaCEP para consultar endereço a partir do
                  CEP informado. A disponibilidade dessa consulta também depende
                  do serviço externo.
                </p>
              </section>

              <section className="documento-secao">
                <h3>8. Privacidade e proteção de dados</h3>
                <p>
                  O tratamento dos dados pessoais é explicado na Política de
                  Privacidade do GAEA, que apresenta os dados utilizados, suas
                  finalidades, o tratamento realizado e os direitos dos titulares.
                </p>
              </section>

              <section className="documento-secao">
                <h3>9. Disponibilidade do projeto</h3>
                <p>
                  Por ser um projeto acadêmico em desenvolvimento, o GAEA pode
                  passar por atualizações, manutenção e períodos de
                  indisponibilidade.
                </p>
              </section>

              <section className="documento-secao documento-destaque">
                <h3>10. Aceite dos Termos</h3>
                <p>
                  Durante o cadastro, o usuário deve marcar a opção específica
                  declarando que leu e aceitou estes Termos de Uso e a Política
                  de Privacidade. O GAEA registra o aceite e sua respectiva
                  data/hora. Sem essa confirmação, o cadastro não é concluído.
                </p>
              </section>

              <section className="documento-secao">
                <h3>11. Contato</h3>
                <p>
                  Dúvidas relacionadas ao projeto podem ser enviadas para:
                  {' '}feliperafaelniendicker@gmail.com.
                </p>
              </section>
            </article>
          </main>
  )
}
