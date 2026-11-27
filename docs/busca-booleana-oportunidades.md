# Guia manual: busca booleana de oportunidades com PostgreSQL

Este documento descreve as mudanças para trocar o parâmetro `title` por `q` e pesquisar título, descrição e requisitos com busca full-text do PostgreSQL. A consulta aceita `AND`, `OR`, `NOT`, parênteses e frases entre aspas. O título recebe o maior peso no ranking. Não são usados embeddings.

> Este é um roteiro para implementar manualmente. Faça uma etapa por vez e confira os nomes de colunas contra os changesets existentes. A consulta de persistência usa jOOQ DSL. Para operadores e funções específicos do PostgreSQL que não tenham uma representação conveniente no DSL, use templates fixos do jOOQ com `QueryPart` e bind values.

## 1. Contrato HTTP e filtro

Atualize `PublicOpportunityController`: troque `title` por `q` e passe `q` para `OpportunityFilter`.

```java
@RequestParam(required = false) @Size(max = 200) String q,
```

```java
return service.listPublic(new OpportunityFilter(q, modality, status, recruiterId), cursor, limit);
```

Renomeie o primeiro campo de `OpportunityFilter` para `query` (ou `q`) e preserve os outros campos. Normalize espaços externos, trate texto vazio como `null` e valide o limite de 200 caracteres. Não converta a consulta para minúsculas: o parser reconhecerá `and`, `AND` etc. sem diferenciar maiúsculas.

```java
public record OpportunityFilter(
        String query,
        OpportunityModality modality,
        OpportunityStatus status,
        Long recruiterId
) {
    public OpportunityFilter {
        query = query == null || query.isBlank() ? null : query.strip();
        if (query != null && query.length() > 200) {
            throw new BadRequestException("Search query must have at most 200 characters");
        }
        if (recruiterId != null && recruiterId < 1) {
            throw new BadRequestException("Recruiter ID must be positive");
        }
    }
}
```

Remova `title` de `OpportunitySpecifications` e do fingerprint do cursor. A busca textual passará por um repositório de busca dedicado; `OpportunitySpecifications` pode continuar apenas se ainda for usado por outra consulta.

## 2. Vetor ponderado e índice GIN

Crie um novo arquivo em `src/main/resources/db/changelog/changes`, por exemplo `z20261007-opportunity-search.yaml`. Não edite changesets já aplicados. `includeAll` no changelog principal já inclui o novo arquivo.

```yaml
databaseChangeLog:
  - changeSet:
      id: 20261007-opportunity-search
      author: linkedufma
      changes:
        - sql:
            splitStatements: true
            sql: |
              ALTER TABLE oportunidades.oportunidade
                ADD COLUMN search_vector tsvector
                GENERATED ALWAYS AS (
                    setweight(to_tsvector('portuguese'::regconfig, coalesce(titulo, '')), 'A') ||
                    setweight(to_tsvector('portuguese'::regconfig, coalesce(descricao, '')), 'B') ||
                    setweight(to_tsvector('portuguese'::regconfig, coalesce(requisitos, '')), 'C')
                ) STORED;

              CREATE INDEX idx_oportunidade_search_vector
                ON oportunidades.oportunidade USING GIN (search_vector);
```

Os pesos do PostgreSQL vão de `D` (menor) a `A` (maior). Portanto, título `A`, descrição `B` e requisitos `C` garantem a prioridade pedida. A coluna gerada acompanha automaticamente criação e edição da oportunidade. Não mapeie `search_vector` na entidade JPA; ela é usada pela consulta SQL nativa.

Se a migration reclamar da configuração `portuguese`, confirme que a imagem local de PostgreSQL inclui o dicionário de busca em português. Não habilite extensões de vetores: GIN e full-text são recursos nativos do PostgreSQL.

## 3. Gramática booleana

Use esta gramática para a entrada de `q`:

```text
expression := orExpression
orExpression := andExpression (OR andExpression)*
andExpression := unaryExpression ((AND)? unaryExpression)*
unaryExpression := NOT unaryExpression | primary
primary := WORD | QUOTED_PHRASE | '(' expression ')'
```

