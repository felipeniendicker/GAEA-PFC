import assert from 'node:assert/strict'
import test from 'node:test'

import { lerTokensDaUrl, possuiTokenSensivel } from './tokensUrl.js'

test('preserva todos os tokens recebidos no hash', () => {
  const casos = {
    conviteAluno: 'convite-seguro',
    verificarEmail: 'verificacao-supervisor',
    verificarInstituicao: 'verificacao-instituicao',
    ativarInstituicao: 'ativacao-instituicao',
    redefinirSenha: 'recuperacao-senha'
  }

  for (const [parametro, token] of Object.entries(casos)) {
    const tokens = lerTokensDaUrl(`#${parametro}=${encodeURIComponent(token)}`)
    assert.equal(tokens[parametro], token)
    assert.equal(possuiTokenSensivel(tokens), true)
  }
})

test('distingue documentos legais de tokens sensiveis', () => {
  const termos = lerTokensDaUrl('#termos')
  const privacidade = lerTokensDaUrl('#privacidade')

  assert.equal(termos.documentoLegal, 'termos')
  assert.equal(privacidade.documentoLegal, 'privacidade')
  assert.equal(possuiTokenSensivel(termos), false)
  assert.equal(possuiTokenSensivel(privacidade), false)
})
