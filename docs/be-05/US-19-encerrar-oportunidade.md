# US-19 — Encerrar oportunidade

## Objetivo

Encerrar explicitamente uma oportunidade sem apagar seus dados ou candidaturas.

## Implementação

- Endpoint: `POST /api/recruiters/{recruiterId}/opportunities/{opportunityId}/close`.
- `Opportunity.close()` permite a transição de `PUBLISHED` para `CLOSED`.
- Repetir o encerramento é uma operação idempotente.
- Outros estados não podem ser encerrados.
- `canReceiveApplications(Instant)` retorna falso após o encerramento.
- A propriedade é validada no service antes da transição.

## Testes

- Encerramento de oportunidade publicada.
- Repetição idempotente.
- Bloqueio de candidaturas após encerramento.
- Resposta `422 Unprocessable Entity` para transição inválida.

## Pendente

- Integrar `canReceiveApplications` ao service de candidaturas quando essa feature estiver disponível.
- Validar em teste de integração que as candidaturas existentes são preservadas.