Regras resultantes:

- `Java AND estágio`: ambos precisam ocorrer.
- `Java OR estágio`: qualquer termo pode ocorrer.
- `Java NOT estágio`: interpretado como `Java AND NOT estágio`.
- `Java OR pesquisa AND estágio`: `AND` tem precedência sobre `OR`.
- `(Java OR dados) AND estágio`: parênteses alteram a precedência.
- `"iniciação científica" AND bolsa`: aspas mantêm a frase próxima/ordenada.
- Termos adjacentes, como `Java estágio`, equivalem a `Java AND estágio`.

Rejeite expressão vazia após a análise, operadores sem operandos, parênteses desbalanceados, mais de 32 tokens, profundidade maior que 8, e consultas que contenham somente negações. O último limite evita uma busca ampla como `NOT Java`, que poderia selecionar quase todas as vagas.

Implemente um parser recursivo descendente em `opportunity.query.BooleanSearchQueryParser`. O parser deve retornar uma árvore tipada, nunca SQL escrito pelo usuário:

```java
sealed interface SearchNode permits Term, Phrase, And, Or, Not {}
record Term(String value) implements SearchNode {}
record Phrase(String value) implements SearchNode {}
record And(SearchNode left, SearchNode right) implements SearchNode {}
record Or(SearchNode left, SearchNode right) implements SearchNode {}
record Not(SearchNode child) implements SearchNode {}
```

O tokenizer deve produzir tokens `WORD`, `PHRASE`, `AND`, `OR`, `NOT`, `LPAREN`, `RPAREN` e `END`; identificar as três palavras reservadas sem diferenciar maiúsculas; e manter o conteúdo de palavras/frases como valores. Ao encontrar `"`, consuma até a próxima `"`; se não houver fechamento, lance `BadRequestException`. Use o parser da gramática acima: `parseOr()` chama `parseAnd()`, `parseAnd()` consome expressões unárias adjacentes ou separadas por `AND`, `parseUnary()` trata `NOT`, e `parsePrimary()` trata termo, frase e parênteses. Qualquer token inesperado deve resultar em `BadRequestException`.

O resultado da árvore precisa ser convertido em uma expressão jOOQ de `tsquery`. Os templates abaixo são constantes do código; valores da busca entram como bind values, nunca por concatenação:

```text
Term(v)   -> plainto_tsquery('portuguese', ?)
Phrase(v) -> phraseto_tsquery('portuguese', ?)
And(a,b)  -> (compile(a) && compile(b))
Or(a,b)   -> (compile(a) || compile(b))
Not(a)    -> (!!compile(a))
```

Cada texto `v` deve ser passado com `DSL.val(v)`. O parser retorna a árvore tipada, e um método no repositório converte cada nó em `Field<Object>` usando `DSL.field(template, SQLDataType.OTHER, queryParts...)`. Exemplo:

```java
private Field<Object> toTsQuery(SearchNode node) {
    return switch (node) {
        case Term term -> field(
                "plainto_tsquery('portuguese', {0})", SQLDataType.OTHER, val(term.value()));
        case Phrase phrase -> field(
                "phraseto_tsquery('portuguese', {0})", SQLDataType.OTHER, val(phrase.value()));
        case Not not -> field("!!({0})", SQLDataType.OTHER, toTsQuery(not.child()));
        case And and -> field("({0} && {1})", SQLDataType.OTHER,
                toTsQuery(and.left()), toTsQuery(and.right()));
        case Or or -> field("({0} || {1})", SQLDataType.OTHER,
                toTsQuery(or.left()), toTsQuery(or.right()));
    };
}
```

Os operadores `&&`, `||` e `!!` são operadores PostgreSQL para `tsquery`; o parser garante que a estrutura vem apenas desses nós conhecidos. Não gere SQL concatenando valores nem crie um objeto `(sqlFragment, parameters)` manual.

### Esqueleto do parser

Este código mostra o núcleo do parser. Coloque-o em `BooleanSearchQueryParser.java`, acrescente os imports indicados pelo IDE e use a classe de exceção do projeto. O tokenizer reconhece operadores por palavra; nomes de vaga que coincidam com `AND`, `OR` ou `NOT` devem ser escritos entre aspas.

