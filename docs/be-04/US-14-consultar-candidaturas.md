# US-14 — Consultar candidaturas

## API

`GET /api/students/{studentId}/applications?page=0&size=20&status=SUBMITTED`

page inicia em zero e deve ser não negativo. size tem padrão 20 e intervalo 1–100.
status é opcional e aceita os valores do ApplicationStatus, incluindo CANCELLED.
Parâmetros inválidos retornam 400; aluno inexistente retorna 404.

A resposta contém items, page, size, totalElements, totalPages e hasNext. Os itens incluem
identificador, aluno, datas, status atual e resumo da oportunidade (título, modalidade,
localização, status, recrutador, organização e deletedAt).

Ordenação fixa: appliedAt decrescente e id decrescente como desempate. Não é uma
paginação por snapshot: mudanças concorrentes entre páginas podem deslocar resultados.

O repository sempre filtra pelo aluno; filtro de status não remove esse isolamento.
EntityGraph carrega oportunidade e recrutador sem depender de open-in-view.
Oportunidades encerradas ou excluídas logicamente continuam aparecendo na candidatura,
preservando o acompanhamento.

## Testes

Service verifica parâmetros, filtro, ordenação e metadados de paginação. Controller
verifica parâmetros HTTP e enum inválido. Integração verifica páginas diferentes,
filtro por CANCELLED/SUBMITTED, isolamento entre dois alunos e preservação dos registros
quando a oportunidade encerra ou é excluída logicamente.

## Pendência

O isolamento é relativo ao studentId fornecido. A comprovação de que o solicitante
é o proprietário ainda depende da BE-01; migrar a rota para /me nessa integração.
