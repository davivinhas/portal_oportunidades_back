# US-13 — Impedir candidatura duplicada

## Regra

O par aluno/oportunidade é único durante toda a existência da candidatura.
Cancelamento, aprovação ou rejeição não liberam uma nova candidatura. Essa regra
segue a unicidade absoluta prevista na tarefa; não é um índice apenas para estados ativos.

ApplicationService valida antecipadamente a existência do par e retorna ConflictException,
mapeada pelo handler global para HTTP 409. A migration cria a constraint
uk_application_student_opportunity. Violações dessa constraint em saveAndFlush são
traduzidas para o mesmo erro; outras violações não são mascaradas como duplicação.

O bloqueio da oportunidade serializa criações concorrentes neste fluxo. A constraint
protege também inserções que não passem pelo service.

## Testes

- Repetição da criação retorna conflito.
- Após cancelamento, nova criação continua bloqueada.
- Duas requisições concorrentes criam exatamente um registro: uma confirma e outra retorna conflito.
- Inserção direta duplicada é rejeitada pelo PostgreSQL.
- Teste unitário verifica a tradução seletiva da constraint.
- Controller retorna erro padronizado 409.

## Banco compartilhado

Antes de aplicar a migration, consultar duplicações existentes:

```sql
SELECT aluno_id, oportunidade_id, count(*)
FROM oportunidades.candidatura
GROUP BY aluno_id, oportunidade_id
HAVING count(*) > 1;
```

Se houver resultados, revisar os registros com a equipe. A migration não apaga nem
consolida dados silenciosamente.
