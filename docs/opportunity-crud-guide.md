# Oportunidades: consulta pública e gestão pelo recrutador

Este documento descreve o contrato implementado. A gestão de oportunidades segue as rotas e regras do PR BE-05; a consulta pública e a paginação por cursor foram acrescentadas neste PR. Não é um roteiro para copiar arquivos.

## Rotas

| Método | Rota | Acesso | Resultado |
| --- | --- | --- | --- |
| GET | `/api/opportunities` | Público | Lista oportunidades publicadas por cursor |
| GET | `/api/opportunities/{id}` | Público | Detalha uma oportunidade publicada |
| POST | `/api/recruiters/{recruiterId}/opportunities` | Autenticado | Cria rascunho para recrutador autorizado |
| GET | `/api/recruiters/{recruiterId}/opportunities` | Autenticado | Lista oportunidades próprias não excluídas |
| GET | `/api/recruiters/{recruiterId}/opportunities/{opportunityId}` | Autenticado | Detalha oportunidade própria |
| PUT | `/api/recruiters/{recruiterId}/opportunities/{opportunityId}` | Autenticado | Atualiza rascunho próprio |
| POST | `/api/recruiters/{recruiterId}/opportunities/{opportunityId}/publish` | Autenticado | Publica rascunho próprio |
| POST | `/api/recruiters/{recruiterId}/opportunities/{opportunityId}/close` | Autenticado | Encerra oportunidade própria publicada |
| DELETE | `/api/recruiters/{recruiterId}/opportunities/{opportunityId}` | Autenticado | Exclui logicamente oportunidade própria |

As rotas de escrita permanecem sob o recurso do recrutador e não aceitam `recruiterId` no body. O service verifica a existência e autorização do recrutador na criação e a propriedade antes das operações sobre uma oportunidade. O Spring Security mantém seu usuário temporário em memória; não há login próprio, JWT nem busca de usuário autenticado no banco nesta fase. As consultas públicas são `permitAll`; as demais rotas exigem usuário autenticado.

## Listagem e cursor

`GET /api/opportunities` aceita:

| Parâmetro | Regra |
| --- | --- |
| `title` | Busca parcial sem diferenciar maiúsculas; `%`, `_` e `\\` são tratados literalmente |
| `modality` | Um valor de `OpportunityModality` |
| `status` | Consulta pública aceita somente `PUBLISHED`; outros status produzem lista vazia |
| `recruiterId` | ID positivo |
| `limit` | Padrão 20; mínimo 1, máximo 100 |
| `cursor` | Opaque; deve ser enviado junto dos mesmos filtros |

Sem `status`, a consulta também retorna somente `PUBLISHED`. Resultados são ordenados por `createdAt DESC, id DESC`. A resposta contém `items`, `hasNext` e `nextCursor`; não calcula total nem usa página/offset. O cursor é Base64 URL-safe com versão, timestamp, ID e impressão dos filtros. Não é autenticação. Um cursor inválido ou usado com filtros diferentes retorna erro 400. A consulta busca um registro adicional para determinar `hasNext`, mas o cursor aponta para o último item entregue.

O detalhe público retorna 404 para ID inexistente, excluído ou ainda não publicado. A listagem própria do recrutador continua sem cursor, conforme o contrato do PR BE-05, e não é duplicada na rota pública.

## Regras de domínio

- Enum values são os nomes em inglês definidos pelo PR BE-05 (`DRAFT`, `PUBLISHED`, `CLOSED`; modalidades como `INTERNSHIP` e `SCIENTIFIC_INITIATION`).
- Uma oportunidade é criada como `DRAFT`; somente recrutador autorizado pode criar ou publicar.
- Apenas `DRAFT` pode ser editado. `PUBLISHED` pode ser encerrada; fechar novamente é idempotente conforme o domínio do PR BE-05.
- O fim da inscrição deve ser posterior ao início; a publicação exige prazo final futuro.
- Exclusão é lógica (`deleted_at`), preserva candidaturas e oculta a oportunidade das consultas. Exclusões repetidas retornam 404.
- `Opportunity.version` usa `@Version`; conflito otimista retorna 409. Regras de estado retornam 422, ausência 404, propriedade negada 403 e validação/cursor inválido 400, no envelope `ApiErrorResponse` do PR BE-05.
- Controllers tratam HTTP; services coordenam autorização, consultas e transações; entidades mantêm as transições de estado; MapStruct transforma DTOs.

## Persistência e execução

Liquibase é a única forma de alterar o schema. Changesets existentes, inclusive `20260916-opportunity-crud` e `20260928-normalize-domain-enums`, foram mantidos. A coluna `version BIGINT NOT NULL DEFAULT 0` é adicionada pelo changeset versionado `20261002-opportunity-version`. Hibernate roda com `spring.jpa.hibernate.ddl-auto=validate`; o teste de integração sobe PostgreSQL 17, aplica as migrations e valida o schema ao iniciar o contexto.

O starter de Security continua no projeto. O Spring Boot gera um usuário temporário e uma senha para desenvolvimento; não use essa senha em testes. Testes HTTP enviam usuários simulados com `spring-security-test`.

Com Java 21:

```shell
./mvnw test
./mvnw verify -Pintegration-tests
```

O segundo comando usa Testcontainers e requer Docker disponível. O perfil Maven é `integration-tests`; o perfil Spring dos testes é `integration-test`.
