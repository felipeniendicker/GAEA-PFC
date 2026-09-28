# Política de Privacidade do GAEA

**Versão:** 1.0
**Data:** 27/09/2026

Esta Política apresenta como o GAEA trata dados pessoais, conforme a Lei nº 13.709/2018 (Lei Geral de Proteção de Dados Pessoais — LGPD).

## 1. Sobre esta política

O GAEA (Gestão e Acompanhamento de Estágios Acadêmicos) é um sistema acadêmico voltado à gestão e ao acompanhamento de processos de estágio. Esta Política explica quais dados são utilizados, suas finalidades, o tratamento realizado e os direitos dos titulares.

## 2. Dados tratados pelo GAEA

De acordo com as funcionalidades atuais, o sistema trata:

- nome, e-mail e perfil de acesso (ALUNO, EMPRESA ou INSTITUICAO);
- senha protegida por hash BCrypt, sem armazenamento em texto puro;
- nome e e-mail do aluno relacionados ao processo de estágio;
- nome e CNPJ da empresa;
- datas de início e fim e status do processo de estágio;
- CEP e dados de endereço consultados por meio do ViaCEP;
- registros de auditoria, incluindo usuário, ação realizada, recurso e data/hora;
- registro do aceite dos Termos de Uso e da Política de Privacidade e sua data/hora.

O GAEA não coleta atualmente CPF, telefone ou documentos pessoais.

## 3. Finalidades

Os dados são utilizados para:

- criar e identificar a conta do usuário;
- autenticar o usuário e controlar o acesso conforme seu perfil;
- diferenciar as permissões dos perfis ALUNO, EMPRESA e INSTITUICAO;
- cadastrar e acompanhar processos de estágio;
- identificar o aluno e a empresa envolvidos no processo;
- consultar o endereço da empresa a partir do CEP;
- registrar ações importantes para rastreabilidade e segurança;
- registrar o aceite dos Termos de Uso e da Política de Privacidade.

## 4. Tratamento e segurança

Os dados são recebidos e persistidos pelo backend e pelo banco de dados do GAEA. As senhas são protegidas com BCrypt, a autenticação utiliza JWT e a autorização conforme o perfil é verificada no backend.

O sistema também mantém registros de auditoria e se comunica com o ViaCEP para consultar endereços. Senhas e tokens de autenticação não são armazenados nos registros de auditoria.

## 5. Serviço externo ViaCEP

O GAEA utiliza o ViaCEP para consultar dados de endereço. Durante a consulta, somente o CEP necessário é enviado ao serviço externo.

Senhas, tokens de autenticação e dados completos do processo de estágio não são enviados ao ViaCEP.

## 6. Retenção

Os dados são mantidos enquanto forem necessários às funcionalidades e à finalidade acadêmica do sistema. Como o projeto ainda não possui uma rotina automática de exclusão por prazo, não é definido um período fixo de retenção.

Em um ambiente real, regras específicas de retenção e descarte deverão observar as finalidades do tratamento e as obrigações aplicáveis.

## 7. Seus direitos

Conforme aplicável, o titular pode solicitar:

- confirmação da existência de tratamento;
- acesso aos seus dados;
- correção de dados incompletos, inexatos ou desatualizados;
- informações sobre o tratamento realizado;
- eliminação de dados tratados com consentimento, quando aplicável e observadas as hipóteses legais;
- revogação do consentimento, quando ele for a base aplicável.

Essas solicitações devem ser encaminhadas pelo canal de contato do projeto. O sistema não possui atualmente uma função de exclusão automática disponível ao usuário.

## 8. Contato

Dúvidas e solicitações relacionadas à privacidade podem ser enviadas ao canal de contato do projeto GAEA:

**E-mail:** feliperafaelniendicker@gmail.com

Esse contato não representa a indicação formal de um DPO ou Encarregado pelo Tratamento de Dados Pessoais.

## 9. Atualização

Esta Política poderá ser atualizada conforme o desenvolvimento do projeto. A versão disponível no sistema deve ser consultada para conhecer o conteúdo vigente.

**Versão atual: 1.0 — 27/09/2026**
