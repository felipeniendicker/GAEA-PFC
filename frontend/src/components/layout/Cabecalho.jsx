import CentralNotificacoes from '../CentralNotificacoes'
import { API } from '../../services/api'
import { apresentacaoInstituicao } from '../../utils/apresentacao'

export default function Cabecalho({
  mostrarSair = false,
  usuario,
  tela,
  secaoInstituicao,
  aoIrInicio,
  aoIrSecao,
  aoIrPrivacidade,
  aoIrTermos,
  aoSair,
  token,
  aoAbrirProcesso
}) {
  const menusPorPerfil = {
    INSTITUICAO: [
      { secao: 'dashboard', rotulo: 'Dashboard' },
      { secao: 'alunos', rotulo: 'Gestão de alunos' },
      { secao: 'processos', rotulo: 'Processos de estágio' },
      { secao: 'acompanhamento', rotulo: 'Pendências e prazos' },
      { secao: 'relatorios', rotulo: 'Relatórios' },
      { secao: 'auditoria', rotulo: 'Auditoria' }
    ],
    ALUNO: [
      { secao: 'dashboard', rotulo: 'Meu dashboard' },
      { secao: 'solicitar', rotulo: 'Solicitar estágio' },
      { secao: 'processos', rotulo: 'Meus processos' }
    ],
    EMPRESA: [
      { secao: 'dashboard', rotulo: 'Meu dashboard' },
      { secao: 'solicitacoes', rotulo: 'Solicitações de supervisão' },
      { secao: 'supervisionados', rotulo: 'Estágios supervisionados' },
      { secao: 'acompanhamento', rotulo: 'Prazos e pendências' }
    ],
    ADMIN_GAEA: [
      { secao: 'dashboard', rotulo: 'Instituições' }
    ]
  }
  const itensPerfil = usuario ? menusPorPerfil[usuario.perfil] || [] : []
  const nomeInstituicao = usuario?.perfil === 'INSTITUICAO'
    ? apresentacaoInstituicao(usuario.instituicao).nomeComSigla
    : ''

  return (
    <header className="navbar">
      <div className="navbar-conteudo">
        <div className="cabecalho-identidade">
          <div className="logo">
            <div className="logo-marca">G</div>

            <div>
              <h1>GAEA</h1>
              <p>Gestão de Estágios</p>
            </div>
          </div>

          {nomeInstituicao && (
            <div className="cabecalho-instituicao" title={nomeInstituicao}>
              <span>Instituição</span>
              <strong>{nomeInstituicao}</strong>
            </div>
          )}
        </div>

        <nav aria-label="Navegação principal">
          {usuario && mostrarSair ? (
            itensPerfil.map((item) => (
              <button
                className={tela === 'sistema' && secaoInstituicao === item.secao ? 'ativo' : ''}
                type="button"
                onClick={() => aoIrSecao(item.secao)}
                aria-current={tela === 'sistema' && secaoInstituicao === item.secao
                  ? 'page'
                  : undefined}
                key={item.secao}
              >
                {item.rotulo}
              </button>
            ))
          ) : (
            <button
              className={tela === 'sistema' ? 'ativo' : ''}
              type="button"
              onClick={aoIrInicio}
            >
              Início
            </button>
          )}

          {usuario && mostrarSair && usuario.perfil !== 'ADMIN_GAEA' && (
            <CentralNotificacoes
              apiBase={API}
              token={token}
              aoAbrirProcesso={aoAbrirProcesso}
            />
          )}

          <button
            className={tela === 'privacidade' ? 'ativo' : ''}
            type="button"
            onClick={aoIrPrivacidade}
          >
            Privacidade
          </button>

          <button
            className={tela === 'termos' ? 'ativo' : ''}
            type="button"
            onClick={aoIrTermos}
          >
            Termos
          </button>

          {mostrarSair && (
            <button type="button" onClick={aoSair}>
              Sair
            </button>
          )}
        </nav>
      </div>
    </header>
  )
}
