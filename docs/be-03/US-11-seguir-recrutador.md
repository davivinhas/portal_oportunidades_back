# US-11 — Seguir e deixar de seguir recrutador

## Implementação

O módulo `profile` contém `RecruiterFollow`, `RecruiterFollowRepository`,
`StudentRepository`, `RecruiterFollowService`, `RecruiterFollowController` e o DTO
`FollowingResponse`. O vínculo usa UUID; os identificadores dos perfis continuam
`Long`, conforme as entidades existentes. Não há corpo de requisição nem necessidade
de mapper, pois a resposta contém apenas a situação do vínculo.

| Método | Rota | Resposta |
|---|---|---|
| PUT | `/api/students/{studentId}/following/{recruiterId}` | 204 |
| DELETE | `/api/students/{studentId}/following/{recruiterId}` | 204 |
| GET | `/api/students/{studentId}/following/{recruiterId}` | 200, `{"following":true}` ou `{"following":false}` |

Todas as operações validam a existência dos dois perfis e retornam o erro
padronizado 404 quando algum deles não existe. O PUT usa `ON CONFLICT DO NOTHING`;
o DELETE remove o vínculo se existir. Repetições mantêm o mesmo resultado. A
transação da operação garante commit antes de retornar ao cliente.

O changeset `z20261009-recruiter-follow.yaml` cria `perfil.recruiter_follow`, com
UUID, chaves estrangeiras para aluno e recrutador, data de criação, unicidade do
par e índice para consultas por recrutador. Não altera changesets históricos.
A restrição única também protege contra requisições simultâneas. Excluir perfis
com vínculos é impedido pelas chaves estrangeiras; não há exclusão em cascata.

O recrutador não precisa estar autorizado a publicar para ser seguido: a US-11
exige apenas a existência do perfil. A funcionalidade não envia notificações.

## Testes

`RecruiterFollowServiceTest` cobre perfis inexistentes, consulta e operações
repetidas. `RecruiterFollowControllerTest` cobre rotas, códigos HTTP, resposta
JSON, erro padronizado e identificadores inválidos. Esses testes de controller
validam o contrato HTTP, sem verificar autenticação.

`RecruiterFollowIntegrationIT` usa PostgreSQL 17 descartável com Testcontainers,
Liquibase e Hibernate em modo validate. Cobre seguir, consultar, deixar de seguir,
seguir novamente, aluno inexistente e duas requisições concorrentes em transações
independentes, com apenas um vínculo persistido.

```bash
./mvnw test
./mvnw verify -Pintegration-tests
```

## Pendências

O identificador do aluno é fornecido na rota por decisão desta etapa. A configuração
de segurança existente permanece em vigor, mas ainda não relaciona a identidade
autenticada ao perfil informado. A existência de um perfil de aluno não comprova
a identidade de quem chamou a API. A restrição a usuários ALUNO e a validação de
propriedade ficam pendentes da BE-01; o aceite de autorização da US-11 ainda não
está concluído. Na integração, substituir `{studentId}` por `/me` e obter o perfil
pelo contexto autenticado.

A migration deve ser revisada e aplicada explicitamente ao Supabase quando o
acesso estiver disponível. Os testes não acessam nem alteram o banco compartilhado.
