# US-18 — Consultar e editar oportunidades próprias

## Objetivo

Permitir que o recrutador consulte e edite somente oportunidades sob sua responsabilidade.

## Implementação

- Listagem: `GET /api/recruiters/{recruiterId}/opportunities`.
- Detalhamento: `GET /api/recruiters/{recruiterId}/opportunities/{opportunityId}`.
- Atualização: `PUT /api/recruiters/{recruiterId}/opportunities/{opportunityId}`.
- `OpportunityService` carrega a oportunidade e valida a propriedade antes de qualquer operação.
- Tentativas de acesso a recursos de outro recrutador retornam `403 Forbidden`.
- Um recrutador cuja autorização tenha sido revogada não pode editar, publicar ou encerrar oportunidades.
- Somente oportunidades em `DRAFT` podem ter seus dados editados.
- A atualização utiliza dirty checking e não chama `save` para entidades carregadas.
- A entidade usa versionamento otimista com `@Version`. Atualizações concorrentes sobre uma versão
  desatualizada são recusadas e retornam `409 Conflict` pela API.

## Testes

- Consulta de recursos próprios.
- Bloqueio de recurso pertencente a outro recrutador.
- Bloqueio de edição após a publicação.
- Bloqueio de edição para recrutador desautorizado.
- Concorrência entre edição e publicação em transações distintas contra PostgreSQL descartável: a
  edição desatualizada falha e não sobrescreve a publicação.
- Mapeamento de conflito de bloqueio otimista para HTTP `409 Conflict`.
- Listagem e contrato HTTP.

## Pendente

- Adicionar paginação quando o contrato de listagem pública for consolidado.
- Substituir o identificador da rota pelo recrutador autenticado.
