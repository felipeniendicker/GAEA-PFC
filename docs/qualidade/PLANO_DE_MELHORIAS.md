# Plano de melhorias do GAEA

Revisão estática da branch `entrega0911` em 02/11/2026. Nenhum teste foi executado nesta revisão.

## Prioridade alta

- [ ] Verificar se o cadastro público permite escolher perfil `INSTITUICAO`; restringir privilégios administrativos se necessário.
- [ ] Revisar autorização por perfil e impedir acesso a processos de outros usuários.
- [ ] Separar configurações de desenvolvimento e produção: revisar `ddl-auto=update` e `show-sql=true`.
- [ ] Verificar concorrência nas transições de status e consistência do histórico e auditoria.
- [ ] Conferir força, armazenamento e rotação do segredo JWT.

## Prioridade média

- [ ] Criar matriz de requisitos funcionais, não funcionais e regras de negócio.
- [ ] Padronizar DTOs de entrada e saída e adicionar paginação às listagens.
- [ ] Testar todos os fluxos na interface e as respostas a erros.
- [ ] Especificar as permissões futuras do perfil `EMPRESA`.
- [ ] Definir bases legais, retenção, descarte e atendimento aos titulares de dados conforme a LGPD.
- [ ] Criar pipeline de CI para testes do backend e build/lint do frontend.

## Critérios para consolidar na main

1. Testes e build executados e aprovados.
2. Revisão de segurança e permissões.
3. Documentação consistente com a implementação.
4. Revisão do pull request antes do merge.
