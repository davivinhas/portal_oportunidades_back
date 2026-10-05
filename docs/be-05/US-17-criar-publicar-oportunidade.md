# US-17 — Criar e publicar oportunidade

## Objetivo

Permitir que um recrutador autorizado crie uma oportunidade como rascunho e a publique explicitamente.

## Implementação

- A criação valida título, descrição, modalidade, vagas e período de inscrição.
- Toda oportunidade nova recebe o status `DRAFT`.
- `Opportunity.publish()` realiza a transição de `DRAFT` para `PUBLISHED`.
- Recrutadores não autorizados não podem criar nem publicar oportunidades.
- `OpportunityCreateRequest`, `OpportunityResponse` e `OpportunityMapper` formam o contrato da API.
- Criação: `POST /api/recruiters/{recruiterId}/opportunities`.
- Publicação: `POST /api/recruiters/{recruiterId}/opportunities/{opportunityId}/publish`.

## Regras cobertas

- Quantidade de vagas deve ser positiva.
- O término das inscrições deve ser posterior ao início.
- Somente um rascunho pode ser publicado.
- A publicação depende de recrutador autorizado.
- O service valida a propriedade antes da publicação.

## Testes

- Criação no estado de rascunho.
- Publicação válida e inválida.
- Recrutador não autorizado.
- Período de inscrição inválido.
- Validação HTTP e resposta `201 Created` com `Location`.

## Pendente

- Validar a persistência contra PostgreSQL após a entrega dos changesets Liquibase.
- Integrar a identificação do recrutador com a autenticação.
- Definir com a equipe se alguma modalidade exige aprovação administrativa antes da publicação.
