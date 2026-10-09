# US-04 — Perfil acadêmico e profissional

## Contrato implementado

- `GET /api/students/{studentId}/profile`: consulta, com resposta 200; perfil inexistente retorna 404.
- `PUT /api/students/{studentId}/profile`: cria o perfil de um usuário existente ou substitui seus dados, com resposta 200.
- Nome e e-mail vêm de User. Este fluxo não altera identidade, senha ou e-mail.
- Curso e período representam a formação atual; não há catálogo de habilidades nem lista de formações.
- Habilidades e interesses são texto livre. Resumo, habilidades e interesses admitem até 10.000 caracteres cada.
- A API usa nomes em inglês e mantém os nomes históricos das colunas do banco.
- IDs permanecem Long por compatibilidade com os perfis existentes.

Exemplo de PUT:

```json
{
  "registrationNumber": "202600001",
  "course": "Ciência da Computação",
  "semester": 3,
  "phone": "(98) 99999-9999",
  "summary": "Estudante interessado em desenvolvimento backend.",
  "skills": "Java, Spring Boot, PostgreSQL",
  "interests": "Estágio e pesquisa",
  "experiences": [
    {
      "id": null,
      "position": "Bolsista de pesquisa",
      "organization": "Universidade",
      "startDate": "2026-01-01",
      "endDate": null,
      "current": true,
      "description": "Desenvolvimento de sistemas."
    }
  ]
}
```

## Regras

Matrícula e curso são obrigatórios, com limites de 30 e 150 caracteres. Matrícula é única.
Período, se informado, é positivo; telefone admite até 30 caracteres. Campos opcionais
em branco são normalizados para null.

experiences é obrigatório e representa a lista completa, com no máximo 100 itens.
Lista vazia remove todas as experiências. Itens sem ID são criados; com ID são
atualizados; registros ausentes da lista são excluídos. IDs repetidos retornam 422.
IDs inexistentes ou pertencentes a outro aluno retornam 403 antes de alterar o perfil.

Cargo e organização são obrigatórios (150 e 200 caracteres); descrição admite 10.000
caracteres. Datas não podem estar no futuro; término não pode anteceder início.
Experiência atual não possui término. Experiência encerrada exige término.

O service usa transação única: falhas desfazem criação/atualização do perfil e todos
os ajustes da lista. A entidade Student possui @Version; alterações só em experiências
também incrementam a versão com OPTIMISTIC_FORCE_INCREMENT. Conflitos concorrentes
retornam 409 e o cliente deve recarregar o perfil. Isso detecta sobreposição de transações,
mas não substitui um contrato de versão do cliente para detectar formulários antigos
enviados depois de outra transação já concluída.

A consulta usa REPEATABLE_READ para montar perfil e experiências no mesmo snapshot.
Respostas contêm apenas DTOs; entidades JPA e hashes de senha não são expostos.

## Componentes e testes

- Entidades: Student e ProfessionalExperience com validações de domínio.
- DTOs record: StudentProfileRequest/Response e ExperienceRequest/Response.
- Mapper MapStruct: StudentProfileMapper.
- Repositories: StudentRepository, ProfessionalExperienceRepository e UserRepository.
- Service: StudentProfileService; controller: StudentProfileController.
- Testes unitários: domínio, service, contrato HTTP e validações.
- Integração: criação, atualização, substituição da lista, textos acima de 255 caracteres,
  unicidade, IDs de outros alunos, rollback e concorrência do perfil e das experiências.

## Pendência de autenticação

O aluno é informado na rota conforme o escopo acordado. A configuração de segurança
existente exige autenticação, mas ainda não verifica papel nem propriedade do perfil.
A criação pressupõe um usuário existente; não cadastra usuário nem concede papéis.

Na BE-01, obter o aluno pelo contexto autenticado, usar /me, permitir proprietário/admin
conforme o contrato aprovado e testar 401/403 com a configuração real de segurança.
Os testes standalone de controller desta entrega não validam autorização de usuários.
Este critério de aceite permanece pendente.
