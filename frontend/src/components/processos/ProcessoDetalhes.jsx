import { formatarData, formatarDataHora } from '../../utils/apresentacao'

const rotulosEventoHistorico = {
  CRIACAO_PROCESSO: 'Criação do processo',
  ALTERACAO_STATUS: 'Alteração de status',
  REABERTURA: 'Reabertura',
  INDICACAO_SUPERVISOR: 'Indicação de supervisor',
  CONFIRMACAO_SUPERVISOR: 'Confirmação do supervisor',
  RECUSA_SUPERVISOR: 'Recusa do supervisor',
  SUBSTITUICAO_SUPERVISOR: 'Substituição do supervisor',
  CRIACAO_PENDENCIA: 'Registro de pendência',
  RESOLUCAO_PENDENCIA: 'Resolução de pendência',
  CRIACAO_PRAZO: 'Registro de prazo',
  ATUALIZACAO_PRAZO: 'Atualização de prazo',
  ENVIO_DOCUMENTO: 'Envio de documento',
  REENVIO_DOCUMENTO: 'Reenvio de documento',
  APROVACAO_DOCUMENTO: 'Aprovação de documento',
  REPROVACAO_DOCUMENTO: 'Reprovação de documento'
}

export function HistoricoProcesso({ itens, carregando, erro }) {
  return (
    <div className="historico-processo" aria-live="polite">
      <h4>Histórico do processo</h4>

      {carregando ? (
        <p>Carregando histórico...</p>
      ) : erro ? (
        <p className="mensagem-erro">{erro}</p>
      ) : itens.length === 0 ? (
        <p>Nenhuma movimentação registrada.</p>
      ) : (
        itens.map((item) => (
          <div className="historico-item" key={item.id}>
            <div>
              <strong>{rotulosEventoHistorico[item.tipo] || item.tipo}</strong>
              <span>{item.descricao}</span>
            </div>
            <span>
              {item.responsavel
                ? `Responsável: ${item.responsavel}`
                : 'Responsável não registrado'}
            </span>
            <span>{formatarDataHora(item.dataHora)}</span>
          </div>
        ))
      )}
    </div>
  )
}

export function PendenciasProcesso({
  itens,
  carregando,
  erro,
  podeGerenciar,
  resolvendoId,
  aoNovaPendencia,
  aoResolver
}) {
  return (
    <div className="pendencias-processo" aria-live="polite">
      <div className="pendencias-cabecalho">
        <h4>Pendências do processo</h4>
        {podeGerenciar && (
          <button type="button" onClick={aoNovaPendencia}>
            Registrar pendência
          </button>
        )}
      </div>

      {carregando ? (
        <p>Carregando pendências...</p>
      ) : erro ? (
        <p className="mensagem-erro" role="alert">{erro}</p>
      ) : itens.length === 0 ? (
        <p>Nenhuma pendência registrada para este processo.</p>
      ) : (
        itens.map((item) => (
          <article className="pendencia-item" key={item.id}>
            <div className="pendencia-item-cabecalho">
              <strong>{item.descricao}</strong>
              <span className={`pendencia-situacao pendencia-${item.situacao}`}>
                {item.situacao === 'RESOLVIDA' ? 'Resolvida' : 'Aberta'}
              </span>
            </div>
            <div className="pendencia-datas">
              <span>Criada em {formatarDataHora(item.criadaEm)}</span>
              {item.resolvidaEm && (
                <span>Resolvida em {formatarDataHora(item.resolvidaEm)}</span>
              )}
            </div>
            {podeGerenciar && item.situacao === 'ABERTA' && (
              <button
                className="acao-resolver-pendencia"
                type="button"
                disabled={resolvendoId === item.id}
                onClick={() => aoResolver(item.id)}
              >
                {resolvendoId === item.id ? 'Resolvendo...' : 'Marcar como resolvida'}
              </button>
            )}
          </article>
        ))
      )}
    </div>
  )
}

function formatarDataPrazo(data) {
  return formatarData(data)
}

function prazoVencido(data) {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  return new Date(`${data}T00:00:00`) < hoje
}

export function PrazosProcesso({
  itens,
  carregando,
  erro,
  podeGerenciar,
  aoNovoPrazo,
  aoEditarPrazo
}) {
  return (
    <div className="prazos-processo" aria-live="polite">
      <div className="prazos-cabecalho">
        <h4>Prazos do processo</h4>
        {podeGerenciar && (
          <button type="button" onClick={aoNovoPrazo}>Registrar prazo</button>
        )}
      </div>

      {carregando ? (
        <p>Carregando prazos...</p>
      ) : erro ? (
        <p className="mensagem-erro" role="alert">{erro}</p>
      ) : itens.length === 0 ? (
        <p>Nenhum prazo registrado para este processo.</p>
      ) : (
        itens.map((item) => {
          const vencido = prazoVencido(item.dataVencimento)
          return (
            <article className="prazo-item" key={item.id}>
              <div className="prazo-item-cabecalho">
                <strong>{item.descricao}</strong>
                <span className={`prazo-situacao ${vencido ? 'prazo-vencido' : 'prazo-em-dia'}`}>
                  {vencido ? 'Vencido' : 'Dentro do prazo'}
                </span>
              </div>
              <p className="prazo-vencimento">
                Vencimento: <strong>{formatarDataPrazo(item.dataVencimento)}</strong>
              </p>
              <div className="prazo-datas">
                <span>Criado em {formatarDataHora(item.criadaEm)}</span>
                <span>Atualizado em {formatarDataHora(item.atualizadaEm)}</span>
              </div>
              {podeGerenciar && (
                <button className="acao-editar-prazo" type="button" onClick={() => aoEditarPrazo(item)}>
                  Atualizar prazo
                </button>
              )}
            </article>
          )
        })
      )}
    </div>
  )
}

export function MensagemAcaoProcesso({ mensagem, aoFechar }) {
  if (!mensagem) {
    return null
  }

  return (
    <div
      className={`mensagem-acao-processo ${mensagem.tipo}`}
      role={mensagem.tipo === 'erro' ? 'alert' : 'status'}
      aria-live={mensagem.tipo === 'erro' ? 'assertive' : 'polite'}
    >
      <span>{mensagem.texto}</span>
      <button type="button" onClick={aoFechar} aria-label="Fechar mensagem">
        ×
      </button>
    </div>
  )
}
