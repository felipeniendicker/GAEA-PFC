import { useEffect, useRef } from 'react'
import { ROTULOS_STATUS_PROCESSO } from '../../utils/apresentacao'

const rotulosStatus = ROTULOS_STATUS_PROCESSO

export function ModalReabertura({
  processo,
  justificativa,
  erro,
  carregando,
  aoMudarJustificativa,
  aoCancelar,
  aoConfirmar
}) {
  const modalRef = useRef(null)
  const campoRef = useRef(null)
  const focoAnteriorRef = useRef(null)

  useEffect(() => {
    focoAnteriorRef.current = document.activeElement
    const overflowAnterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    campoRef.current?.focus()

    return () => {
      document.body.style.overflow = overflowAnterior
      if (focoAnteriorRef.current?.isConnected) {
        focoAnteriorRef.current.focus()
      } else {
        document.querySelector(`[data-processo-id="${processo.id}"]`)?.focus()
      }
    }
  }, [processo.id])

  function lidarTecla(event) {
    if (event.key === 'Escape') {
      event.preventDefault()
      if (!carregando) {
        aoCancelar()
      }
      return
    }

    if (event.key !== 'Tab') {
      return
    }

    const elementosFocaveis = [...(modalRef.current?.querySelectorAll(
      'button:not([disabled]), textarea:not([disabled]), input:not([disabled]), [tabindex]:not([tabindex="-1"])'
    ) || [])]

    if (elementosFocaveis.length === 0) {
      return
    }

    const primeiro = elementosFocaveis[0]
    const ultimo = elementosFocaveis[elementosFocaveis.length - 1]
    const indiceAtual = elementosFocaveis.indexOf(document.activeElement)

    if (event.shiftKey && indiceAtual <= 0) {
      event.preventDefault()
      ultimo.focus()
    } else if (!event.shiftKey && (indiceAtual === -1 || document.activeElement === ultimo)) {
      event.preventDefault()
      primeiro.focus()
    }
  }

  function lidarCliqueFora(event) {
    if (event.target === event.currentTarget && !carregando) {
      aoCancelar()
    }
  }

  return (
    <div className="modal-sobreposicao" onMouseDown={lidarCliqueFora}>
      <section
        className="modal-gaea"
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-modal-reabertura"
        aria-describedby="descricao-modal-reabertura"
        aria-busy={carregando}
        onKeyDown={lidarTecla}
      >
        <div className="modal-cabecalho">
          <div>
            <p className="modal-contexto">PROCESSO #{processo.id}</p>
            <h2 id="titulo-modal-reabertura">Reabrir processo de estágio</h2>
          </div>
          <button
            className="modal-fechar"
            type="button"
            onClick={aoCancelar}
            disabled={carregando}
            aria-label="Cancelar e fechar modal"
          >
            ×
          </button>
        </div>

        <p id="descricao-modal-reabertura" className="modal-descricao">
          O processo retornará para análise. A decisão anterior permanecerá registrada no histórico.
        </p>

        <div className="modal-status-atual">
          <span>Status atual</span>
          <strong className={`status status-${processo.status}`}>
            {rotulosStatus[processo.status] || processo.status}
          </strong>
        </div>

        <form onSubmit={aoConfirmar} noValidate>
          <label htmlFor="justificativaReabertura">Justificativa</label>
          <textarea
            ref={campoRef}
            id="justificativaReabertura"
            value={justificativa}
            onChange={(event) => aoMudarJustificativa(event.target.value)}
            minLength={10}
            maxLength={1000}
            rows={5}
            required
            readOnly={carregando}
            aria-invalid={Boolean(erro)}
            aria-describedby={erro ? 'erro-modal-reabertura' : 'ajuda-modal-reabertura'}
          />
          <span id="ajuda-modal-reabertura" className="modal-ajuda">
            Informe pelo menos 10 caracteres.
          </span>

          {erro && (
            <p id="erro-modal-reabertura" className="modal-erro" role="alert">
              {erro}
            </p>
          )}

          <div className="modal-acoes">
            <button
              className="modal-cancelar"
              type="button"
              onClick={aoCancelar}
              disabled={carregando}
            >
              Cancelar
            </button>
            <button className="modal-confirmar" type="submit" disabled={carregando}>
              {carregando ? 'Reabrindo...' : 'Confirmar reabertura'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}

export function ModalNovaPendencia({
  processo,
  descricao,
  erro,
  carregando,
  aoMudarDescricao,
  aoCancelar,
  aoConfirmar
}) {
  const modalRef = useRef(null)
  const campoRef = useRef(null)
  const focoAnteriorRef = useRef(null)

  useEffect(() => {
    focoAnteriorRef.current = document.activeElement
    const overflowAnterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    campoRef.current?.focus()

    return () => {
      document.body.style.overflow = overflowAnterior
      focoAnteriorRef.current?.focus()
    }
  }, [])

  function lidarTecla(event) {
    if (event.key === 'Escape' && !carregando) {
      event.preventDefault()
      aoCancelar()
      return
    }

    if (event.key !== 'Tab') {
      return
    }

    const elementos = [...(modalRef.current?.querySelectorAll(
      'button:not([disabled]), textarea:not([disabled])'
    ) || [])]
    const primeiro = elementos[0]
    const ultimo = elementos[elementos.length - 1]

    if (event.shiftKey && document.activeElement === primeiro) {
      event.preventDefault()
      ultimo?.focus()
    } else if (!event.shiftKey && document.activeElement === ultimo) {
      event.preventDefault()
      primeiro?.focus()
    }
  }

  return (
    <div
      className="modal-sobreposicao"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !carregando) {
          aoCancelar()
        }
      }}
    >
      <section
        className="modal-gaea"
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-modal-pendencia"
        aria-busy={carregando}
        onKeyDown={lidarTecla}
      >
        <div className="modal-cabecalho">
          <div>
            <p className="modal-contexto">PROCESSO #{processo.id}</p>
            <h2 id="titulo-modal-pendencia">Registrar pendência</h2>
          </div>
          <button
            className="modal-fechar"
            type="button"
            onClick={aoCancelar}
            disabled={carregando}
            aria-label="Cancelar e fechar modal"
          >
            ×
          </button>
        </div>

        <p className="modal-descricao">
          Descreva objetivamente o que precisa ser regularizado neste processo.
        </p>

        <form onSubmit={aoConfirmar} noValidate>
          <label htmlFor="descricaoPendencia">Descrição</label>
          <textarea
            ref={campoRef}
            id="descricaoPendencia"
            value={descricao}
            onChange={(event) => aoMudarDescricao(event.target.value)}
            maxLength={1000}
            rows={5}
            required
            readOnly={carregando}
            aria-invalid={Boolean(erro)}
            aria-describedby={erro ? 'erro-modal-pendencia' : undefined}
          />
          {erro && (
            <p id="erro-modal-pendencia" className="modal-erro" role="alert">
              {erro}
            </p>
          )}

          <div className="modal-acoes">
            <button className="modal-cancelar" type="button" onClick={aoCancelar} disabled={carregando}>
              Cancelar
            </button>
            <button className="modal-confirmar" type="submit" disabled={carregando}>
              {carregando ? 'Registrando...' : 'Registrar pendência'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}

export function ModalPrazo({
  processo,
  prazo,
  descricao,
  dataVencimento,
  erro,
  carregando,
  aoMudarDescricao,
  aoMudarData,
  aoCancelar,
  aoConfirmar
}) {
  const modalRef = useRef(null)
  const campoRef = useRef(null)
  const focoAnteriorRef = useRef(null)

  useEffect(() => {
    focoAnteriorRef.current = document.activeElement
    campoRef.current?.focus()
    return () => focoAnteriorRef.current?.focus()
  }, [])

  function lidarTecla(event) {
    if (event.key === 'Escape' && !carregando) {
      event.preventDefault()
      aoCancelar()
    }
  }

  return (
    <div className="modal-sobreposicao">
      <section
        className="modal-gaea"
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-modal-prazo"
        aria-busy={carregando}
        onKeyDown={lidarTecla}
      >
        <div className="modal-cabecalho">
          <div>
            <p className="modal-contexto">PROCESSO #{processo.id}</p>
            <h2 id="titulo-modal-prazo">{prazo ? 'Atualizar prazo' : 'Registrar prazo'}</h2>
          </div>
          <button
            className="modal-fechar"
            type="button"
            onClick={aoCancelar}
            disabled={carregando}
            aria-label="Cancelar e fechar modal"
          >
            ×
          </button>
        </div>

        <form onSubmit={aoConfirmar} noValidate>
          <label htmlFor="descricaoPrazo">Descrição</label>
          <textarea
            id="descricaoPrazo"
            ref={campoRef}
            value={descricao}
            onChange={(event) => aoMudarDescricao(event.target.value)}
            maxLength={1000}
            rows={4}
            required
            readOnly={carregando}
          />

          <label htmlFor="dataVencimentoPrazo">Data de vencimento</label>
          <input
            id="dataVencimentoPrazo"
            type="date"
            value={dataVencimento}
            onChange={(event) => aoMudarData(event.target.value)}
            required
            disabled={carregando}
          />

          {erro && <p className="modal-erro" role="alert">{erro}</p>}

          <div className="modal-acoes">
            <button className="modal-cancelar" type="button" onClick={aoCancelar} disabled={carregando}>
              Cancelar
            </button>
            <button className="modal-confirmar" type="submit" disabled={carregando}>
              {carregando ? 'Salvando...' : prazo ? 'Atualizar prazo' : 'Registrar prazo'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
