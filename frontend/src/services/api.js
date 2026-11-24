export const API = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

export async function mensagemDaApi(resposta, mensagemPadrao) {
  try {
    const dados = await resposta.json()
    const mensagensDosCampos = dados.campos
      ? [...new Set(Object.values(dados.campos))]
      : []

    if (mensagensDosCampos.length > 0) {
      return mensagensDosCampos.join('. ')
    }

    return dados.mensagem || mensagemPadrao
  } catch {
    return mensagemPadrao
  }
}
