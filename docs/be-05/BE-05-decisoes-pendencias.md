# BE-05 — Decisões e pendências de integração

## Decisões desta implementação

- Código, enums e contratos da API usam inglês.
- A entidade mantém os nomes atuais das tabelas e colunas por meio das anotações JPA.
- Services recebem `recruiterId` explicitamente até a implementação da autenticação.
- Regras de propriedade permanecem no service.
- Criação gera rascunho e publicação é uma operação separada.
- Edição dos dados é permitida somente em rascunho.
- Encerramento repetido é idempotente.

## Infraestrutura adicionada

- MapStruct e annotation processor configurados no Maven.
- Maven Wrapper marcado como executável no Linux.
- Profile `test` desabilita banco, Liquibase, Redis e Docker Compose no teste de contexto.
- Extensões recomendadas do VS Code registradas em `.vscode/extensions.json`.
- Respostas de erro uniformes para validação, recurso inexistente, propriedade e regra de negócio.

## Pendências bloqueadas por outras entregas

1. Integrar os endpoints com o principal autenticado e substituir as rotas temporárias por `/me`.
2. Reconciliar o tratamento global de erros com a tarefa transversal BE-00.2.
3. Criar ou receber os changesets Liquibase iniciais antes de validar o mapeamento JPA.
4. Confirmar os valores dos enums PostgreSQL com os novos valores em inglês.
5. Executar testes de integração com PostgreSQL e Liquibase.
6. Definir o fluxo administrativo para `PENDING_APPROVAL` e `REJECTED`.
7. Integrar o bloqueio de candidatura com `canReceiveApplications`.

## Compatibilidade de banco

Os nomes das tabelas e colunas continuam em português para compatibilidade com o modelo existente. Os valores dos enums Java foram normalizados para inglês; os changesets deverão criar os tipos PostgreSQL com os mesmos valores usados no código.
