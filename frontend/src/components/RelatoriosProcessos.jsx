import { useCallback, useEffect, useState } from 'react'
import './RelatoriosProcessos.css'
import { formatarData, ROTULOS_STATUS_PROCESSO } from '../utils/apresentacao'

const rotulosStatus = ROTULOS_STATUS_PROCESSO

function dataFormatada(data) {
  return formatarData(data)
}

export default function RelatoriosProcessos({ apiBase, token }) {
  const [filtros, setFiltros] = useState({
    situacao: '',
    dataInicio: '',
    dataFim: '',
    aluno: ''
  })
  const [relatorio, setRelatorio] = useState(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')

  const carregarRelatorio = useCallback(async () => {
    setCarregando(true)
    setErro('')
    const parametros = new URLSearchParams()
    Object.entries(filtros).forEach(([chave, valor]) => {
      if (valor) parametros.set(chave, valor)
    })

    try {
      const resposta = await fetch(
        `${apiBase}/api/relatorios/estagios?${parametros.toString()}`,
        { headers: { Authorization: `Bearer ${token}` } }
      )
      if (!resposta.ok) {
        setRelatorio(null)
        setErro('Não foi possível consultar o relatório.')
        return
      }
      setRelatorio(await resposta.json())
    } catch {
      setRelatorio(null)
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setCarregando(false)
    }
  }, [apiBase, filtros, token])

  useEffect(() => {
    const tarefa = window.setTimeout(() => carregarRelatorio(), 0)
    return () => window.clearTimeout(tarefa)
  }, [carregarRelatorio])

  function alterarFiltro(event) {
    setFiltros((atual) => ({ ...atual, [event.target.name]: event.target.value }))
  }

  async function exportarCsv() {
    const parametros = new URLSearchParams()
    Object.entries(filtros).forEach(([chave, valor]) => {
      if (valor) parametros.set(chave, valor)
    })
    try {
      const resposta = await fetch(
        `${apiBase}/api/relatorios/estagios/csv?${parametros.toString()}`,
        { headers: { Authorization: `Bearer ${token}` } }
      )
      if (!resposta.ok) {
        setErro('Não foi possível exportar o relatório.')
        return
      }
      const blob = await resposta.blob()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'relatorio-estagios.csv'
      link.click()
      URL.revokeObjectURL(url)
    } catch {
      setErro('Não foi possível exportar o relatório.')
    }
  }

  return (
    <>
      <header className="cabecalho">
        <p className="cabecalho-contexto">VISÃO INSTITUCIONAL</p>
        <h2>Relatórios de estágios</h2>
        <p className="cabecalho-descricao">
          Consulte e exporte os processos vinculados à sua instituição.
        </p>
      </header>

      <section className="formulario relatorios-filtros">
        <div className="campo">
          <label htmlFor="relatorioSituacao">Situação</label>
          <select id="relatorioSituacao" name="situacao" value={filtros.situacao} onChange={alterarFiltro}>
            <option value="">Todas</option>
            {Object.entries(rotulosStatus).map(([valor, rotulo]) => (
              <option key={valor} value={valor}>{rotulo}</option>
            ))}
          </select>
        </div>
        <div className="campo">
          <label htmlFor="relatorioAluno">Aluno</label>
          <input id="relatorioAluno" name="aluno" value={filtros.aluno} onChange={alterarFiltro} placeholder="Nome do aluno" />
        </div>
        <div className="campo">
          <label htmlFor="relatorioDataInicio">Período inicial</label>
          <input id="relatorioDataInicio" name="dataInicio" type="date" value={filtros.dataInicio} onChange={alterarFiltro} />
        </div>
        <div className="campo">
          <label htmlFor="relatorioDataFim">Período final</label>
          <input id="relatorioDataFim" name="dataFim" type="date" value={filtros.dataFim} onChange={alterarFiltro} />
        </div>
        <div className="relatorios-acoes">
          <button type="button" onClick={carregarRelatorio} disabled={carregando}>
            {carregando ? 'Consultando...' : 'Aplicar filtros'}
          </button>
          <button type="button" className="botao-secundario" onClick={exportarCsv} disabled={carregando}>
            Exportar CSV
          </button>
        </div>
      </section>

      {erro && <p className="mensagem-erro" role="alert">{erro}</p>}
      {carregando && <p className="estado-carregamento">Carregando relatório...</p>}
      {relatorio && !carregando && (
        <>
          <section className="indicadores-relatorio" aria-label="Indicadores do relatório">
            <div className="indicador-card"><span>Total</span><strong>{relatorio.total}</strong></div>
            {Object.entries(relatorio.porSituacao).map(([situacao, total]) => (
              <div className="indicador-card" key={situacao}>
                <span>{rotulosStatus[situacao] || situacao}</span><strong>{total}</strong>
              </div>
            ))}
          </section>
          {relatorio.processos.length === 0 ? (
            <p className="estado-vazio">Nenhum processo encontrado para os filtros informados.</p>
          ) : (
            <section className="tabela-relatorio" aria-label="Processos do relatório">
              <div className="relatorio-linha relatorio-cabecalho">
                <span>Aluno</span><span>Empresa</span><span>Supervisor</span>
                <span>Situação</span><span>Período</span>
              </div>
              {relatorio.processos.map((processo) => (
                <div className="relatorio-linha" key={processo.processoId}>
                  <span>{processo.aluno || '—'}</span>
                  <span>{processo.empresa || '—'}</span>
                  <span>{processo.supervisor || 'Não confirmado'}</span>
                  <span>{rotulosStatus[processo.situacao] || processo.situacao}</span>
                  <span>{dataFormatada(processo.dataInicio)} a {dataFormatada(processo.dataFim)}</span>
                </div>
              ))}
            </section>
          )}
        </>
      )}
    </>
  )
}
