# BE-04 — Integração e migration

## Componentes

Novo módulo application com ApplicationController, ApplicationService, ApplicationRepository,
ApplicationMapper (MapStruct) e DTOs record ApplicationCreateRequest, ApplicationResponse,
ApplicationOpportunityResponse e ApplicationPageResponse.

As entidades Application e ApplicationStatus permanecem no pacote existente opportunity.entity
para preservar compatibilidade. Student ganhou getter de ID e StudentRepository. OpportunityRepository
ganhou consulta com bloqueio para criação. O handler global trata ConflictException e usa mensagem
genérica para conflitos otimistas, aplicável também às candidaturas.

## Migration

z20261009-application-lifecycle.yaml contém dois changesets:

1. Adiciona CANCELLED ao enum nativo oportunidades.status_candidatura.
2. Adiciona version BIGINT NOT NULL DEFAULT 0, unicidade aluno/oportunidade e índice
   composto por aluno, data da candidatura e ID para a listagem.

A alteração do enum é executada fora de transação e antes das alterações da tabela,
permitindo usar o novo valor depois. Changesets históricos não foram modificados.
As chaves estrangeiras existentes são reutilizadas.

Rollback remove índice, constraint única e versão, mas preserva o valor aditivo CANCELLED.
Remover um valor de enum PostgreSQL exigiria reconstrução do tipo e tratamento de registros
cancelados. Revisar esse rollback parcial antes de usá-lo.

## Revisão do draft

O goal disponível no plugin Liquibase 5.0.3 é liquibase:diff com diffChangeLogFile;
diffChangeLog não existe nessa versão. Foi gerado um draft em target/liquibase/be04-draft.yaml.
A comparação automática apresentou alterações destrutivas de tabelas e constraints devido
à divergência de schemas na referência Hibernate; essas sugestões foram descartadas.

O changeset versionado contém apenas as alterações revisadas da BE-04. A geração padrão
de draft agora usa target/liquibase/generated-diff.yaml, sobrescrevível por propriedade Maven,
evitando alterações acidentais no changeset histórico generated-diff.yaml.

## Supabase: aplicação pendente

O banco compartilhado não foi acessado. Antes da aplicação, verificar duplicações com o
SQL documentado na US-13, gerar updateSQL contra o banco compartilhado e revisar todos
os changesets pendentes. Aplicar explicitamente com Liquibase após revisão.

Não corrigir duplicações apagando registros sem alinhamento com a equipe.

## Testes

```bash
./mvnw test
./mvnw verify -Pintegration-tests
```

ApplicationTest testa regras da entidade. ApplicationServiceTest testa orquestração,
erros, filtro, propriedade e tradução seletiva de constraints. ApplicationControllerTest
testa o contrato HTTP com MockMvc standalone.

ApplicationIntegrationIT usa PostgreSQL 17 descartável com Testcontainers, Liquibase
e Hibernate validate. Testa criação, auditoria, cancelamento, consultas, unicidade
direta no banco e concorrência (duplicação, encerramento e aprovação/cancelamento).

Os testes de controller não verificam autenticação. O critério de autorização pelo
solicitante real continua pendente.

Verificação final: 49 testes unitários/de aplicação e 12 testes de integração aprovados,
sem falhas, erros ou testes ignorados. O SQL revisado foi aplicado ao esquema anterior
num PostgreSQL 17 isolado: dois changesets executados e 14 anteriores preservados.
Foram conferidos CANCELLED, a unicidade e o índice composto. O container de revisão
foi parado ao final. Nenhuma alteração foi feita no Supabase.

## Integração com BE-02 e US-11

Esta branch parte da base sem essas duas implementações. No merge, preservar os métodos
do StudentRepository necessários a todas as entregas, o getter de Student e os mocks
dos repositories no teste de contexto. Coordenar eventuais sobreposições com Maurício,
também responsável pela BE-04 no ClickUp.
