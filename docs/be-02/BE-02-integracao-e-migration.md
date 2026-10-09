# BE-02 — Integração, migration e revisão

## Migration

z20261009-student-profile.yaml altera somente as tabelas de perfil:

- Converte resumo, habilidades e interesses do aluno e descrição da experiência para TEXT.
- Adiciona perfil.aluno.version BIGINT NOT NULL DEFAULT 0.
- Adiciona constraint de período positivo quando informado.
- Adiciona constraint de coerência entre experiência atual e término.

As chaves estrangeiras, índice por aluno e unicidade da matrícula já existem nos
changesets anteriores e são reutilizados. Nenhum changeset aplicado foi modificado.

Foi gerado um draft com o goal liquibase:diff em PostgreSQL 17 isolado. Nesta versão
do plugin (5.0.3), o goal diffChangeLog não existe; diff com diffChangeLogFile
produz o draft. A comparação automática sugeriu recriação de tabelas por diferenças
de schema e não foi aplicada. O changeset versionado contém apenas os ajustes revisados
necessários à BE-02, incluindo constraints que o diff não infere.

O destino padrão do draft no pom agora é target/liquibase/generated-diff.yaml,
sobrescrevível com -Dliquibase.diffChangeLogFile=..., para evitar alterações acidentais
no changeset histórico generated-diff.yaml. Drafts não entram no includeAll das migrations.

O SQL final foi gerado contra o esquema anterior no container isolado e revisado.
Foi aplicado nesse banco existente: um changeset novo executado, 14 anteriores preservados.
As colunas TEXT e version foram conferidas, e o container de revisão foi parado ao final.
Os testes de integração aplicam todos os changesets com Hibernate em validate.

Rollback remove versão e constraints, mas mantém os campos TEXT para evitar truncar
dados maiores que 255 caracteres. Descrever e revisar esse rollback parcial antes de uso.

## Aplicação no Supabase: pendente

A migration não foi aplicada ao banco compartilhado. Antes da aplicação, verificar
se dados existentes violam as novas constraints:

```sql
SELECT usuario_id, periodo
FROM perfil.aluno
WHERE periodo <= 0;

SELECT id, atual, data_inicio, data_fim
FROM perfil.experiencia_profissional
WHERE (atual AND data_fim IS NOT NULL)
   OR (NOT atual AND (data_fim IS NULL OR data_fim < data_inicio));
```

Caso haja resultados, revisar a correção com a equipe antes de aplicar. A migration
não corrige nem apaga registros silenciosamente. Gerar updateSQL contra o Supabase,
revisar os changesets pendentes e executar update explicitamente quando o acesso estiver disponível.
Não usar o banco compartilhado nos testes.

## Verificação

```bash
./mvnw test
./mvnw verify -Pintegration-tests
```

Testcontainers inicia PostgreSQL 17 descartável e aplica Liquibase. Os testes cobrem
também rollback, experiências de outro aluno, unicidade e conflitos de versão.
A geração de PDF é verificada pelo conteúdo extraído do arquivo e pela paginação.
Resultado da execução: 46 testes unitários/de aplicação e 12 testes de integração,
sem falhas, erros ou testes ignorados. Build finalizado com sucesso.

## Integração entre branches

Esta branch não contém a implementação da US-11. Ao integrar as duas entregas, preservar
os métodos de StudentRepository usados pela BE-02 e compatibilizar os mocks do teste
de contexto. Os campos adicionados às entidades não alteram o identificador dos perfis.
