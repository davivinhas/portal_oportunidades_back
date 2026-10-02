# US-16 — Perfil do recrutador

## Objetivo

Permitir a consulta e a atualização dos dados do perfil de um recrutador.

## Implementação

- `Recruiter` concentra a alteração de tipo e nome da organização em `updateProfile`.
- `RecruiterRepository` fornece a persistência JPA.
- `RecruiterUpdateRequest` valida tipo obrigatório e nome com até 200 caracteres.
- `RecruiterResponse` não expõe a entidade JPA.
- `RecruiterMapper` usa MapStruct para produzir a resposta.
- `RecruiterService` carrega o perfil, aplica a alteração dentro de transação e utiliza dirty checking.
- `RecruiterController` expõe `GET` e `PUT` em `/api/recruiters/{recruiterId}/profile`.

## Regras cobertas

- Perfil inexistente retorna `404 Not Found`.
- Nome vazio retorna `400 Bad Request`.
- Apenas os campos editáveis do perfil são recebidos pela API.
- O campo `authorized` não pode ser alterado por esse endpoint.

## Testes

- Atualização dos dados na entidade.
- Rejeição de nome vazio.
- Consulta e atualização no service.
- Perfil inexistente.
- Validação e contrato HTTP do controller.

## Pendente

Com a autenticação, `recruiterId` será obtido do usuário autenticado e a rota será convertida para o contrato `/me`.
