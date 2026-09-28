# Integração com ViaCEP

## Objetivo da integração

O GAEA utiliza a API ViaCEP para consultar dados de endereço a partir do CEP informado durante o preenchimento dos dados da empresa relacionados ao processo de estágio.

A consulta evita a digitação manual de informações como logradouro, bairro, cidade e estado.

## API utilizada

- **Serviço:** ViaCEP
- **Endereço:** https://viacep.com.br
- **Autenticação externa:** a API não exige chave ou token.

## Endpoint interno do GAEA

```http
GET /api/cep/{cep}
```

O endpoint aceita o CEP com oito números ou com hífen:

```text
01001000
01001-000
```

## Endpoint externo do ViaCEP

Depois da normalização e validação, o backend consulta:

```http
GET https://viacep.com.br/ws/{cep}/json/
```

Exemplo:

```http
GET https://viacep.com.br/ws/01001000/json/
```

## Autenticação e permissão

Embora o ViaCEP seja público, o endpoint interno do GAEA é protegido por JWT. Somente um usuário autenticado com o perfil `INSTITUICAO` pode acessar `/api/cep/{cep}`.

O frontend envia o token no cabeçalho:

```http
Authorization: Bearer <token>
```

## Funcionamento

1. O backend recebe o CEP informado.
2. Espaços e hífens são removidos.
3. O valor normalizado deve conter exatamente oito números.
4. O backend consulta o ViaCEP por meio de uma requisição GET.
5. A resposta é convertida para `ViaCepResponse`.
6. Se o CEP existir, os dados necessários são devolvidos ao frontend.

O frontend não acessa diretamente a API externa.

## Dados utilizados

O GAEA utiliza os seguintes campos da resposta:

- CEP;
- logradouro;
- complemento;
- bairro;
- localidade;
- UF.

O DTO também recebe o campo `erro`, utilizado para identificar um CEP inexistente. Outros campos retornados pelo ViaCEP são ignorados.

## Exemplo de requisição ao GAEA

```http
GET http://localhost:8080/api/cep/01001-000
Authorization: Bearer <token>
```

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
```

## Tratamento de erros

O backend trata os seguintes cenários:

- **CEP com formato inválido:** retorna `400 Bad Request` com a mensagem `CEP inválido`;
- **CEP inexistente:** quando o ViaCEP retorna `{"erro": true}`, o GAEA retorna `404 Not Found` com a mensagem `CEP não encontrado`;
- **falha na comunicação:** se o ViaCEP estiver indisponível ou ocorrer erro durante a chamada externa, o GAEA retorna `503 Service Unavailable` com a mensagem `Serviço de consulta de CEP indisponível`.

Assim, um CEP inexistente não é apresentado como um endereço vazio e exceções técnicas da integração não são expostas ao usuário.

## Privacidade e LGPD

O GAEA envia ao ViaCEP somente o CEP necessário para localizar o endereço. Senhas, tokens JWT, dados completos do processo de estágio e outras informações da conta não são enviados ao serviço externo.

O uso do ViaCEP e a finalidade da consulta também são informados na Política de Privacidade do GAEA.

## Fluxo resumido

```text
Usuário INSTITUICAO
        ↓
Frontend envia CEP e JWT
        ↓
Spring Security valida acesso
        ↓
Backend normaliza e valida o CEP
        ↓
Backend consulta o ViaCEP
        ↓
Resposta válida → endereço para o frontend
CEP inexistente → HTTP 404
Falha externa → HTTP 503
```