```java
public final class BooleanSearchQueryParser {
    private static final int MAX_TOKENS = 32;
    private static final int MAX_DEPTH = 8;

    public SearchNode parse(String input) {
        if (input == null || input.isBlank()) {
            throw invalid("Search query is empty");
        }
        Parser parser = new Parser(tokenize(input));
        SearchNode result = parser.parseOr(0);
        if (!parser.check(Type.END)) throw invalid("Unexpected token in search query");
        if (!hasPositiveTerm(result, false)) throw invalid("Search must contain a positive term");
        return result;
    }

    private List<Token> tokenize(String input) {
        List<Token> result = new ArrayList<>();
        for (int i = 0; i < input.length();) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(') { result.add(new Token(Type.LPAREN, "(")); i++; continue; }
            if (c == ')') { result.add(new Token(Type.RPAREN, ")")); i++; continue; }
            if (c == '"') {
                int end = input.indexOf('"', i + 1);
                if (end < 0) throw invalid("Unclosed phrase in search query");
                String phrase = input.substring(i + 1, end).strip();
                if (phrase.isEmpty()) throw invalid("Search phrase cannot be empty");
                result.add(new Token(Type.PHRASE, phrase));
                i = end + 1;
                continue;
            }
            int start = i;
            while (i < input.length() && !Character.isWhitespace(input.charAt(i))
                    && input.charAt(i) != '(' && input.charAt(i) != ')' && input.charAt(i) != '"') i++;
            String word = input.substring(start, i);
            Type type = switch (word.toUpperCase(Locale.ROOT)) {
                case "AND" -> Type.AND;
                case "OR" -> Type.OR;
                case "NOT" -> Type.NOT;
                default -> Type.WORD;
            };
            result.add(new Token(type, word));
            if (result.size() > MAX_TOKENS) throw invalid("Search query has too many terms");
        }
        if (result.size() > MAX_TOKENS) throw invalid("Search query has too many terms");
        result.add(new Token(Type.END, ""));
        return result;
    }

    private boolean hasPositiveTerm(SearchNode node, boolean negated) {
        return switch (node) {
            case Term ignored -> !negated;
            case Phrase ignored -> !negated;
            case Not not -> hasPositiveTerm(not.child(), !negated);
            case And and -> hasPositiveTerm(and.left(), negated) || hasPositiveTerm(and.right(), negated);
            case Or or -> hasPositiveTerm(or.left(), negated) || hasPositiveTerm(or.right(), negated);
        };
    }

    private static BadRequestException invalid(String message) {
        return new BadRequestException(message);
    }

    private enum Type { WORD, PHRASE, AND, OR, NOT, LPAREN, RPAREN, END }
    private record Token(Type type, String text) {}

    private final class Parser {
        private final List<Token> tokens;
        private int position;

        private Parser(List<Token> tokens) { this.tokens = tokens; }

        private SearchNode parseOr(int depth) {
            SearchNode node = parseAnd(depth);
            while (match(Type.OR)) node = new Or(node, parseAnd(depth));
            return node;
        }

        private SearchNode parseAnd(int depth) {
            SearchNode node = parseUnary(depth);
            while (true) {
                if (match(Type.AND)) {
                    if (!startsUnary(peek().type())) throw invalid("AND must be followed by a term");
                    node = new And(node, parseUnary(depth));
                } else if (startsUnary(peek().type())) {
                    node = new And(node, parseUnary(depth)); // implicit AND
                } else {
                    return node;
                }
            }
        }

        private SearchNode parseUnary(int depth) {
            if (match(Type.NOT)) {
                if (!startsUnary(peek().type())) throw invalid("NOT must be followed by a term");
                return new Not(parseUnary(depth));
            }
            return parsePrimary(depth);
        }

        private SearchNode parsePrimary(int depth) {
            if (match(Type.WORD)) return new Term(previous().text());
            if (match(Type.PHRASE)) return new Phrase(previous().text());
            if (match(Type.LPAREN)) {
                if (depth >= MAX_DEPTH) throw invalid("Search query is nested too deeply");
                SearchNode node = parseOr(depth + 1);
                if (!match(Type.RPAREN)) throw invalid("Unclosed parenthesis in search query");
                return node;
            }
            throw invalid("Expected a search term");
        }

        private boolean startsUnary(Type type) {
            return type == Type.WORD || type == Type.PHRASE || type == Type.NOT || type == Type.LPAREN;
        }
        private boolean check(Type type) { return peek().type() == type; }
        private boolean match(Type type) {
            if (!check(type)) return false;
            position++;
            return true;
        }
        private Token peek() { return tokens.get(position); }
        private Token previous() { return tokens.get(position - 1); }
    }
}
```

