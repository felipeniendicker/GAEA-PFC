import { useCallback, useEffect, useRef, useState } from 'react'
import './DocumentosProcesso.css'
import { formatarDataHora } from '../utils/apresentacao'

const LIMITE_BYTES = 10 * 1024 * 1024

const rotulosSituacao = {
  PENDENTE: 'Pendente de análise',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado'
}

async function mensagemDaResposta(resposta, padrao) {
  try {
    const dados = await resposta.json()
    return dados.mensagem || padrao
  } catch {
    return padrao
  }
}

function formatarTamanho(bytes) {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function DocumentosProcesso({
  apiBase,
  token,
  processoId,
  perfil,
  aoAtualizarHistorico
}) {
  const [documentos, setDocumentos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [arquivo, setArquivo] = useState(null)
  const [reenvioId, setReenvioId] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [baixandoId, setBaixandoId] = useState(null)
  const [analisandoId, setAnalisandoId] = useState(null)
  const [reprovandoId, setReprovandoId] = useState(null)
  const [justificativa, setJustificativa] = useState('')
  const arquivoRef = useRef(null)

  const carregar = useCallback(async () => {
    setCarregando(true)
    setErro('')
    try {
      const resposta = await fetch(`${apiBase}/api/processos/${processoId}/documentos`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!resposta.ok) {
        throw new Error(await mensagemDaResposta(resposta, 'Não foi possível carregar os documentos.'))
      }
      setDocumentos(await resposta.json())
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setCarregando(false)
    }
  }, [apiBase, processoId, token])

  useEffect(() => {
    let ativo = true
    fetch(`${apiBase}/api/processos/${processoId}/documentos`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(async (resposta) => {
        if (!resposta.ok) {
          throw new Error(await mensagemDaResposta(
            resposta,
            'Não foi possível carregar os documentos.'
          ))
        }
        return resposta.json()
      })
      .then((dados) => {
        if (ativo) setDocumentos(dados)
      })
      .catch((falha) => {
        if (ativo) setErro(falha.message || 'Não foi possível conectar com o servidor.')
      })
      .finally(() => {
        if (ativo) setCarregando(false)
      })

    return () => {
      ativo = false
    }
  }, [apiBase, processoId, token])

  function selecionarArquivo(event) {
    setErro('')
    setMensagem('')
    const selecionado = event.target.files?.[0] || null
    if (selecionado && !selecionado.name.toLowerCase().endsWith('.pdf')) {
      setArquivo(null)
      setErro('Selecione um arquivo com extensão .pdf.')
      event.target.value = ''
      return
    }
    if (selecionado && selecionado.size > LIMITE_BYTES) {
      setArquivo(null)
      setErro('O arquivo deve possuir no máximo 10 MB.')
      event.target.value = ''
      return
    }
    setArquivo(selecionado)
  }

  async function enviar(event) {
    event.preventDefault()
    if (!arquivo || arquivo.size === 0) {
      setErro('Selecione um arquivo PDF não vazio.')
      return
    }
    setEnviando(true)
    setErro('')
    setMensagem('')
    const dados = new FormData()
    dados.append('arquivo', arquivo)
    if (reenvioId) {
      dados.append('documentoAnteriorId', reenvioId)
    }
    try {
      const resposta = await fetch(`${apiBase}/api/processos/${processoId}/documentos`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: dados
      })
      if (!resposta.ok) {
        throw new Error(await mensagemDaResposta(resposta, 'Não foi possível enviar o documento.'))
      }
      setArquivo(null)
      setReenvioId(null)
      if (arquivoRef.current) arquivoRef.current.value = ''
      setMensagem('Documento enviado e aguardando análise.')
      await carregar()
      aoAtualizarHistorico?.()
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setEnviando(false)
    }
  }

  async function baixar(documento) {
    setBaixandoId(documento.id)
    setErro('')
    try {
      const resposta = await fetch(
        `${apiBase}/api/processos/${processoId}/documentos/${documento.id}/conteudo`,
        { headers: { Authorization: `Bearer ${token}` } }
      )
      if (!resposta.ok) {
        throw new Error(await mensagemDaResposta(resposta, 'Não foi possível baixar o documento.'))
      }
      const url = URL.createObjectURL(await resposta.blob())
      const link = document.createElement('a')
      link.href = url
      link.download = documento.nomeOriginal
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setBaixandoId(null)
    }
  }

  async function analisar(documentoId, resultado) {
    if (resultado === 'REPROVADO' && !justificativa.trim()) {
      setErro('Informe a justificativa da reprovação.')
      return
    }
    setAnalisandoId(documentoId)
    setErro('')
    setMensagem('')
    try {
      const resposta = await fetch(
        `${apiBase}/api/processos/${processoId}/documentos/${documentoId}/analise`,
        {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            resultado,
            justificativa: resultado === 'REPROVADO' ? justificativa.trim() : null
          })
        }
      )
      if (!resposta.ok) {
        throw new Error(await mensagemDaResposta(resposta, 'Não foi possível analisar o documento.'))
      }
      setReprovandoId(null)
      setJustificativa('')
      setMensagem(resultado === 'APROVADO' ? 'Documento aprovado.' : 'Documento reprovado.')
      await carregar()
      aoAtualizarHistorico?.()
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setAnalisandoId(null)
    }
  }

  function prepararReenvio(documento) {
    setReenvioId(documento.id)
    setArquivo(null)
    setErro('')
    setMensagem(`Selecione a nova versão de ${documento.nomeOriginal}.`)
    arquivoRef.current?.focus()
  }

  return (
    <section className="documentos-processo" aria-live="polite">
      <div className="documentos-cabecalho">
        <div>
          <h4>Documentos do processo</h4>
          <p>Arquivos PDF privados, disponíveis somente aos usuários vinculados.</p>
        </div>
      </div>

      {perfil === 'ALUNO' && (
        <form className="documento-envio" onSubmit={enviar}>
          <label htmlFor={`arquivo-documento-${processoId}`}>
            {reenvioId ? 'Nova versão do documento' : 'Enviar documento PDF'}
          </label>
          <div className="documento-envio-controles">
            <input
              ref={arquivoRef}
              id={`arquivo-documento-${processoId}`}
              type="file"
              accept="application/pdf,.pdf"
              onChange={selecionarArquivo}
              disabled={enviando}
            />
            <button type="submit" disabled={enviando || !arquivo}>
              {enviando ? 'Enviando...' : reenvioId ? 'Enviar nova versão' : 'Enviar documento'}
            </button>
            {reenvioId && (
              <button
                type="button"
                className="botao-secundario"
                disabled={enviando}
                onClick={() => {
                  setReenvioId(null)
                  setArquivo(null)
                  if (arquivoRef.current) arquivoRef.current.value = ''
                }}
              >
                Cancelar reenvio
              </button>
            )}
          </div>
          <small>Somente PDF, com no máximo 10 MB.</small>
        </form>
      )}

      {erro && <p className="mensagem-erro" role="alert">{erro}</p>}
      {mensagem && <p className="mensagem-sucesso" role="status">{mensagem}</p>}

      {carregando ? (
        <p>Carregando documentos...</p>
      ) : documentos.length === 0 ? (
        <p className="documentos-vazio">Nenhum documento enviado para este processo.</p>
      ) : (
        <div className="documentos-lista">
          {documentos.map((documento) => (
            <article className="documento-item" key={documento.id}>
              <div className="documento-item-cabecalho">
                <div>
                  <strong>{documento.nomeOriginal}</strong>
                  <span>{formatarTamanho(documento.tamanhoBytes)}</span>
                </div>
                <span className={`documento-situacao documento-${documento.situacao}`}>
                  {rotulosSituacao[documento.situacao] || documento.situacao}
                </span>
              </div>
              <div className="documento-metadados">
                <span>Enviado em {formatarDataHora(documento.enviadoEm)}</span>
                <span>Por {documento.enviadoPor}</span>
                {documento.analisadoEm && (
                  <span>
                    Analisado em {formatarDataHora(documento.analisadoEm)}
                    {documento.analisadoPor ? ` por ${documento.analisadoPor}` : ''}
                  </span>
                )}
              </div>
              {documento.justificativaReprovacao && (
                <p className="documento-justificativa">
                  <strong>Justificativa:</strong> {documento.justificativaReprovacao}
                </p>
              )}
              <div className="documento-acoes">
                <button
                  type="button"
                  className="botao-secundario"
                  disabled={baixandoId === documento.id}
                  onClick={() => baixar(documento)}
                >
                  {baixandoId === documento.id ? 'Baixando...' : 'Baixar PDF'}
                </button>
                {perfil === 'ALUNO' && documento.situacao === 'REPROVADO' && (
                  <button type="button" onClick={() => prepararReenvio(documento)}>
                    Enviar nova versão
                  </button>
                )}
                {perfil === 'INSTITUICAO' && documento.situacao === 'PENDENTE' && (
                  <>
                    <button
                      type="button"
                      disabled={analisandoId === documento.id}
                      onClick={() => analisar(documento.id, 'APROVADO')}
                    >
                      Aprovar
                    </button>
                    <button
                      type="button"
                      className="botao-reprovar-documento"
                      disabled={analisandoId === documento.id}
                      onClick={() => {
                        setReprovandoId(documento.id)
                        setJustificativa('')
                      }}
                    >
                      Reprovar
                    </button>
                  </>
                )}
              </div>
              {perfil === 'INSTITUICAO' && reprovandoId === documento.id && (
                <div className="documento-reprovacao">
                  <label htmlFor={`justificativa-documento-${documento.id}`}>
                    Justificativa da reprovação
                  </label>
                  <textarea
                    id={`justificativa-documento-${documento.id}`}
                    rows={3}
                    maxLength={1000}
                    value={justificativa}
                    onChange={(event) => setJustificativa(event.target.value)}
                    disabled={analisandoId === documento.id}
                  />
                  <div className="documento-acoes">
                    <button
                      type="button"
                      className="botao-secundario"
                      disabled={analisandoId === documento.id}
                      onClick={() => setReprovandoId(null)}
                    >
                      Cancelar
                    </button>
                    <button
                      type="button"
                      className="botao-reprovar-documento"
                      disabled={analisandoId === documento.id || !justificativa.trim()}
                      onClick={() => analisar(documento.id, 'REPROVADO')}
                    >
                      {analisandoId === documento.id ? 'Salvando...' : 'Confirmar reprovação'}
                    </button>
                  </div>
                </div>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
