# Integração com ViaCEP

## Objetivo

O GAEA utiliza a API ViaCEP para consultar dados de endereço a partir do CEP informado durante o preenchimento de informações relacionadas à empresa do processo de estágio.

A integração evita que dados como cidade, estado, bairro e logradouro precisem ser digitados manualmente.

## API utilizada

Serviço: ViaCEP

Endereço do serviço:
https://viacep.com.br

A API não exige chave de autenticação.

## Endpoint utilizado pelo GAEA

GET /api/cep/{cep}

Exemplo:

GET /api/cep/01001000

O acesso ao endpoint é permitido somente para usuários autenticados com o perfil INSTITUICAO.

## Funcionamento

O CEP informado é recebido pelo backend do GAEA.

Antes da consulta, hífens e espaços são removidos. O sistema verifica se o CEP possui oito números.

Depois da validação, o backend realiza uma requisição GET ao ViaCEP.

Exemplo da requisição externa:

GET /ws/01001000/json/

## Dados utilizados

Da resposta do ViaCEP, o GAEA utiliza:

- CEP;
- logradouro;
- complemento;
- bairro;
- localidade;
- UF.

Outros campos retornados pela API são ignorados.

## Exemplo de resposta

```json
{
  "cep": "01001-000",
  "logradouro": "Praça da Sé",
  "complemento": "lado ímpar",
  "bairro": "Sé",
  "localidade": "São Paulo",
  "uf": "SP"
}