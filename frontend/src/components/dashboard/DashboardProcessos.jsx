export default function DashboardProcessos({ resumo, carregando, erro, aoTentarNovamente }) {
  if (carregando) {
    return (
      <section className="dashboard-estado" aria-live="polite">
        <div className="dashboard-carregando" aria-hidden="true" />
        <p>Carregando indicadores institucionais...</p>
      </section>
    )
  }

  if (erro) {
    return (
      <section className="dashboard-estado dashboard-erro" role="alert">
        <h3>Não foi possível carregar o dashboard</h3>
        <p>{erro}</p>
        <button type="button" onClick={aoTentarNovamente}>Tentar novamente</button>
      </section>
    )
  }

  if (!resumo) {
    return null
  }

  const indicadores = [
    { chave: 'pendentes', rotulo: 'Pendentes', valor: resumo.pendentes, classe: 'pendente' },
    { chave: 'emAnalise', rotulo: 'Em análise', valor: resumo.emAnalise, classe: 'analise' },
    { chave: 'aprovados', rotulo: 'Aprovados', valor: resumo.aprovados, classe: 'aprovado' },
    { chave: 'reprovados', rotulo: 'Reprovados', valor: resumo.reprovados, classe: 'reprovado' }
  ]
  const percentualAprovacao = Number(resumo.percentualAprovacao)

  return (
    <>
      {resumo.totalProcessos === 0 && (
        <div className="dashboard-vazio">
          <strong>Nenhum processo cadastrado.</strong>
          <span>Os indicadores serão exibidos conforme os processos forem registrados.</span>
        </div>
      )}

      <section className="dashboard-cards" aria-label="Indicadores dos processos">
        <article className="dashboard-card total">
          <span>Total de processos</span>
          <strong>{resumo.totalProcessos}</strong>
        </article>
        {indicadores.map((indicador) => (
          <article className={`dashboard-card ${indicador.classe}`} key={indicador.chave}>
            <span>{indicador.rotulo}</span>
            <strong>{indicador.valor}</strong>
          </article>
        ))}
      </section>

      <section className="dashboard-detalhes">
        <article className="dashboard-grafico">
          <div className="secao-cabecalho">
            <h3>Distribuição por status</h3>
            <p>Quantidade atual de processos em cada etapa.</p>
          </div>

          {resumo.totalProcessos === 0 ? (
            <p className="estado-vazio">Não há dados para apresentar no gráfico.</p>
          ) : (
            <div className="grafico-barras">
              {indicadores.map((indicador) => {
                const largura = (indicador.valor / resumo.totalProcessos) * 100
                return (
                  <div className="grafico-linha" key={indicador.chave}>
                    <div className="grafico-rotulo">
                      <span>{indicador.rotulo}</span>
                      <strong>{indicador.valor}</strong>
                    </div>
                    <div className="grafico-trilho" aria-hidden="true">
                      <div
                        className={`grafico-barra ${indicador.classe}`}
                        style={{ width: `${largura}%` }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </article>

        <article className="dashboard-percentual">
          <span>Percentual de aprovação</span>
          <strong>
            {percentualAprovacao.toLocaleString('pt-BR', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })}%
          </strong>
          <p>Calculado somente entre processos aprovados e reprovados.</p>
        </article>
      </section>
    </>
  )
}