O parser acima usa pattern matching para `switch` sobre tipos selados, disponível no Java 21. Para compilar, declare os records selados mostrados na seção anterior como tipos top-level/package-private ou aninhados na classe, e importe `ArrayList`, `List` e `Locale`. Adapte mensagens de `BadRequestException` conforme o padrão já usado pelo projeto.

Verifique antes de executar a busca que a expressão compilada tem pelo menos um lexema. `plainto_tsquery`/`phraseto_tsquery` podem virar uma consulta vazia quando a palavra é stopword na configuração escolhida. Faça `dsl.select(field("numnode({0})", SQLDataType.INTEGER, tsquery)).fetchOne()` e retorne 400 se o resultado for zero.

## 4. Busca ranqueada e cursor

Crie uma projeção interna:

```java
public record OpportunitySearchHit(Long id, Instant createdAt, double rank) {}
```

Crie um método de repositório dedicado que receba `OpportunityFilter`, a árvore, o cursor e o limite. Injete `DSLContext` e construa a consulta com `select`, `from`, `where`, `orderBy` e `limit`. Use `DSL.field`/`DSL.condition` somente com templates fixos para a função de ranking e os operadores/tipos PostgreSQL. Passe os dados do usuário com `DSL.val` ou comparações tipadas do DSL. A forma lógica da consulta deve ser:

```sql
WITH search_query AS (
    SELECT <fragmento_tsquery_parametrizado> AS query
), ranked AS (
    SELECT o.id,
           o.criado_em,
           ts_rank_cd(
               ARRAY[0.1, 0.2, 0.4, 1.0]::real[],
               o.search_vector,
               sq.query,
               32
           ) AS rank
    FROM oportunidades.oportunidade o
    CROSS JOIN search_query sq
    WHERE o.deleted_at IS NULL
      AND o.status = 'PUBLISHED'::oportunidades.status_oportunidade
      AND o.search_vector @@ sq.query
      -- modality/recruiterId entram aqui somente quando informados
)
SELECT id, criado_em, rank
FROM oportunidades.oportunidade
WHERE deleted_at IS NULL
  AND status = 'PUBLISHED'::oportunidades.status_oportunidade
  AND search_vector @@ <tsquery jOOQ>
  -- modalidade/recrutador e predicado do cursor entram como condições jOOQ
ORDER BY rank DESC, criado_em DESC, id DESC
LIMIT limit + 1
```

O bloco SQL acima descreve a forma da consulta; não o execute como uma string montada manualmente. No jOOQ, a tabela/colunas devem ser `Table`/`Field`, e o status/modo enum podem usar `DSL.condition` com cast PostgreSQL constante e `DSL.val(...)` para o valor.

O array de pesos respeita a ordem `D, C, B, A`, então o vetor da migration usa peso maior para título. Adicione `modality` e `recruiterId` como parâmetros, nunca como fragmentos concatenados. O changeset `20260928-normalize-domain-enums` renomeia os valores para inglês; o bloco `rollback` é apenas a operação inversa. Portanto, passe `filter.modality().name()` como modalidade e use `PUBLISHED` no cast SQL do status público.

Para a continuação do cursor, ordenando por `rank DESC, criado_em DESC, id DESC`, aplique:

```sql
AND (
    rank < :lastRank
    OR (rank = :lastRank AND criado_em < :lastCreatedAt)
    OR (rank = :lastRank AND criado_em = :lastCreatedAt AND id < :lastId)
)
```

