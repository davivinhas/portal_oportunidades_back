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

A atualização de status existe como regra da entidade e é testada. Não foi criado
endpoint de gestão de candidatos pelo recrutador, pois essa API não está definida
nas subtarefas da BE-04. O futuro service deve verificar recrutador autorizado e
propriedade da oportunidade antes de invocar updateStatus.

Não foram criadas notificações, entrevistas, pontuação ou histórico.

## Testes

Testes parametrizados cobrem estados iniciais, finais, regressões e cancelamento.
Service/controller verificam detalhe, propriedade por ID e cancelamento.
Integração confirma persistência de CANCELLED e que uma aprovação concorrente
vence um cancelamento carregado com versão antiga, causando rollback deste último.

## Pendência

Autenticação e autorização pelo usuário real permanecem pendentes da BE-01.
