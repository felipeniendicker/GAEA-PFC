import test from 'node:test'
import assert from 'node:assert/strict'
import {
  cnpjValido,
  erroCnpj,
  formatarCnpj,
  possuiErros,
  validarSolicitacaoInstituicao
} from './validacao.js'

test('formata CNPJ digitado ou colado e limita caracteres excedentes', () => {
  assert.equal(formatarCnpj('11222333000181'), '11.222.333/0001-81')
  assert.equal(formatarCnpj('11.222.333/0001-81'), '11.222.333/0001-81')
  assert.equal(formatarCnpj('112223330001819999'), '11.222.333/0001-81')
})

test('distingue CNPJ incompleto, inválido e válido', () => {
  assert.equal(erroCnpj('11.222'), 'CNPJ incompleto. Informe os 14 dígitos.')
  assert.equal(erroCnpj('11.111.111/1111-11'), 'CNPJ inválido. Confira os dígitos verificadores.')
  assert.equal(erroCnpj('11.222.333/0001-81'), '')
  assert.equal(cnpjValido('11.222.333/0001-81'), true)
})

test('identifica todos os impedimentos da solicitação institucional', () => {
  const erros = validarSolicitacaoInstituicao({
    nomeInstitucional: '',
    cnpj: '11.222',
    responsavelNome: '',
    emailInstitucional: 'invalido'
  }, false)

  assert.equal(possuiErros(erros), true)
  assert.deepEqual(Object.keys(erros).filter((campo) => erros[campo]), [
    'nomeInstitucional', 'cnpj', 'responsavelNome', 'emailInstitucional', 'termos'
  ])
})

test('aceita formulário institucional completo', () => {
  const erros = validarSolicitacaoInstituicao({
    nomeInstitucional: 'Faculdade Exemplo',
    cnpj: '11.222.333/0001-81',
    responsavelNome: 'Ana Souza',
    emailInstitucional: 'ana@example.com'
  }, true)

  assert.equal(possuiErros(erros), false)
})