No cursor, adicione `rank` ao registro `OpportunityCursor` e ao codec. Codifique versão, rank, timestamp, ID e fingerprint dos filtros (incluindo a consulta normalizada `q`) em Base64 URL-safe, como o codec atual. Valide rank finito, ID positivo, timestamp válido, versão e fingerprint; qualquer divergência retorna 400. O cursor só é emitido quando a consulta buscou `limit + 1` linhas. Sem `q`, continue usando a consulta e cursor atuais por data/ID; não calcule ranking quando não há busca textual.

Após receber os IDs ranqueados, busque as entidades com recrutador carregado (`@EntityGraph(attributePaths = "recruiter")`) e reconstrua a lista na ordem dos hits. Converta com `OpportunityMapper.toSummary`, como já faz `OpportunityService`. Não retorne o rank no DTO público nesta primeira versão.

> A implementação de paginação precisa usar o mesmo `double` retornado pelo PostgreSQL no cursor seguinte, sem arredondar. Se decidir formatar o valor, use `Double.toString` e faça parse com `Double.parseDouble`.

## 5. Integração no service e remoção de `title`

No `OpportunityService.listPublic`:

1. Preserve a validação de `limit`, o comportamento de `status` e a criação do filtro público com `PUBLISHED`.
2. Se `filter.query()` for `null`, execute o caminho atual ordenado por data/ID.
3. Caso contrário, faça parse de `q`, decodifique cursor de ranking, chame o repositório de busca booleana com `limit + 1`, e gere o próximo cursor a partir do último hit entregue.
4. Em ambos os caminhos, retorne `OpportunityCursorResponse` com a mesma estrutura atual.

Apague a busca `LIKE` de `OpportunitySpecifications` e remova o parâmetro `title` do controller. Atualize o documento `docs/opportunity-crud-guide.md`: a tabela de parâmetros deve descrever `q`, a gramática e a ordenação por relevância quando houver consulta.

## 6. Atualização de testes existentes

Atualize usos de `new OpportunityFilter("...", ...)` para manter o primeiro argumento como consulta booleana. Adapte `OpportunityCursorCodecTest` para validar fingerprint de `q`, filtros e rank. Atualize o teste de integração atual, que procura `java 100%` literalmente no título: `%` deixa de ser caractere-curinga de `LIKE`; use termos reais do conteúdo e teste o escape em outro caso.

Casos mínimos para adicionar:

```text
"Java AND estágio" retorna somente vagas com ambos os termos.
"Java OR estágio" retorna vagas com pelo menos um dos termos.
"Java NOT estágio" exclui vagas com estágio e mantém Java.
"(Java OR dados) AND estágio" respeita parênteses e precedência.
"\"iniciação científica\" AND bolsa" trata frase entre aspas.
"Java estágio" funciona como AND implícito.
"AND Java", "Java OR", parêntese aberto e aspas sem fechar retornam 400.
"NOT Java" retorna 400 por não conter termo positivo.
Resultados cujo título contém os termos relevantes aparecem antes dos que só os contêm na descrição/requisitos.
Cursor de uma consulta não pode ser usado com outro q, modalidade ou recrutador.
Sem q, listagem e cursor continuam ordenados por data/ID.
Vagas fechadas/removidas nunca aparecem; apenas publicadas aparecem.
```

Não rode os testes durante a implementação manual se não quiser; quando for validar, o teste PostgreSQL existente usa Testcontainers e requer Docker.

## 7. Checklist final

- [ ] API pública usa `q`; não aceita mais `title`.
- [ ] Migration nova e versionada; nenhuma migration antiga alterada.
- [ ] `search_vector` usa peso `A` para título, `B` para descrição e `C` para requisitos.
- [ ] Índice GIN criado em `oportunidades.oportunidade.search_vector`.
- [ ] Parser aceita `AND`, `OR`, `NOT`, parênteses, frase e AND implícito.
- [ ] Entrada do usuário sempre chega ao banco por bind de parâmetro.
- [ ] Ranking e cursor são compatíveis; filtros e regras públicas permanecem aplicados.
- [ ] `docs/opportunity-crud-guide.md` e testes refletem o novo contrato.
