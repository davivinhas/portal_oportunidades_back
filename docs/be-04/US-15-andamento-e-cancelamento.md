# US-15 — Andamento e cancelamento

## APIs

- `GET /api/students/{studentId}/applications/{applicationId}`: 200 com estado atual.
- `POST /api/students/{studentId}/applications/{applicationId}/cancel`: 204 sem corpo.

O repository busca o detalhe pelo par candidatura/aluno. Registros inexistentes ou
pertencentes a outro aluno retornam 404, sem revelar dados do outro perfil.

## Estados e transições

Application.updateStatus permite:

- SUBMITTED → UNDER_REVIEW, APPROVED ou REJECTED.
- UNDER_REVIEW → APPROVED ou REJECTED.

Repetir o estado atual é uma operação sem alteração. Regressões e mudanças de estados
finais são rejeitadas. Não há etapas configuráveis nem histórico de status.

Application.cancel permite SUBMITTED/UNDER_REVIEW → CANCELLED.
Repetir cancelamento retorna sucesso, preservando CANCELLED. APPROVED e REJECTED não
podem ser cancelados. Violações de negócio retornam 422.

A entidade possui @Version; cancelamento desatualizado não pode sobrescrever aprovação
concorrente. Conflitos de versão retornam HTTP 409. Cancelar não apaga o registro e não
permite nova candidatura à mesma oportunidade.

A proteção detecta transações concorrentes. Não há versão enviada pelo cliente para
comparar um formulário antigo após outra transação já ter terminado.

## Limites da entrega

A gestão de candidatos está disponível para o recrutador autorizado proprietário:

- `GET /api/recruiters/{recruiterId}/opportunities/{opportunityId}/applications` lista candidaturas, com filtro opcional `status` e paginação `page` e `size`.
- `PATCH /api/recruiters/{recruiterId}/opportunities/{opportunityId}/applications/{applicationId}/status` atualiza o status com corpo `{"status":"UNDER_REVIEW"}`.

A identidade autenticada deve corresponder ao recrutador da rota. O serviço verifica
a autorização do recrutador, a propriedade da oportunidade e o vínculo da candidatura.

Não foram criadas notificações, entrevistas ou pontuação. O histórico de status e as etapas seletivas configuráveis por candidatura estão disponíveis nas rotas do recrutador.

## Testes

Testes parametrizados cobrem estados iniciais, finais, regressões e cancelamento.
Service/controller verificam detalhe, propriedade por ID e cancelamento.
Integração confirma persistência de CANCELLED e que uma aprovação concorrente
vence um cancelamento carregado com versão antiga, causando rollback deste último.
