# Opportunity CRUD — guia de implementação manual

Este guia acompanha o projeto LinkedUFMA e foi preparado para você escrever o código por etapas. **Criar este documento
não implementa a funcionalidade.** Os blocos abaixo são o código a copiar nos caminhos indicados; substitua arquivos
somente quando indicado. Os caminhos são relativos à raiz do repositório.

O texto está em português para acompanhar nosso estudo. Código, mensagens de erro e novos identificadores estão em
inglês. A base considerada usa Java 21 e Spring Boot 4.1.1.

**Já aplicado a seu pedido:** os ajustes do pom.xml das seções 2 e 11 foram feitos no projeto: MapStruct, remoção de
Security, starter Liquibase e perfil integration. Não duplique esses blocos. Os arquivos Java, migrations e testes
abaixo continuam como passos para sua implementação manual.

**Validação do material:** os 32 arquivos completos apresentados foram extraídos para uma cópia temporária em target e
compilados com Java 21. Os 22 testes rápidos e os 6 testes de integração passaram. As migrations foram aplicadas a um
PostgreSQL 17 isolado e validadas pelo Hibernate. O projeto atual também compilou após a edição do pom.xml. Isso valida
os exemplos; os fontes de produção do CRUD continuam para você implementar.

## Índice

1. [Decisões e funcionamento](#1-decisões-e-funcionamento)
2. [Dependências](#2-dependências)
3. [Exceções e relógio](#3-exceções-e-relógio)
4. [Modelo de domínio](#4-modelo-de-domínio)
5. [Migration](#5-migration)
6. [DTOs e mapper](#6-dtos-e-mapper)
7. [Repositories, filtros e cursor](#7-repositories-filtros-e-cursor)
8. [Service](#8-service)
9. [Controller e erros HTTP](#9-controller-e-erros-http)
10. [Testes unitários e HTTP](#10-testes-unitários-e-http)
11. [Integração com PostgreSQL](#11-integração-com-postgresql)
12. [Execução manual e checklist](#12-execução-manual-e-checklist)

## 1. Decisões e funcionamento

Mantemos a estrutura por módulo, com controller → service → repository. Não haverá interface de service, classe de use
case, entidade duplicada ou camada de adapters.

| Operação                   | Rota                             | Resultado               |
|----------------------------|----------------------------------|-------------------------|
| Criar rascunho completo    | POST /opportunities              | 201 e Location          |
| Listar                     | GET /opportunities               | 200 e página por cursor |
| Detalhar                   | GET /opportunities/{id}          | 200                     |
| Substituir dados editáveis | PUT /opportunities/{id}          | 200                     |
| Publicar                   | POST /opportunities/{id}/publish | 200                     |
| Encerrar                   | POST /opportunities/{id}/close   | 200                     |
| Excluir logicamente        | DELETE /opportunities/{id}       | 204                     |

Decisões já alinhadas:

- Criação como RASCUNHO, com todos os campos obrigatórios.
- Edição de RASCUNHO e PUBLICADA; ENCERRADA não pode ser editada nem reaberta.
- Publicar apenas RASCUNHO; encerrar apenas PUBLICADA.
- Aprovação/rejeição administrativa fica para depois. Valores legados continuam nos enums.
- Exclusão permitida em qualquer estado, sem remover a linha ou candidaturas.
- Repetir exclusão retorna 404; repetir publicação/encerramento retorna 409.
- Sem autenticação, ownership ou verificação de autorização do recrutador. Validamos apenas sua existência.
- recruiterId é informado na criação e não é editável.
- IDs Long e enums em português são preservados por compatibilidade com o banco atual.
- Filtros por title, modality, status e recruiterId; sem status, todos os não excluídos aparecem.
- Ordenação fixa por createdAt DESC, id DESC. limit padrão 20, mínimo 1, máximo 100.
- Sem total, page, offset ou ordenação customizável.
- Período: início estritamente anterior ao fim. Publicar ou editar uma publicada exige fim no futuro.
- O período pode começar no futuro. Expiração não altera automaticamente o status.
- PUT substitui todos os campos editáveis; opcionais ausentes viram null.
- Descrição e requisitos passam de VARCHAR(255) para TEXT.
- Acrescentamos version interno com @Version: evita que uma atualização concorrente sobrescreva um soft delete. Não é um
  campo editável nem parte da API.

### Quem faz o quê?

O controller interpreta HTTP. O service consulta o recrutador, abre a transação e chama a entidade. A entidade verifica
suas próprias regras. O repository consulta e persiste.

Exemplo: “o recrutador existe?” exige consulta e fica no service. “uma encerrada pode ser publicada?” depende do próprio
estado e fica na entidade.

MapStruct será usado para transformações de dados. Ele **não** alterará diretamente a entidade: isso contornaria os
métodos que protegem o domínio.

## 2. Dependências

Esta etapa já foi aplicada no pom.xml. Os trechos abaixo documentam as alterações para estudo; não precisam ser colados
novamente. A configuração existente do plugin Liquibase foi preservada.

Remova os dois blocos de dependência com estes artifactIds:

- spring-boot-starter-security
- spring-boot-starter-security-test

Não crie SecurityFilterChain nesta etapa. A segurança será adicionada posteriormente. O starter atual ativa proteção
automática; somente deixar de escrever anotações de autorização não libera a
API. [Referência do Spring Boot](https://docs.spring.io/spring-boot/reference/web/spring-security.html).

Adicione dentro de properties:

```
<mapstruct.version>1.6.3</mapstruct.version>
```

Adicione dentro de dependencies:

```
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
```

Adicione dentro de build/plugins:

```
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

O processador gera a implementação do mapper durante a compilação. Não escreva nem versione a classe gerada. Não
precisamos adicionar Lombok: os getters explícitos ajudam a entender a
entidade. [Configuração do MapStruct](https://mapstruct.org/documentation/stable/reference/html/#_apache_maven).

Adicione também este bloco profiles no nível de project, depois de build. Ele permite executar testes de integração
separados dos testes rápidos:

```
<profiles>
    <profile>
        <id>integration</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <executions>
                        <execution>
                            <goals>
                                <goal>integration-test</goal>
                                <goal>verify</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Use JDK 21 para o projeto. No terminal inspecionado havia Java 25 como padrão; ajuste o SDK da IDE e JAVA_HOME se
necessário. Não altere java.version para contornar isso.

## 3. Exceções e relógio

Crie os quatro arquivos abaixo. As exceções expressam a causa; a tradução para HTTP ficará no handler.

### src/main/java/com/example/portal_oportunidades_back/exception/BadRequestException.java

```
package com.example.portal_oportunidades_back.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
```

### src/main/java/com/example/portal_oportunidades_back/exception/BusinessException.java

```
package com.example.portal_oportunidades_back.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

### src/main/java/com/example/portal_oportunidades_back/exception/ResourceNotFoundException.java

```
package com.example.portal_oportunidades_back.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

### src/main/java/com/example/portal_oportunidades_back/configuration/TimeConfiguration.java

```
package com.example.portal_oportunidades_back.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

Injetar Clock evita espalhar Instant.now() e permite testes com horário fixo. A entidade recebe o instante como
argumento e não depende do Spring.

## 4. Modelo de domínio

### src/main/java/com/example/portal_oportunidades_back/opportunity/entity/OpportunityDetails.java

Crie este record. É um conjunto imutável dos dados editáveis, não um DTO HTTP. Evita repetir oito argumentos em cada
operação e garante que os dados sejam validados mesmo fora de um controller.

```
package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import java.time.Instant;

public record OpportunityDetails(
        String title,
        String description,
        String requirements,
        OpportunityModality modality,
        String location,
        Integer vacancyCount,
        Instant registrationStartsAt,
        Instant registrationEndsAt
) {
    public OpportunityDetails {
        title = requiredText(title, "Title");
        description = requiredText(description, "Description");
        requirements = optionalText(requirements);
        location = optionalText(location);
        if (title.length() > 200) {
            throw new BadRequestException("Title must have at most 200 characters");
        }
        if (location != null && location.length() > 200) {
            throw new BadRequestException("Location must have at most 200 characters");
        }
        if (modality == null) {
            throw new BadRequestException("Modality is required");
        }
        if (vacancyCount == null || vacancyCount < 1) {
            throw new BadRequestException("Vacancy count must be greater than zero");
        }
        if (registrationStartsAt == null || registrationEndsAt == null
                || !registrationStartsAt.isBefore(registrationEndsAt)) {
            throw new BadRequestException("Registration start must be before registration end");
        }
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(field + " is required");
        }
        return value.strip();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
```

### src/main/java/com/example/portal_oportunidades_back/opportunity/entity/Opportunity.java

Substitua o arquivo. Os nomes Java passam a inglês, mantendo @Column explícito para não renomear colunas antigas.

```
package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.auth.entity.Administrator;
import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "oportunidades", name = "oportunidade")
public class Opportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recrutador_id", nullable = false)
    private Recruiter recruiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliador_id")
    private Administrator evaluator;

    @Column(name = "titulo", nullable = false, length = 200)
    private String title;

    @Column(name = "descricao", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "requisitos", columnDefinition = "text")
    private String requirements;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "modalidade", nullable = false,
            columnDefinition = "oportunidades.modalidade_oportunidade")
    private OpportunityModality modality;

    @Column(name = "localizacao", length = 200)
    private String location;

    @Column(name = "quantidade_vagas", nullable = false)
    private Integer vacancyCount;

    @Column(name = "inicio_inscricoes", nullable = false)
    private Instant registrationStartsAt;

    @Column(name = "fim_inscricoes", nullable = false)
    private Instant registrationEndsAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "oportunidades.status_oportunidade")
    private OpportunityStatus status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Opportunity() {
    }

    public static Opportunity create(Recruiter recruiter, OpportunityDetails details, Instant now) {
        Opportunity opportunity = new Opportunity();
        opportunity.recruiter = Objects.requireNonNull(recruiter, "Recruiter is required");
        opportunity.apply(Objects.requireNonNull(details, "Details are required"));
        opportunity.status = OpportunityStatus.RASCUNHO;
        opportunity.createdAt = timestamp(now);
        opportunity.updatedAt = opportunity.createdAt;
        return opportunity;
    }

    public void updateDetails(OpportunityDetails details, Instant now) {
        requireNotDeleted();
        if (status != OpportunityStatus.RASCUNHO && status != OpportunityStatus.PUBLICADA) {
            throw new BusinessException("Only draft or published opportunities can be edited");
        }
        Objects.requireNonNull(details, "Details are required");
        if (status == OpportunityStatus.PUBLICADA) {
            requireFutureEnd(details.registrationEndsAt(), now);
        }
        apply(details);
        updatedAt = timestamp(now);
    }

    public void publish(Instant now) {
        requireNotDeleted();
        if (status != OpportunityStatus.RASCUNHO) {
            throw new BusinessException("Only draft opportunities can be published");
        }
        requireFutureEnd(registrationEndsAt, now);
        status = OpportunityStatus.PUBLICADA;
        updatedAt = timestamp(now);
    }

     public void close(Instant now) {
        requireNotDeleted();
        if (status != OpportunityStatus.PUBLICADA) {
            throw new BusinessException("Only published opportunities can be closed");
        }
        status = OpportunityStatus.ENCERRADA;
        updatedAt = timestamp(now);
    }

    public void softDelete(Instant now) {
        requireNotDeleted();
        deletedAt = timestamp(now);
        updatedAt = deletedAt;
    }

    public boolean canReceiveApplications(Instant now) {
        return deletedAt == null
                && status == OpportunityStatus.PUBLICADA
                && !now.isBefore(registrationStartsAt)
                && now.isBefore(registrationEndsAt);
    }

    private void requireNotDeleted() {
        if (deletedAt != null) {
            throw new BusinessException("Deleted opportunities cannot be changed");
        }
    }

    private static void requireFutureEnd(Instant end, Instant now) {
        if (!end.isAfter(now)) {
            throw new BusinessException("Registration must end in the future");
        }
    }

    private void apply(OpportunityDetails details) {
        title = details.title();
        description = details.description();
        requirements = details.requirements();
        modality = details.modality();
        location = details.location();
        vacancyCount = details.vacancyCount();
        registrationStartsAt = details.registrationStartsAt();
        registrationEndsAt = details.registrationEndsAt();
    }

    private static Instant timestamp(Instant now) {
        return Objects.requireNonNull(now, "Current time is required")
                .truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public Recruiter getRecruiter() { return recruiter; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRequirements() { return requirements; }
    public OpportunityModality getModality() { return modality; }
    public String getLocation() { return location; }
    public Integer getVacancyCount() { return vacancyCount; }
    public Instant getRegistrationStartsAt() { return registrationStartsAt; }
    public Instant getRegistrationEndsAt() { return registrationEndsAt; }
    public OpportunityStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
```

Aqui está a mudança de modelo anêmico para rich model: o chamador não consegue usar setStatus para pular etapas. A
própria entidade decide se publish é válido.

A validação ocorre antes da alteração dos campos. Assim, uma operação recusada não deixa o objeto parcialmente
modificado.

Os timestamps desta entidade são atualizados pelos próprios métodos, em vez de @UpdateTimestamp. Isso permite que a
resposta já tenha o updatedAt correto antes do flush. Truncamos os timestamps de auditoria para microssegundos para
respeitar a precisão do PostgreSQL e a chave do cursor.

canReceiveApplications usa intervalo [início, fim): inclui o instante inicial e exclui o final. Apenas prepara a regra
da entidade; não cria endpoints de candidatura.

@Version faz o Hibernate detectar alterações concorrentes na mesma linha. Um conflito retorna 409 pelo handler que
criaremos.

Não usamos filtro global de soft delete: consultas deste módulo excluem deletedAt, mas relações históricas de
candidatura continuam capazes de referenciar a oportunidade.

### src/main/java/com/example/portal_oportunidades_back/profile/entity/Recruiter.java

Substitua pelo conteúdo abaixo, preservando seus campos. As mudanças funcionais são os dois getters usados no resumo e o
carregamento LAZY do usuário.

```
package com.example.portal_oportunidades_back.profile.entity;

import com.example.portal_oportunidades_back.auth.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "perfil", name = "recrutador")
public class Recruiter {
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "perfil.tipo_recrutador")
    private RecruiterType tipo;

    @Column(name = "nome_organizacao", nullable = false, length = 200)
    private String organizationName;

    @Column(nullable = false)
    private boolean autorizado;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;

    protected Recruiter() {
    }

    public Long getId() { return id; }
    public String getOrganizationName() { return organizationName; }
}
```

Não altere OpportunityStatus, OpportunityModality ou RecruiterType nesta entrega. Seus valores já correspondem a enums
PostgreSQL.

## 5. Migration

Mantenha o master atual com includeAll. Ele ordena os arquivos pelo nome: **um arquivo 20260916_... entraria antes de
generated-diff.yaml**, que cria as tabelas. Por isso usamos z20260916-opportunity-crud.yaml, que ordena depois do
arquivo atual. Não renomeie migrations já executadas.

Primeiro termine de copiar o código Java deste guia. Depois compile e gere o rascunho fora da pasta incluída
automaticamente:

```
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd liquibase:diffChangeLog "-Dliquibase.diffChangeLogFile=target/opportunity-crud-draft.yaml"
```

Use as variáveis LIQUIBASE_COMMAND_URL, LIQUIBASE_COMMAND_USERNAME e LIQUIBASE_COMMAND_PASSWORD apontando para um
PostgreSQL **local de desenvolvimento** com as migrations atuais aplicadas. O arquivo liquibase.properties e o plugin
existentes já usam essa configuração. Não execute o diff contra um banco vazio nem contra o schema que já recebeu esta
nova migration.

Se ainda não aplicou a base, execute liquibase:update com apenas as migrations antigas, antes de criar o arquivo abaixo.
Não inicie a aplicação entre a alteração das entidades e a aplicação da migration, pois ddl-auto=validate detectará a
divergência.

Revise o rascunho: as mudanças intencionais são somente deleted_at, version, os dois tipos TEXT e o índice. Renomear os
atributos Java não deve renomear colunas físicas. O índice parcial precisa ser incluído manualmente na versão revisada.

### src/main/resources/db/changelog/changes/z20260916-opportunity-crud.yaml

Crie este arquivo **depois da comparação com o rascunho**. Este é o conteúdo revisado esperado para a base inspecionada:

```
databaseChangeLog:
  - changeSet:
      id: 20260916-opportunity-crud
      author: linkedufma
      changes:
        - addColumn:
            schemaName: oportunidades
            tableName: oportunidade
            columns:
              - column:
                  name: deleted_at
                  type: TIMESTAMP(6) WITH TIME ZONE
              - column:
                  name: version
                  type: BIGINT
                  defaultValueNumeric: 0
                  constraints:
                    nullable: false
        - modifyDataType:
            schemaName: oportunidades
            tableName: oportunidade
            columnName: descricao
            newDataType: TEXT
        - modifyDataType:
            schemaName: oportunidades
            tableName: oportunidade
            columnName: requisitos
            newDataType: TEXT
        - sql:
            dbms: postgresql
            sql: |
              CREATE INDEX idx_opportunity_active_cursor
              ON oportunidades.oportunidade (criado_em DESC, id DESC)
              WHERE deleted_at IS NULL;
```

Dados antigos recebem version=0 e deleted_at=null. Não incluímos rollback que reduza TEXT para VARCHAR(255), pois isso
poderia perder dados. Correções posteriores devem usar novo changeset.

Não altere migrations anteriores, não use ddl-auto=update e não copie o rascunho para changes sem revisão.

Checkpoint: após copiar todos os arquivos, execute liquibase:validate e liquibase:updateSQL; confira que a nova
migration roda após a criação da tabela.

## 6. DTOs e mapper

DTOs definem o contrato HTTP. Não possuem comportamento de persistência. Jakarta Validation rejeita erros simples na
entrada; as regras de domínio também são verificadas quando a entidade é usada sem HTTP.

Crie os arquivos desta seção. Todos ficam dentro do módulo opportunity.

### src/main/java/com/example/portal_oportunidades_back/opportunity/dto/CreateOpportunityRequest.java

```
package com.example.portal_oportunidades_back.opportunity.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateOpportunityRequest(
        @NotNull @Positive Long recruiterId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        String requirements,
        @NotNull OpportunityModality modality,
        @Size(max = 200) String location,
        @NotNull @Positive Integer vacancyCount,
        @NotNull Instant registrationStartsAt,
        @NotNull Instant registrationEndsAt
) {
}
```

### src/main/java/com/example/portal_oportunidades_back/opportunity/dto/UpdateOpportunityRequest.java

```
package com.example.portal_oportunidades_back.opportunity.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record UpdateOpportunityRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        String requirements,
        @NotNull OpportunityModality modality,
        @Size(max = 200) String location,
        @NotNull @Positive Integer vacancyCount,
        @NotNull Instant registrationStartsAt,
        @NotNull Instant registrationEndsAt
) {
}
```

### src/main/java/com/example/portal_oportunidades_back/opportunity/dto/RecruiterSummaryResponse.java

```
package com.example.portal_oportunidades_back.opportunity.dto;

public record RecruiterSummaryResponse(Long id, String organizationName) {
}
```

### src/main/java/com/example/portal_oportunidades_back/opportunity/dto/OpportunityResponse.java

```
package com.example.portal_oportunidades_back.opportunity.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import java.time.Instant;

public record OpportunityResponse(
        Long id,
        String title,
        String description,
        String requirements,
        OpportunityModality modality,
        String location,
        Integer vacancyCount,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        OpportunityStatus status,
        Instant createdAt,
        Instant updatedAt,
        RecruiterSummaryResponse recruiter
) {
}
```

### src/main/java/com/example/portal_oportunidades_back/opportunity/dto/OpportunitySummaryResponse.java

```
package com.example.portal_oportunidades_back.opportunity.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import java.time.Instant;

public record OpportunitySummaryResponse(
        Long id,
        String title,
        OpportunityModality modality,
        String location,
        Integer vacancyCount,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        OpportunityStatus status,
        Instant createdAt,
        RecruiterSummaryResponse recruiter
) {
}
```

### src/main/java/com/example/portal_oportunidades_back/opportunity/dto/OpportunityCursorResponse.java

```
package com.example.portal_oportunidades_back.opportunity.dto;

import java.util.List;

public record OpportunityCursorResponse(
        List<OpportunitySummaryResponse> items,
        String nextCursor,
        boolean hasNext
) {
    public OpportunityCursorResponse {
        items = List.copyOf(items);
    }
}
```

O resumo não envia descrição e requisitos. O recrutador expõe apenas ID e organização. Não há email, senha, usuário
completo, avaliador ou referências recursivas.

### src/main/java/com/example/portal_oportunidades_back/opportunity/mapper/OpportunityMapper.java

```
package com.example.portal_oportunidades_back.opportunity.mapper;

import com.example.portal_oportunidades_back.opportunity.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OpportunityMapper {
    OpportunityDetails toDetails(CreateOpportunityRequest request);
    OpportunityDetails toDetails(UpdateOpportunityRequest request);
    OpportunityResponse toResponse(Opportunity opportunity);
    OpportunitySummaryResponse toSummary(Opportunity opportunity);
    RecruiterSummaryResponse toRecruiterSummary(Recruiter recruiter);
}
```

O mapper não cria nem modifica Opportunity. A conversão de request gera OpportunityDetails, que passa pelo construtor
com validação; a entidade recebe esses dados pelos seus métodos.

ReportingPolicy.ERROR faz a compilação falhar se adicionarmos um campo ao DTO de saída e esquecermos de mapeá-lo.

## 7. Repositories, filtros e cursor

### src/main/java/com/example/portal_oportunidades_back/profile/repository/RecruiterRepository.java

```
package com.example.portal_oportunidades_back.profile.repository;

import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
}
```

### src/main/java/com/example/portal_oportunidades_back/opportunity/repository/OpportunityRepository.java

```
package com.example.portal_oportunidades_back.opportunity.repository;

import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OpportunityRepository
        extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    @EntityGraph(attributePaths = "recruiter")
    Optional<Opportunity> findByIdAndDeletedAtIsNull(Long id);
}
```

A consulta por ID já exclui registros apagados e carrega o recrutador necessário ao DTO.

JpaRepository expõe métodos de delete por contrato, mas nenhum fluxo deste módulo os utiliza. Soft delete será uma
alteração de estado dentro da transação. Não adicionamos SQLDelete como mecanismo oculto.

### src/main/java/com/example/portal_oportunidades_back/opportunity/query/OpportunityFilter.java

```
package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import java.util.Locale;

public record OpportunityFilter(
        String title,
        OpportunityModality modality,
        OpportunityStatus status,
        Long recruiterId
) {
    public OpportunityFilter {
        title = title == null || title.isBlank()
                ? null : title.strip().toLowerCase(Locale.ROOT);
        if (title != null && title.length() > 200) {
            throw new BadRequestException("Title filter must have at most 200 characters");
        }
        if (recruiterId != null && recruiterId < 1) {
            throw new BadRequestException("Recruiter ID must be positive");
        }
    }
}
```

### src/main/java/com/example/portal_oportunidades_back/opportunity/query/OpportunityCursor.java

```
package com.example.portal_oportunidades_back.opportunity.query;

import java.time.Instant;

public record OpportunityCursor(Instant createdAt, long id) {
}
```

### src/main/java/com/example/portal_oportunidades_back/opportunity/query/OpportunityCursorCodec.java

```
package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class OpportunityCursorCodec {
    private static final int MAX_LENGTH = 1024;

    public String encode(OpportunityCursor cursor, OpportunityFilter filter) {
        String payload = String.join("\n",
                "1",
                cursor.createdAt().toString(),
                Long.toString(cursor.id()),
                fingerprint(filter));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public OpportunityCursor decode(String token, OpportunityFilter filter) {
        if (token == null) {
            return null;
        }
        if (token.isBlank() || token.length() > MAX_LENGTH) {
            throw invalidCursor();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token),
                    StandardCharsets.UTF_8);
            String[] parts = payload.split("\n", -1);
            if (parts.length != 4 || !parts[0].equals("1")
                    || !parts[3].equals(fingerprint(filter))) {
                throw invalidCursor();
            }
            Instant createdAt = Instant.parse(parts[1]);
            long id = Long.parseLong(parts[2]);
            if (id < 1 || createdAt.getNano() % 1000 != 0
                    || createdAt.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
                    || createdAt.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z"))) {
                throw invalidCursor();
            }
            return new OpportunityCursor(createdAt, id);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private String fingerprint(OpportunityFilter filter) {
        String title = filter.title() == null ? "" : filter.title();
        String canonical = title.length() + ":" + title + "\n"
                + (filter.modality() == null ? "" : filter.modality().name()) + "\n"
                + (filter.status() == null ? "" : filter.status().name()) + "\n"
                + (filter.recruiterId() == null ? "" : filter.recruiterId().toString());
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private BadRequestException invalidCursor() {
        return new BadRequestException("Invalid cursor or cursor does not match the filters");
    }
}
```

O cursor contém versão, timestamp, ID e um resumo dos filtros. O cliente apenas guarda a string e a devolve.

Base64 e SHA-256 aqui **não são autenticação nem assinatura**. O resumo detecta uso acidental com filtros diferentes.
Não há dados secretos no cursor, e alterá-lo não contorna permissões: os mesmos filtros da consulta sempre são
reaplicados. Quando houver autorização, ela deverá vir do usuário autenticado no service, nunca do cursor.

limit não integra o resumo: o cliente pode alterar o tamanho do próximo lote, mantendo os filtros. Cursor inválido não
reinicia silenciosamente a consulta.

### src/main/java/com/example/portal_oportunidades_back/opportunity/query/OpportunitySpecifications.java

```
package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class OpportunitySpecifications {
    private OpportunitySpecifications() {
    }

    public static Specification<Opportunity> matching(
            OpportunityFilter filter, OpportunityCursor cursor) {
        return (root, query, builder) -> {
            if (query != null && query.getResultType() == Opportunity.class) {
                root.fetch("recruiter", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isNull(root.get("deletedAt")));
            if (filter.title() != null) {
                predicates.add(builder.like(
                        builder.lower(root.get("title")),
                        "%" + escapeLike(filter.title()) + "%",
                        '\\'));
            }
            if (filter.modality() != null) {
                predicates.add(builder.equal(root.get("modality"), filter.modality()));
            }
            if (filter.status() != null) {
                predicates.add(builder.equal(root.get("status"), filter.status()));
            }
            if (filter.recruiterId() != null) {
                predicates.add(builder.equal(root.get("recruiter").get("id"),
                        filter.recruiterId()));
            }
            if (cursor != null) {
                predicates.add(builder.or(
                        builder.lessThan(root.<Instant>get("createdAt"), cursor.createdAt()),
                        builder.and(
                                builder.equal(root.get("createdAt"), cursor.createdAt()),
                                builder.lessThan(root.<Long>get("id"), cursor.id()))));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
```

Specification é justificada porque os filtros são opcionais e combináveis. O filtro por título trata % e _ como
caracteres literais, não como curingas enviados pelo usuário.

O fetch é de uma relação to-one, então não multiplica os resultados como uma coleção faria. Isso evita uma consulta
adicional por recrutador ao mapear cada item.

A condição de continuação equivale a:

```
WHERE deleted_at IS NULL
  AND (
      criado_em < :cursorCreatedAt
      OR (criado_em = :cursorCreatedAt AND id < :cursorId)
  )
ORDER BY criado_em DESC, id DESC
LIMIT :limitPlusOne
```

Se dois registros possuem a mesma data, o ID decide a ordem. O cursor não depende de a linha anterior ainda existir:
excluir essa linha não interrompe o scroll.

O SQL acima é explicativo; não é uma migration nem deve ser copiado para um repository paralelo.

## 8. Service

### src/main/java/com/example/portal_oportunidades_back/opportunity/service/OpportunityService.java

```
package com.example.portal_oportunidades_back.opportunity.service;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.opportunity.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.query.*;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpportunityService {
    private static final Sort ORDER = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OpportunityRepository opportunities;
    private final RecruiterRepository recruiters;
    private final OpportunityMapper mapper;
    private final OpportunityCursorCodec cursorCodec;
    private final Clock clock;

    public OpportunityService(
            OpportunityRepository opportunities,
            RecruiterRepository recruiters,
            OpportunityMapper mapper,
            OpportunityCursorCodec cursorCodec,
            Clock clock) {
        this.opportunities = opportunities;
        this.recruiters = recruiters;
        this.mapper = mapper;
        this.cursorCodec = cursorCodec;
        this.clock = clock;
    }

    @Transactional
    public OpportunityResponse create(CreateOpportunityRequest request) {
        var details = mapper.toDetails(request);
        var recruiter = recruiters.findById(request.recruiterId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found"));
        var opportunity = Opportunity.create(recruiter, details, clock.instant());
        return mapper.toResponse(opportunities.save(opportunity));
    }

    public OpportunityResponse findById(Long id) {
        return mapper.toResponse(findActive(id));
    }

    public OpportunityCursorResponse list(
            OpportunityFilter filter, String cursorToken, int limit) {
        if (limit < 1 || limit > 100) {
            throw new BadRequestException("Limit must be between 1 and 100");
        }
        var cursor = cursorCodec.decode(cursorToken, filter);
        var specification = OpportunitySpecifications.matching(filter, cursor);
        List<Opportunity> batch = opportunities.findBy(specification,
                query -> query.sortBy(ORDER).limit(limit + 1).all());

        boolean hasNext = batch.size() > limit;
        List<Opportunity> visible = hasNext ? batch.subList(0, limit) : batch;
        String nextCursor = null;
        if (hasNext) {
            Opportunity last = visible.getLast();
            nextCursor = cursorCodec.encode(
                    new OpportunityCursor(last.getCreatedAt(), last.getId()), filter);
        }
        return new OpportunityCursorResponse(
                visible.stream().map(mapper::toSummary).toList(),
                nextCursor,
                hasNext);
    }

    @Transactional
    public OpportunityResponse update(Long id, UpdateOpportunityRequest request) {
        Opportunity opportunity = findActive(id);
        opportunity.updateDetails(mapper.toDetails(request), clock.instant());
        return mapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse publish(Long id) {
        Opportunity opportunity = findActive(id);
        opportunity.publish(clock.instant());
        return mapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse close(Long id) {
        Opportunity opportunity = findActive(id);
        opportunity.close(clock.instant());
        return mapper.toResponse(opportunity);
    }

    @Transactional
    public void delete(Long id) {
        findActive(id).softDelete(clock.instant());
    }

    private Opportunity findActive(Long id) {
        return opportunities.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }
}
```

O service retorna DTOs mapeados ainda dentro da transação. Como open-in-view=false já existe, isso evita depender de
carregamento lazy durante a serialização HTTP. O controller continua responsável por escolher status, headers e formato
de entrada.

Somente a criação usa save. Na atualização, publicação, encerramento e exclusão, a entidade carregada é gerenciada pelo
JPA; as alterações são sincronizadas no commit.

Na listagem buscamos no máximo limit+1, sem count. O item extra informa hasNext, mas o cursor aponta para o último item
**entregue**, nunca para o extra. Usamos a API fluent de Specification disponível na versão do
projeto. [Referência do Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/api/java/org/springframework/data/jpa/repository/JpaSpecificationExecutor.SpecificationFluentQuery.html).

O scroll não é um snapshot: oportunidades criadas no topo aparecem quando você reinicia a consulta. Exclusões e mudanças
nos filtros dos registros podem mudar o conjunto durante a navegação; a ordenação não muda com edições porque createdAt
é imutável.

## 9. Controller e erros HTTP

### src/main/java/com/example/portal_oportunidades_back/exception/GlobalExceptionHandler.java

```
package com.example.portal_oportunidades_back.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(
            BadRequestException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(
            BusinessException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentUpdate(
            OptimisticLockingFailureException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT,
                "Opportunity was modified concurrently; reload it and try again", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null
                                ? "Invalid value" : error.getDefaultMessage()))
                .toList();
        body.setProperty("errors", errors);
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    private ProblemDetail problem(
            HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setInstance(URI.create(request.getRequestURI()));
        return body;
    }
}
```

ResponseEntityExceptionHandler já traduz JSON inválido, enum desconhecido, tipo de parâmetro inválido e validação de
argumentos de métodos para respostas HTTP padronizadas. O override acima acrescenta os erros de campos do body sem expor
o valor recebido.

Não convertemos qualquer Exception em 400: um bug interno não deve parecer erro do usuário. Não enviamos stack traces,
SQL nem valores sensíveis em mensagens.

### src/main/java/com/example/portal_oportunidades_back/opportunity/controller/OpportunityController.java

```
package com.example.portal_oportunidades_back.opportunity.controller;

import com.example.portal_oportunidades_back.opportunity.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/opportunities")
@Tag(name = "Opportunities", description = "Opportunity management without authentication in this phase")
@ApiResponse(responseCode = "400", description = "Invalid request, filter or cursor",
        content = @Content(mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
public class OpportunityController {
    private final OpportunityService service;

    public OpportunityController(OpportunityService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a complete draft opportunity")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Draft created"),
            @ApiResponse(responseCode = "404", description = "Recruiter not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OpportunityResponse> create(
            @Valid @RequestBody CreateOpportunityRequest request) {
        OpportunityResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List non-deleted opportunities using a forward cursor",
            description = "Sorted by createdAt DESC and id DESC. Keep filters unchanged "
                    + "when sending nextCursor. No total count or page numbers.")
    public OpportunityCursorResponse list(
            @Parameter(description = "Opaque nextCursor from the previous response")
            @RequestParam(name = "cursor", required = false) @Size(max = 1024) String cursor,
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(name = "title", required = false) @Size(max = 200) String title,
            @RequestParam(name = "modality", required = false) OpportunityModality modality,
            @RequestParam(name = "status", required = false) OpportunityStatus status,
            @RequestParam(name = "recruiterId", required = false) @Positive Long recruiterId) {
        return service.list(new OpportunityFilter(title, modality, status, recruiterId),
                cursor, limit);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a non-deleted opportunity")
    @ApiResponse(responseCode = "404", description = "Opportunity not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public OpportunityResponse findById(@PathVariable("id") @Positive Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace editable details of a draft or published opportunity")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Opportunity not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "State or concurrent update conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OpportunityResponse update(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody UpdateOpportunityRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a draft whose registration end is in the future")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Opportunity not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "State, deadline or concurrent update conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OpportunityResponse publish(@PathVariable("id") @Positive Long id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a published opportunity")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Opportunity not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "State or concurrent update conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OpportunityResponse close(@PathVariable("id") @Positive Long id) {
        return service.close(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete an opportunity without removing its applications")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Opportunity soft deleted",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Opportunity not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Concurrent update conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void delete(@PathVariable("id") @Positive Long id) {
        service.delete(id);
    }
}
```

Não adicione @Validated à classe do controller: usamos a validação de métodos integrada ao Spring MVC para as
constraints dos parâmetros. @Valid no body continua necessário.

As ações publish e close têm rotas próprias porque expressam intenções diferentes de editar conteúdo. O cliente não
envia status arbitrário em PUT.

Checkpoint: todos os arquivos de produção estão apresentados. Execute a compilação antes de avançar para os testes:

```
.\mvnw.cmd -DskipTests compile
```

Se a IDE não encontrar OpportunityMapperImpl, reimporte o Maven e confira o processamento de anotações. A implementação
aparece em target/generated-sources/annotations; não deve ser criada manualmente.

## 10. Testes unitários e HTTP

Os testes de domínio e service não usam banco. Os testes HTTP usam MockMvc com o service simulado. A integração com
PostgreSQL aparece na próxima seção.

**Antes de executar mvnw test:** o arquivo original PortalOportunidadesBackApplicationTests.java usa @SpringBootTest sem
perfil de teste e pode tentar acessar o banco configurado na aplicação. Renomeie/substitua esse teste pelo
PortalOportunidadesBackApplicationIT da seção 11. Não mantenha os dois: o smoke test continuará existindo, agora na
suíte de integração isolada.

### src/test/java/com/example/portal_oportunidades_back/opportunity/OpportunityFixtures.java

Um helper restrito a testes, sem novas abstrações na aplicação.

```
package com.example.portal_oportunidades_back.opportunity;

import com.example.portal_oportunidades_back.opportunity.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.mock;

public final class OpportunityFixtures {
    public static final Instant NOW = Instant.parse("2030-01-10T12:00:00Z");
    public static final Instant START = NOW.minusSeconds(60);
    public static final Instant END = NOW.plusSeconds(3600);

    private OpportunityFixtures() {
    }

    public static OpportunityDetails details() {
        return new OpportunityDetails("Research assistant", "Research activities", "Java",
                OpportunityModality.IC, "São Luís", 2, START, END);
    }

    public static Opportunity draft(long id) {
        Opportunity opportunity = Opportunity.create(mock(Recruiter.class), details(), NOW);
        ReflectionTestUtils.setField(opportunity, "id", id);
        return opportunity;
    }

    public static CreateOpportunityRequest createRequest() {
        return new CreateOpportunityRequest(7L, "Research assistant", "Research activities",
                "Java", OpportunityModality.IC, "São Luís", 2, START, END);
    }

    public static UpdateOpportunityRequest updateRequest() {
        return new UpdateOpportunityRequest("Updated title", "Updated description",
                null, OpportunityModality.IC, null, 3, START, END);
    }
}
```

ReflectionTestUtils é usado somente no teste para simular o ID que seria atribuído pelo JPA. Não crie um setId público
na entidade para facilitar testes.

### src/test/java/com/example/portal_oportunidades_back/opportunity/entity/OpportunityTest.java

```
package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import org.junit.jupiter.api.Test;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OpportunityTest {
    @Test
    void createsCompleteDraft() {
        var opportunity = Opportunity.create(mock(Recruiter.class), details(), NOW);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.RASCUNHO);
        assertThat(opportunity.getCreatedAt()).isEqualTo(NOW);
        assertThat(opportunity.getUpdatedAt()).isEqualTo(NOW);
        assertThat(opportunity.canReceiveApplications(NOW)).isFalse();
    }

    @Test
    void rejectsInvalidPeriodAndVacancyCount() {
        assertThatThrownBy(() -> new OpportunityDetails(
                "Title", "Description", null, OpportunityModality.IC, null, 1, END, START))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> new OpportunityDetails(
                "Title", "Description", null, OpportunityModality.IC, null, 1, START, START))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> new OpportunityDetails(
                "Title", "Description", null, OpportunityModality.IC, null, 0, START, END))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void publishesAndHonorsRegistrationBoundaries() {
        var opportunity = draft(1);
        opportunity.publish(NOW);
        assertThat(opportunity.canReceiveApplications(START.minusNanos(1))).isFalse();
        assertThat(opportunity.canReceiveApplications(START)).isTrue();
        assertThat(opportunity.canReceiveApplications(END.minusNanos(1))).isTrue();
        assertThat(opportunity.canReceiveApplications(END)).isFalse();
        assertThatThrownBy(() -> opportunity.publish(NOW))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void allowsPublishingBeforeRegistrationStarts() {
        var details = new OpportunityDetails("Title", "Description", null,
                OpportunityModality.IC, null, 1, NOW.plusSeconds(60), END);
        var opportunity = Opportunity.create(mock(Recruiter.class), details, NOW);
        opportunity.publish(NOW);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.PUBLICADA);
        assertThat(opportunity.canReceiveApplications(NOW)).isFalse();
    }

    @Test
    void rejectsPublishingAtDeadline() {
        var opportunity = draft(1);
        assertThatThrownBy(() -> opportunity.publish(END))
                .isInstanceOf(BusinessException.class);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.RASCUNHO);
    }

    @Test
    void updatesDraftAndPublishedButRejectsExpiredPeriodAtomically() {
        var opportunity = draft(1);
        var replacement = new OpportunityDetails("New title", "New description", null,
                OpportunityModality.BOLSA, null, 4, START, END);
        opportunity.updateDetails(replacement, NOW);
        assertThat(opportunity.getTitle()).isEqualTo("New title");
        assertThat(opportunity.getRequirements()).isNull();
        opportunity.publish(NOW);
        opportunity.updateDetails(details(), NOW.plusSeconds(1));
        assertThat(opportunity.getTitle()).isEqualTo(details().title());

        var expired = new OpportunityDetails("Should not apply", "Description", null,
                OpportunityModality.IC, null, 1, START, NOW);
        assertThatThrownBy(() -> opportunity.updateDetails(expired, NOW))
                .isInstanceOf(BusinessException.class);
        assertThat(opportunity.getTitle()).isEqualTo(details().title());
    }

    @Test
    void closesOnlyPublishedAndNeverReopens() {
        var opportunity = draft(1);
        assertThatThrownBy(() -> opportunity.close(NOW))
                .isInstanceOf(BusinessException.class);
        opportunity.publish(NOW);
        opportunity.close(NOW.plusSeconds(1));
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.ENCERRADA);
        assertThat(opportunity.canReceiveApplications(NOW)).isFalse();
        assertThatThrownBy(() -> opportunity.updateDetails(details(), NOW))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.publish(NOW))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.close(NOW))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void softDeletePreservesStatusAndBlocksChanges() {
        var opportunity = draft(1);
        opportunity.publish(NOW);
        opportunity.softDelete(NOW.plusSeconds(2));
        assertThat(opportunity.getDeletedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.PUBLICADA);
        assertThat(opportunity.canReceiveApplications(NOW)).isFalse();
        assertThatThrownBy(() -> opportunity.updateDetails(details(), NOW))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.publish(NOW))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.close(NOW))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.softDelete(NOW))
                .isInstanceOf(BusinessException.class);
    }
}
```

### src/test/java/com/example/portal_oportunidades_back/opportunity/query/OpportunityCursorCodecTest.java

```
package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.NOW;
import static org.assertj.core.api.Assertions.*;

class OpportunityCursorCodecTest {
    private final OpportunityCursorCodec codec = new OpportunityCursorCodec();
    private final OpportunityFilter filter =
            new OpportunityFilter(" Java ", OpportunityModality.IC, null, 7L);

    @Test
    void roundTripsAndAcceptsEquivalentNormalizedFilters() {
        String token = codec.encode(new OpportunityCursor(NOW, 123), filter);
        var decoded = codec.decode(token,
                new OpportunityFilter("java", OpportunityModality.IC, null, 7L));
        assertThat(decoded).isEqualTo(new OpportunityCursor(NOW, 123));
        assertThat(token).doesNotContain("+", "/", "=");
    }

    @Test
    void rejectsChangedFilters() {
        String token = codec.encode(new OpportunityCursor(NOW, 123), filter);
        assertThatThrownBy(() -> codec.decode(token,
                new OpportunityFilter("other", OpportunityModality.IC, null, 7L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsMalformedOversizedAndUnsupportedTokens() {
        assertThat(codec.decode(null, filter)).isNull();
        for (String token : new String[]{"", "%%%", "a".repeat(1025)}) {
            assertThatThrownBy(() -> codec.decode(token, filter))
                    .isInstanceOf(BadRequestException.class);
        }
        String valid = codec.encode(new OpportunityCursor(NOW, 123), filter);
        String payload = new String(Base64.getUrlDecoder().decode(valid), StandardCharsets.UTF_8);
        String unsupported = Base64.getUrlEncoder().withoutPadding().encodeToString(
                payload.replaceFirst("1\n", "2\n").getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> codec.decode(unsupported, filter))
                .isInstanceOf(BadRequestException.class);
    }
}
```

### src/test/java/com/example/portal_oportunidades_back/opportunity/service/OpportunityServiceTest.java

```
package com.example.portal_oportunidades_back.opportunity.service;

import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursorCodec;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {
    @Mock OpportunityRepository opportunities;
    @Mock RecruiterRepository recruiters;
    private OpportunityService service;

    @BeforeEach
    void setUp() {
        service = new OpportunityService(opportunities, recruiters,
                Mappers.getMapper(OpportunityMapper.class), new OpportunityCursorCodec(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsDraftWithExistingRecruiter() {
        var recruiter = mock(Recruiter.class);
        when(recruiters.findById(7L)).thenReturn(Optional.of(recruiter));
        when(opportunities.save(any(Opportunity.class))).thenAnswer(call -> call.getArgument(0));
        var response = service.create(createRequest());
        assertThat(response.status()).isEqualTo(OpportunityStatus.RASCUNHO);
        assertThat(response.title()).isEqualTo(createRequest().title());
        verify(opportunities).save(argThat(opportunity ->
                opportunity.getRecruiter() == recruiter
                        && opportunity.getStatus() == OpportunityStatus.RASCUNHO));
    }

    @Test
    void rejectsMissingRecruiterWithoutSaving() {
        when(recruiters.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(opportunities);
    }

    @Test
    void updatesLoadedEntityWithoutCallingSave() {
        var opportunity = draft(10);
        when(opportunities.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(opportunity));
        var response = service.update(10L, updateRequest());
        assertThat(response.title()).isEqualTo("Updated title");
        assertThat(response.requirements()).isNull();
        verify(opportunities, never()).save(any());
    }

    @Test
    void publishesAndClosesThroughDomainMethods() {
        var opportunity = draft(10);
        when(opportunities.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(opportunity));
        assertThat(service.publish(10L).status()).isEqualTo(OpportunityStatus.PUBLICADA);
        assertThat(service.close(10L).status()).isEqualTo(OpportunityStatus.ENCERRADA);
        verify(opportunities, never()).save(any());
    }

    @Test
    void softDeletesWithoutInvokingAnyRepositoryDelete() {
        var opportunity = draft(10);
        when(opportunities.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(opportunity));
        service.delete(10L);
        assertThat(opportunity.getDeletedAt()).isEqualTo(NOW);
        verify(opportunities).findByIdAndDeletedAtIsNull(10L);
        verifyNoMoreInteractions(opportunities);
    }

    @Test
    void returnsNotFoundForAllOperationsOnMissingOrDeletedRecords() {
        when(opportunities.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(10L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(10L, updateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.publish(10L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.close(10L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(10L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
```

O mapper real também participa destes testes, então campos com nomes incompatíveis não ficam escondidos por mocks. Os
testes de service verificam a coordenação; os de entidade verificam transições; os de PostgreSQL testarão o resultado
real da paginação.

### src/test/java/com/example/portal_oportunidades_back/opportunity/controller/OpportunityControllerTest.java

```
package com.example.portal_oportunidades_back.opportunity.controller;

import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.opportunity.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpportunityController.class)
@Import(GlobalExceptionHandler.class)
class OpportunityControllerTest {
    private final MockMvc mvc;

    @MockitoBean
    private OpportunityService service;

    @Autowired
    OpportunityControllerTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    private static final String BODY = """
            {
              "recruiterId": 7,
              "title": "Research assistant",
              "description": "Research activities",
              "requirements": "Java",
              "modality": "IC",
              "location": "São Luís",
              "vacancyCount": 2,
              "registrationStartsAt": "2030-01-10T11:59:00Z",
              "registrationEndsAt": "2030-01-10T13:00:00Z"
            }
            """;

    private OpportunityResponse response() {
        return new OpportunityResponse(10L, "Research assistant", "Research activities",
                "Java", OpportunityModality.IC, "São Luís", 2, START, END,
                OpportunityStatus.RASCUNHO, NOW, NOW,
                new RecruiterSummaryResponse(7L, "UFMA"));
    }

    @Test
    void createsWithoutCredentialsAndReturnsLocation() throws Exception {
        when(service.create(any())).thenReturn(response());
        mvc.perform(post("/opportunities").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/opportunities/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.recruiter.organizationName").value("UFMA"))
                .andExpect(jsonPath("$.recruiter.user").doesNotExist());
    }

    @Test
    void readsAndMutatesWithoutAuthenticationOrCsrfToken() throws Exception {
        when(service.findById(10L)).thenReturn(response());
        when(service.update(eq(10L), any())).thenReturn(response());
        when(service.publish(10L)).thenReturn(response());
        when(service.close(10L)).thenReturn(response());

        mvc.perform(get("/opportunities/10")).andExpect(status().isOk());
        String updateBody = BODY.replace("\"recruiterId\": 7,", "");
        mvc.perform(put("/opportunities/10")
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk());
        mvc.perform(post("/opportunities/10/publish")).andExpect(status().isOk());
        mvc.perform(post("/opportunities/10/close")).andExpect(status().isOk());
        mvc.perform(delete("/opportunities/10")).andExpect(status().isNoContent());
        verify(service).delete(10L);
    }

    @Test
    void listsWithoutPageOrTotal() throws Exception {
        when(service.list(any(), isNull(), eq(20)))
                .thenReturn(new OpportunityCursorResponse(List.of(), null, false));
        mvc.perform(get("/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.totalElements").doesNotExist())
                .andExpect(jsonPath("$.page").doesNotExist());
    }

    @Test
    void rejectsInvalidBodiesAndQueryParameters() throws Exception {
        mvc.perform(post("/opportunities").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
        mvc.perform(post("/opportunities").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/opportunities").param("limit", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/opportunities").param("limit", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/opportunities").param("status", "UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(get("/opportunities/0")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void mapsDomainErrorsToProblemDetails() throws Exception {
        when(service.findById(10L)).thenThrow(new ResourceNotFoundException("Opportunity not found"));
        mvc.perform(get("/opportunities/10"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));

        when(service.publish(10L)).thenThrow(new BusinessException("Only draft opportunities can be published"));
        mvc.perform(post("/opportunities/10/publish"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        when(service.list(any(), eq("broken"), eq(20)))
                .thenThrow(new BadRequestException("Invalid cursor"));
        mvc.perform(get("/opportunities").param("cursor", "broken"))
                .andExpect(status().isBadRequest());
    }
}
```

O pacote de WebMvcTest é o do **Spring Boot 4**, não o encontrado em exemplos antigos de Boot

3. [Referência da anotação](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/WebMvcTest.html).

Execute os testes rápidos após mover o smoke test para a suíte IT:

```
.\mvnw.cmd test
```

Nenhum desses testes precisa de PostgreSQL, Redis ou Supabase.

## 11. Integração com PostgreSQL

Usaremos PostgreSQL local exclusivo para testes, porque os enums e o índice parcial são específicos dele. H2 não
validaria essa compatibilidade.

### Ajuste adicional de dependência para Spring Boot 4

Este ajuste também já foi aplicado: a dependência direta org.liquibase:liquibase-core foi substituída por:

```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-liquibase</artifactId>
</dependency>
```

Isso é necessário para a integração automática do Liquibase no Boot 4. O plugin liquibase-maven-plugin continua no
build: ele executa comandos Maven; o starter executa migrations ao iniciar a aplicação. São responsabilidades
diferentes. [Inicialização de banco no Spring Boot](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

Faça esse ajuste mesmo se for executar migrations manualmente durante o desenvolvimento: os testes de integração abaixo
verificam a inicialização completa a partir de um banco vazio.

### src/test/resources/application-integration.properties

Crie o arquivo. Estas credenciais são exclusivas do container descartável local, não são segredos de um ambiente real.

```
spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/linkedufma_opportunity_test
spring.datasource.username=opportunity_test
spring.datasource.password=opportunity_test
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
spring.docker.compose.enabled=false
spring.data.redis.repositories.enabled=false
management.health.redis.enabled=false
```

O perfil substitui as propriedades de datasource que atualmente usam SUPABASE_DB_*. Não use um banco compartilhado para
estes testes.

### src/test/java/com/example/portal_oportunidades_back/PortalOportunidadesBackApplicationIT.java

Renomeie o antigo PortalOportunidadesBackApplicationTests.java para este caminho e substitua o conteúdo:

```
package com.example.portal_oportunidades_back;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
class PortalOportunidadesBackApplicationIT {
    @Test
    void contextLoads() {
    }
}
```

O sufixo IT é executado pelo Failsafe com -Pintegration verify. O sufixo Test é executado pelo Surefire com test.

### src/test/java/com/example/portal_oportunidades_back/opportunity/OpportunityPersistenceIT.java

```
package com.example.portal_oportunidades_back.opportunity;

import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.opportunity.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
@Import(OpportunityPersistenceIT.FixedTime.class)
@Transactional
class OpportunityPersistenceIT {
    private final OpportunityService service;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private long recruiterId;

    @Autowired
    OpportunityPersistenceIT(
            OpportunityService service, JdbcTemplate jdbc, EntityManager entityManager) {
        this.service = service;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @TestConfiguration
    static class FixedTime {
        @Bean
        @Primary
        Clock integrationClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void seedRecruiter() {
        recruiterId = createUser();
        jdbc.update("""
                INSERT INTO perfil.recrutador
                  (usuario_id, autorizado, nome_organizacao, tipo, criado_em, atualizado_em)
                VALUES (?, false, 'UFMA test', 'PROFESSOR', now(), now())
                """, recruiterId);
    }

    private long createUser() {
        return jdbc.queryForObject("""
                INSERT INTO app_auth.usuario
                  (name, email, senha_hash, active, criado_em, atualizado_em)
                VALUES ('Test user', ?, 'unused-test-value', true, now(), now())
                RETURNING id
                """, Long.class, UUID.randomUUID() + "@example.test");
    }

    private OpportunityResponse create(String title) {
        return service.create(new CreateOpportunityRequest(recruiterId, title,
                "Description ".repeat(100), "Requirements ".repeat(100),
                OpportunityModality.IC, "São Luís", 2, START, END));
    }

    private OpportunityFilter filter() {
        return new OpportunityFilter(null, null, null, recruiterId);
    }

    @Test
    void keysetUsesIdForTiesAndContinuesAfterCursorRowIsDeleted() {
        var firstCreated = create("First");
        var secondCreated = create("Second");
        var thirdCreated = create("Third");
        entityManager.flush();

        var firstPage = service.list(filter(), null, 2);
        assertThat(firstPage.items()).extracting(OpportunitySummaryResponse::id)
                .containsExactly(thirdCreated.id(), secondCreated.id());
        assertThat(firstPage.hasNext()).isTrue();

        service.delete(secondCreated.id());
        create("Inserted after the first page");
        entityManager.flush();
        entityManager.clear();

        var secondPage = service.list(filter(), firstPage.nextCursor(), 2);
        assertThat(secondPage.items()).extracting(OpportunitySummaryResponse::id)
                .containsExactly(firstCreated.id());
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.nextCursor()).isNull();
    }

    @Test
    void combinesFiltersAndTreatsLikeWildcardsLiterally() {
        var matching = create("Java 100% research");
        var unpublished = create("Java 100% draft");
        var wrongTitle = create("Java 100X research");
        service.publish(matching.id());
        service.publish(wrongTitle.id());

        var filtered = service.list(new OpportunityFilter(
                " JAVA 100% ", OpportunityModality.IC,
                OpportunityStatus.PUBLICADA, recruiterId), null, 20);
        assertThat(filtered.items()).extracting(OpportunitySummaryResponse::id)
                .containsExactly(matching.id());
        assertThat(service.findById(unpublished.id()).status())
                .isEqualTo(OpportunityStatus.RASCUNHO);
    }

    @Test
    void softDeleteKeepsRowAndApplicationAndHidesOpportunity() {
        var opportunity = create("Opportunity with application");
        long studentId = createUser();
        jdbc.update("""
                INSERT INTO perfil.aluno
                  (usuario_id, matricula, curso, criado_em, atualizado_em)
                VALUES (?, 'TEST-001', 'Computer Science', now(), now())
                """, studentId);
        jdbc.update("""
                INSERT INTO oportunidades.candidatura
                  (aluno_id, oportunidade_id, data_candidatura, status, criado_em, atualizado_em)
                VALUES (?, ?, now(), 'SUBMITTED', now(), now())
                """, studentId, opportunity.id());

        service.delete(opportunity.id());
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM oportunidades.oportunidade
                WHERE id = ? AND deleted_at IS NOT NULL
                """, Long.class, opportunity.id())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM oportunidades.candidatura WHERE oportunidade_id = ?
                """, Long.class, opportunity.id())).isEqualTo(1L);
        assertThat(service.list(filter(), null, 20).items()).isEmpty();
        assertThatThrownBy(() -> service.findById(opportunity.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(opportunity.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void dirtyCheckingPersistsUpdatesAndIncrementsVersion() {
        var opportunity = create("Original");
        entityManager.flush();
        long before = jdbc.queryForObject(
                "SELECT version FROM oportunidades.oportunidade WHERE id = ?",
                Long.class, opportunity.id());

        service.update(opportunity.id(), updateRequest());
        entityManager.flush();
        entityManager.clear();

        var reloaded = service.findById(opportunity.id());
        assertThat(reloaded.title()).isEqualTo("Updated title");
        assertThat(reloaded.requirements()).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT version FROM oportunidades.oportunidade WHERE id = ?",
                Long.class, opportunity.id())).isGreaterThan(before);
    }

    @Test
    void readsLongTextAndAnEmptyLastPage() {
        var opportunity = create("Long description");
        entityManager.flush();
        entityManager.clear();
        assertThat(service.findById(opportunity.id()).description().length()).isGreaterThan(255);
        var page = service.list(filter(), null, 1);
        assertThat(page.items()).hasSize(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        service.delete(opportunity.id());
        assertThat(service.list(filter(), null, 1).items()).isEmpty();
    }
}
```

Os inserts SQL acima são fixtures de teste: não mudam o schema e rodam numa transação revertida ao terminar cada teste.
O boolean autorizado=false prova que, conforme combinado, não estamos exigindo autorização do recrutador.

A classe usa service e repository reais. Ela verifica o filtro de soft delete, a atualização sem save, o armazenamento
de textos longos e a paginação no PostgreSQL com registros empatados.

### Como iniciar o banco isolado

Com Docker Desktop em execução, crie um container exclusivo:

```
docker run --name linkedufma-opportunity-test -e POSTGRES_DB=linkedufma_opportunity_test -e POSTGRES_USER=opportunity_test -e POSTGRES_PASSWORD=opportunity_test -p 127.0.0.1:55432:5432 -d postgres:17
docker exec linkedufma-opportunity-test pg_isready -U opportunity_test -d linkedufma_opportunity_test
```

Espere o pg_isready indicar accepting connections. Se o container já existir de uma execução anterior, use docker start
linkedufma-opportunity-test em vez de repetir docker run.

Execute:

```
.\mvnw.cmd -Pintegration verify
```

As migrations criam a estrutura nesse banco e o Hibernate a valida. Os dados inseridos por cada teste são revertidos; o
schema permanece para uma próxima execução.

Ao terminar, você pode parar apenas esse container:

```
docker stop linkedufma-opportunity-test
```

Não use comandos de limpeza contra o container de desenvolvimento ou o banco compartilhado.

## 12. Execução manual e checklist

### Preparar o ambiente de desenvolvimento

Para a aplicação, mantenha as propriedades atuais de datasource. Configure SUPABASE_DB_URL, SUPABASE_DB_USERNAME e
SUPABASE_DB_PASSWORD com o banco de desenvolvimento desejado; apesar do nome das variáveis, a URL JDBC também pode
apontar para PostgreSQL local.

Para o plugin Maven, configure separadamente LIQUIBASE_COMMAND_URL, LIQUIBASE_COMMAND_USERNAME e
LIQUIBASE_COMMAND_PASSWORD para **o mesmo banco**. Valores não são intercambiados automaticamente pelo projeto atual.

As credenciais permanecem em variáveis de ambiente. Um arquivo .env não é carregado automaticamente pelo Spring Boot;
use a configuração de execução da IDE ou exporte as variáveis no terminal.

Depois da revisão da migration:

```
.\mvnw.cmd liquibase:validate
.\mvnw.cmd liquibase:updateSQL
.\mvnw.cmd liquibase:update
.\mvnw.cmd spring-boot:run
```

O starter também aplicará migrations pendentes ao iniciar. Executar update previamente torna a conferência manual mais
explícita. Não há necessidade de Redis para este CRUD.

### Obter um recrutador para o teste manual

O cadastro de recrutadores não é implementado aqui. Utilize o ID de um recrutador que já exista no banco de
desenvolvimento:

```
SELECT usuario_id, nome_organizacao
FROM perfil.recrutador
ORDER BY usuario_id;
```

Se o banco local estiver vazio, use esta fixture exclusivamente nele. Não execute na base compartilhada ou de produção:

```
WITH inserted_user AS (
    INSERT INTO app_auth.usuario
        (name, email, senha_hash, active, criado_em, atualizado_em)
    VALUES
        ('Local recruiter', 'recruiter-crud@example.test',
         'unused-local-fixture', true, now(), now())
    RETURNING id
)
INSERT INTO perfil.recrutador
    (usuario_id, autorizado, nome_organizacao, tipo, criado_em, atualizado_em)
SELECT id, false, 'UFMA local', 'PROFESSOR', now(), now()
FROM inserted_user
RETURNING usuario_id;
```

Execute uma única vez e anote o usuario_id retornado. Essa fixture não fornece uma senha utilizável e não faz parte da
implementação de autenticação futura.

### Exercitar a API pelo PowerShell

O exemplo gera datas relativas ao momento da execução para não expirar com o tempo. Troque 7 pelo ID real obtido no
passo anterior.

```
$api = 'http://localhost:8080'
$recruiterId = 7
$startsAt = [DateTimeOffset]::UtcNow.AddMinutes(-5).ToString('o')
$endsAt = [DateTimeOffset]::UtcNow.AddDays(7).ToString('o')

$createBody = @{
    recruiterId = $recruiterId
    title = 'Research assistant'
    description = 'Support academic research activities.'
    requirements = 'Basic Java knowledge.'
    modality = 'IC'
    location = 'São Luís'
    vacancyCount = 2
    registrationStartsAt = $startsAt
    registrationEndsAt = $endsAt
} | ConvertTo-Json

$created = Invoke-RestMethod -Method Post -Uri "$api/opportunities" -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($createBody))
$opportunityId = $created.id

Invoke-RestMethod -Method Get -Uri "$api/opportunities/$opportunityId"

$updateBody = @{
    title = 'Updated research assistant'
    description = 'Updated academic research activities.'
    modality = 'IC'
    vacancyCount = 3
    registrationStartsAt = $startsAt
    registrationEndsAt = $endsAt
} | ConvertTo-Json

Invoke-RestMethod -Method Put -Uri "$api/opportunities/$opportunityId" -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($updateBody))
Invoke-RestMethod -Method Post -Uri "$api/opportunities/$opportunityId/publish"

$firstPage = Invoke-RestMethod -Method Get -Uri "$api/opportunities?limit=2&modality=IC"
$firstPage

if ($firstPage.hasNext) {
    $encodedCursor = [Uri]::EscapeDataString($firstPage.nextCursor)
    Invoke-RestMethod -Method Get -Uri "$api/opportunities?limit=2&modality=IC&cursor=$encodedCursor"
}

Invoke-RestMethod -Method Post -Uri "$api/opportunities/$opportunityId/close"
Invoke-RestMethod -Method Delete -Uri "$api/opportunities/$opportunityId"

# Expected: HTTP 404 after deletion.
Invoke-RestMethod -Method Get -Uri "$api/opportunities/$opportunityId"
```

Para exercitar mais de uma página, crie pelo menos três oportunidades. O mesmo filtro modality=IC foi mantido ao enviar
nextCursor.

No frontend, ao trocar filtros, limpe a lista e o cursor. Acrescente novos itens somente após concluir a requisição
anterior; descarte respostas antigas quando os filtros mudarem. Não calcule nem interprete o cursor no cliente.

### Swagger

Abra http://localhost:8080/swagger-ui/index.html. A descrição JSON fica em http://localhost:8080/v3/api-docs. Confira
enums, campos obrigatórios, respostas e os exemplos de erro antes de integrar um frontend.

### Checklist de conclusão

- [ ] Maven compila e gera o mapper.
- [ ] Testes rápidos passam sem banco.
- [ ] Smoke test original foi movido para IT com perfil integration.
- [ ] Testes de integração passam em PostgreSQL isolado.
- [ ] Migration foi gerada como rascunho, revisada e versionada depois da migration inicial.
- [ ] Nenhuma migration antiga foi alterada.
- [ ] Todas as operações funcionam sem autenticação.
- [ ] PUT não altera recrutador, status ou campos de auditoria.
- [ ] GET, PUT e ações retornam 404 para excluídas.
- [ ] DELETE mantém a linha e candidaturas; não existe caminho de hard delete no service.
- [ ] Cursor usa o último item retornado e respeita os mesmos filtros.
- [ ] Scroll com datas iguais não repete nem pula registros de um conjunto estável.
- [ ] Registros novos no topo aparecem após reiniciar o scroll.
- [ ] Erros de validação são 400, ausências 404 e conflitos de estado 409.
- [ ] Swagger corresponde ao contrato implementado.

### Perguntas para estudar enquanto implementa

1. Por que o service consulta o recrutador, mas a entidade valida a transição de estado?
2. O que seria possível fazer indevidamente se existisse setStatus?
3. Por que updatedAt não é uma boa chave para esta paginação?
4. Por que o ID precisa acompanhar createdAt no cursor?
5. Por que buscamos um registro a mais?
6. Por que fechar uma oportunidade e excluí-la são operações diferentes?
7. Por que uma entidade gerenciada não precisa de save após cada mudança?
8. Qual problema @Version resolve quando edição e exclusão ocorrem simultaneamente?
9. Por que uma candidatura histórica ainda deve poder referenciar uma oportunidade excluída?
10. Por que DTOs e mapper não devem substituir os métodos de negócio?

O objetivo é que você implemente cada etapa entendendo seu papel. A ordem deste documento permite montar a solução
progressivamente; só inicie a aplicação quando entidades, migration e dependências estiverem coerentes.
