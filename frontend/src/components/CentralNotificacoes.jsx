import { useCallback, useEffect, useRef, useState } from 'react'
import './CentralNotificacoes.css'

function dataNotificacao(valor) {
  return new Date(valor).toLocaleString('pt-BR')
}

export default function CentralNotificacoes({ apiBase, token, aoAbrirProcesso }) {
  const [aberta, setAberta] = useState(false)
  const [itens, setItens] = useState([])
  const [contador, setContador] = useState(0)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')
  const referencia = useRef(null)

  const atualizarContador = useCallback(async () => {
    const resposta = await fetch(`${apiBase}/api/notificacoes/nao-lidas/contagem`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (resposta.ok) {
      const dados = await resposta.json()
      setContador(dados.naoLidas)
    }
  }, [apiBase, token])

  async function carregarNotificacoes() {
    setCarregando(true)
    setErro('')
    try {
      const resposta = await fetch(`${apiBase}/api/notificacoes`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!resposta.ok) {
        setErro('Não foi possível carregar as notificações.')
        return
      }
      setItens(await resposta.json())
      await atualizarContador()
    } catch {
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    if (!token) return undefined
    const inicializacao = window.setTimeout(() => {
      atualizarContador().catch(() => {})
    }, 0)
    const intervalo = window.setInterval(() => {
      atualizarContador().catch(() => {})
    }, 60000)
    return () => {
      window.clearTimeout(inicializacao)
      window.clearInterval(intervalo)
    }
  }, [token, atualizarContador])

  useEffect(() => {
    function fecharAoClicarFora(event) {
      if (referencia.current && !referencia.current.contains(event.target)) {
        setAberta(false)
      }
    }
    document.addEventListener('mousedown', fecharAoClicarFora)
    return () => document.removeEventListener('mousedown', fecharAoClicarFora)
  }, [])

  function alternar() {
    const proxima = !aberta
    setAberta(proxima)
    if (proxima) carregarNotificacoes()
  }

  async function marcarComoLida(item) {
    if (item.lida) return
    const resposta = await fetch(`${apiBase}/api/notificacoes/${item.id}/ler`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${token}` }
    })
    if (resposta.ok) {
      setItens((atuais) => atuais.map((atual) => (
        atual.id === item.id
          ? { ...atual, lida: true, lidaEm: new Date().toISOString() }
          : atual
      )))
      setContador((atual) => Math.max(0, atual - 1))
    }
  }

  return (
    <div className="central-notificacoes" ref={referencia}>
      <button
        className="botao-notificacoes"
        type="button"
        onClick={alternar}
        aria-expanded={aberta}
        aria-label={`Notificações${contador > 0 ? `, ${contador} não lidas` : ''}`}
      >
        <span aria-hidden="true">🔔</span>
        {contador > 0 && <span className="contador-notificacoes">{contador > 99 ? '99+' : contador}</span>}
      </button>

      {aberta && (
        <section className="painel-notificacoes" aria-label="Central de notificações">
          <div className="cabecalho-notificacoes">
            <h2>Notificações</h2>
            <button type="button" onClick={carregarNotificacoes}>Atualizar</button>
          </div>
          {carregando ? (
            <p className="estado-notificacoes">Carregando...</p>
          ) : erro ? (
            <p className="estado-notificacoes mensagem-erro">{erro}</p>
          ) : itens.length === 0 ? (
            <p className="estado-notificacoes">Nenhuma notificação.</p>
          ) : (
            <div className="lista-notificacoes">
              {itens.map((item) => (
                <article className={`item-notificacao ${item.lida ? '' : 'nao-lida'}`} key={item.id}>
                  <button type="button" className="conteudo-notificacao" onClick={() => marcarComoLida(item)}>
                    <strong>{item.titulo}</strong>
                    <span>{item.mensagem}</span>
                    <small>{dataNotificacao(item.criadaEm)}</small>
                  </button>
                  {item.processoId && (
                    <button
                      type="button"
                      className="link-notificacao"
                      onClick={() => {
                        marcarComoLida(item)
                        aoAbrirProcesso(item.processoId)
                        setAberta(false)
                      }}
                    >
                      Ver processo
                    </button>
                  )}
                  {!item.lida && <span className="marcador-nao-lida" aria-label="Não lida" />}
                </article>
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  )
}
