# BE-05 — Integração com o banco de dados

## Implementações realizadas

- As imagens do PostgreSQL e do Redis foram fixadas em `postgres:17-alpine` e `redis:8-alpine`.
- As configurações local, cloud, de testes unitários e de integração foram separadas por meio de
  perfis do Spring.
- O Liquibase está habilitado nos bancos locais e nos bancos descartáveis utilizados pelos testes.
- O Liquibase está desabilitado durante a inicialização normal da aplicação com o perfil `cloud`.
- Os changesets históricos do banco foram recuperados preservando suas identidades originais.
- Um novo changeset normaliza os valores dos enums nativos do PostgreSQL para os valores em inglês
  utilizados pelo domínio.
- Um novo changeset adiciona restrições de unicidade, chaves estrangeiras e índices para os
  relacionamentos.
- Um teste de integração com Testcontainers valida todas as migrations, os mapeamentos do Hibernate,
  os enums nativos, a criação e publicação de oportunidades e as consultas aos repositories no
  PostgreSQL 17.

## Banco compartilhado no Supabase

O banco compartilhado foi inspecionado em modo somente leitura. Ele utiliza PostgreSQL 17 e ainda
mantém os valores antigos dos enums em português. Nenhuma migration desta branch foi aplicada ao
Supabase.

Antes da implantação, a equipe deve revisar o SQL gerado e aplicar os changesets pendentes por meio
de um comando explícito do Liquibase. Os testes automatizados devem continuar utilizando o container
descartável.

Um dos changesets históricos recuperados mantém o identificador original do autor, embora esse
identificador não seja mais o nome do projeto. O Liquibase utiliza o ID, o autor e o caminho do
arquivo como identidade do changeset. Alterar qualquer um desses elementos faria uma migration já
executada parecer uma migration nova.
