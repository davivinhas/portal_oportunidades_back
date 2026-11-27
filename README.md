# LinkedUFMA Backend

Backend da plataforma LinkedUFMA, desenvolvido com Spring Boot, PostgreSQL e Liquibase.

## Criar o primeiro administrador

O cadastro público não permite escolher o papel de administrador. Para criar a primeira conta administrativa no banco configurado pelo Docker Compose, execute na raiz do projeto:

```powershell
docker compose run --build --rm -it -e SPRING_DOCKER_COMPOSE_ENABLED=false app bootstrap-admin
```

O comando cria um container temporário usando a configuração do serviço `app` e espera os serviços de banco de dados e Redis. O Spring aplica as migrations pendentes e, em seguida, solicita nome, e-mail e senha no terminal. A senha é lida sem aparecer na tela; não a informe como argumento do comando.

Se ainda não configurou o ambiente, crie o arquivo `.env` a partir do exemplo e defina `JWT_SECRET` com pelo menos 32 bytes:

```powershell
Copy-Item .env.example .env
```

O comando valida se o e-mail já está cadastrado, armazena a senha usando o mesmo BCrypt da aplicação e cria o vínculo do administrador na mesma transação. Em caso de sucesso, imprime o ID da conta e encerra. Se o e-mail já estiver em uso, a senha for inválida ou o banco não puder ser acessado, exibe o erro e encerra sem criar parcialmente a conta.

É necessário executar em um terminal interativo, pois a senha é solicitada de forma oculta. O `--build` garante que o comando use a versão atual do código.

## Documentação adicional

- [Autenticação e sessões](docs/authentication.md)
- [Guia de migrations](docs/migrations.md)
- [CRUD de oportunidades](docs/opportunity-crud-guide.md)
