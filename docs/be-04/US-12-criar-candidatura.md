# US-12 — Criar candidatura

## API

`POST /api/opportunities/{opportunityId}/applications`

```json
{"studentId": 123}
```

Retorna 201 com ApplicationResponse e Location apontando para o detalhe em
`/api/students/{studentId}/applications/{applicationId}`.

studentId é obrigatório e positivo. A candidatura exige perfil de aluno existente;
usuário sem perfil não é suficiente. Aluno ou oportunidade inexistente retorna 404.
Oportunidade excluída logicamente também retorna 404. Oportunidade não publicada,
encerrada ou fora do período retorna erro de negócio padronizado 422.

A candidatura nasce em SUBMITTED. appliedAt usa o Clock injetado. createdAt e updatedAt
são mantidos pelo Hibernate. Identificadores Long são preservados para compatibilidade
com a tabela existente. Respostas usam DTOs e não expõem entidades JPA nem dados de senha.

## Concorrência com encerramento

A criação carrega a oportunidade com PESSIMISTIC_WRITE e mantém o bloqueio até o commit.
O instante da inscrição é obtido após adquirir o bloqueio. A validação do período
reutiliza Opportunity.canReceiveApplications, incluindo os instantes inicial e final.

O UPDATE do encerramento/exclusão também disputa o bloqueio da mesma linha:

- Se o encerramento confirmar primeiro, a candidatura lê CLOSED e é rejeitada.
- Se a candidatura adquirir o bloqueio primeiro, ela confirma antes do encerramento.

A estratégia serializa inscrições por oportunidade durante suas transações. É uma
decisão de consistência do MVP; observar contenção antes de aumentar o volume de uso.
Não há reserva de vagas, entrevistas, avaliações ou cálculo de compatibilidade nesta entrega.

## Testes

Testes de entidade cobrem estado inicial, limites do período, rascunho, encerramento e
exclusão. Testes de service/controller cobrem perfil, validação HTTP, status e Location.
ApplicationIntegrationIT verifica auditoria e a disputa real entre criação e encerramento
nas duas ordens, observando espera de bloqueio no PostgreSQL.

## Autenticação

studentId é explícito por decisão desta etapa. A identidade e o papel do solicitante
ainda não são vinculados ao perfil informado. Integrar com BE-01 antes de uso em produção.
