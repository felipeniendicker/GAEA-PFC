# Política de Privacidade do GAEA

**Versão:** 1.0  
**Data:** 27/09/2026

## 1. Sobre o GAEA

O GAEA (Gestão e Acompanhamento de Estágios Acadêmicos) é um projeto acadêmico desenvolvido com o objetivo de auxiliar no gerenciamento e acompanhamento de processos de estágio.

Esta Política de Privacidade apresenta quais dados podem ser tratados pelo sistema, para quais finalidades são utilizados e quais medidas são adotadas para sua proteção.

## 2. Dados tratados pelo sistema

De acordo com as funcionalidades disponíveis no GAEA, poderão ser tratados os seguintes dados:

### Dados da conta
- nome;
- endereço de e-mail;
- senha, armazenada de forma protegida por hash;
- perfil de acesso do usuário.

### Dados relacionados ao processo de estágio
- nome do aluno;
- e-mail do aluno;
- nome da empresa;
- CNPJ da empresa;
- data de início do estágio;
- data de término do estágio;
- situação do processo de estágio.

### Dados de endereço
O sistema poderá utilizar o CEP da empresa para realizar a consulta de endereço por meio da integração com o ViaCEP.

### Dados de auditoria
Para permitir a rastreabilidade de determinadas operações, o GAEA registra informações como:
- usuário responsável pela ação;
- ação realizada;
- recurso relacionado à operação;
- data e horário da ação.

Senhas e tokens de autenticação não devem ser armazenados nos registros de auditoria.

## 3. Finalidades do tratamento

Os dados são utilizados de acordo com as funcionalidades do GAEA, principalmente para:

- identificar e autenticar usuários;
- controlar o acesso às funcionalidades de acordo com o perfil;
- cadastrar e acompanhar processos de estágio;
- relacionar informações do aluno e da empresa ao processo;
- consultar informações de endereço da empresa;
- registrar ações relevantes para auditoria e rastreabilidade;
- auxiliar no funcionamento e na segurança do sistema.

O GAEA não deve solicitar dados pessoais que não sejam necessários para suas funcionalidades.

## 4. Autenticação e segurança

As senhas cadastradas no sistema são protegidas utilizando BCrypt e não devem ser armazenadas em texto puro.

Após a autenticação, o sistema utiliza JWT (JSON Web Token) para identificar o usuário durante o acesso às funcionalidades protegidas.

O controle de acesso é realizado de acordo com o perfil do usuário, incluindo os perfis ALUNO, EMPRESA e INSTITUICAO.

O GAEA também utiliza registros de auditoria para permitir o acompanhamento de determinadas ações realizadas no sistema.

Nenhuma medida de segurança elimina completamente os riscos relacionados ao uso de sistemas de informação. Em uma implantação de produção, também deverão ser adotadas medidas adequadas de infraestrutura, incluindo o uso de HTTPS e a proteção das credenciais e segredos utilizados pela aplicação.

## 5. Compartilhamento e serviços externos

O GAEA utiliza o ViaCEP como serviço externo para consulta de endereço.

Durante essa consulta, somente o CEP necessário para localizar o endereço é enviado ao serviço ViaCEP.

Dados de autenticação, senhas, tokens e informações completas do processo de estágio não são enviados ao ViaCEP.

O uso de outros serviços externos deverá ser informado nesta Política caso novas integrações sejam adicionadas ao sistema.

## 6. Armazenamento e retenção

Os dados são armazenados enquanto forem necessários para o funcionamento e acompanhamento dos processos existentes no sistema.

Por se tratar de um projeto acadêmico em desenvolvimento, ainda não existe uma rotina automática definitiva para exclusão dos dados após determinado período.

Em uma implantação real, os períodos de retenção e os procedimentos de descarte deverão ser definidos pela instituição responsável pelo uso do sistema, considerando a finalidade do tratamento e as obrigações aplicáveis.

## 7. Direitos relacionados aos dados pessoais

O titular dos dados poderá solicitar informações e exercer os direitos aplicáveis previstos na legislação de proteção de dados, incluindo solicitações relacionadas ao acesso, correção e eliminação de dados, quando aplicável.

As solicitações relacionadas aos dados utilizados no projeto GAEA poderão ser encaminhadas pelo canal de contato informado nesta Política.

## 8. Canal de contato

Para dúvidas ou solicitações relacionadas à privacidade e ao tratamento de dados no projeto GAEA:

**E-mail:** feliperafaelniendicker@gmail.com

Este endereço é apresentado como canal de contato do projeto acadêmico e não representa a indicação formal de um Encarregado pelo Tratamento de Dados Pessoais (DPO).

## 9. Responsabilidade pelo tratamento

O GAEA é atualmente um projeto acadêmico e não possui uma instituição formalmente definida como controladora dos dados para uma implantação em produção.

Caso o sistema seja implantado para utilização real, as responsabilidades relacionadas ao tratamento dos dados deverão ser definidas entre a instituição responsável pelo uso do sistema e os demais envolvidos.

## 10. Alterações desta Política

Esta Política poderá ser atualizada conforme novas funcionalidades ou formas de tratamento de dados sejam adicionadas ao GAEA.

Quando houver alterações relevantes, a versão e a data deste documento deverão ser atualizadas.

**Versão atual: 1.0 — 27/09/2026**