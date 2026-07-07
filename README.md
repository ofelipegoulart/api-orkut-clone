# Orkut Clone — API

API REST que recria as funcionalidades sociais do saudoso **Orkut**: perfis, amigos,
comunidades, depoimentos, avaliações (estrelinhas), scraps (recados) e uma busca
universal por pessoas e comunidades. Construída com **Spring Boot 3** e **Java 21**,
com autenticação via **JWT** e persistência em **PostgreSQL**.

---

## Sumário

- [Stack](#stack)
- [Funcionalidades](#funcionalidades)
- [Pré-requisitos](#pré-requisitos)
- [Configuração do ambiente](#configuração-do-ambiente)
- [Executando o projeto](#executando-o-projeto)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)
- [Autenticação](#autenticação)
- [Principais endpoints](#principais-endpoints)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Testes](#testes)
- [Notas sobre a busca (unaccent)](#notas-sobre-a-busca-unaccent)
- [Upload de avatares](#upload-de-avatares)

---

## Stack

| Camada           | Tecnologia                                   |
|------------------|----------------------------------------------|
| Linguagem        | Java 21                                       |
| Framework        | Spring Boot 3.4                               |
| Segurança        | Spring Security + JWT (jjwt 0.12)             |
| Persistência     | Spring Data JPA + Hibernate                   |
| Banco de dados   | PostgreSQL (produção) · H2 em memória (testes)|
| Cache            | Spring Cache + Caffeine                       |
| Documentação     | springdoc-openapi (Swagger UI)               |
| Build            | Maven                                         |
| Utilitários      | Lombok, jsoup (sanitização de HTML)          |

---

## Funcionalidades

- **Autenticação** — cadastro e login com emissão de token JWT.
- **Perfis** — perfil dividido em seções (geral, social, contato, profissional e
  pessoal), avatar e uma visão consolidada (`overview`).
- **Amizades** — envio, aceite e recusa de solicitações; listagem e remoção de amigos.
- **Comunidades** — criação, entrada e saída.
- **Depoimentos** — envio, aprovação/recusa e listagem (enviados e recebidos).
- **Avaliações** — sistema de estrelinhas com média por usuário.
- **Scraps (recados)** — envio, edição, exclusão, threads de resposta, marcação como
  lido e contagem de não lidos.
- **Busca universal** — pesquisa por pessoas e comunidades com suporte a acentuação
  (`unaccent`), filtros de tipo, localização e idioma, com paginação.
- **Estatísticas de perfil** — snapshot agregado atualizado conforme a atividade.

---

## Pré-requisitos

Antes de começar, garanta que você tem instalado:

- **JDK 21** (LTS) — `java -version` deve reportar `21.x`.
- **Maven 3.9+** — `mvn -version`. *(O projeto não inclui o Maven Wrapper; use um Maven
  instalado localmente.)*
- **PostgreSQL 13+** em execução.
- **Git**.

---

## Configuração do ambiente

### 1. Clone o repositório

```bash
git clone <url-do-repositorio>
cd api-orkut-clone
```

### 2. Crie o banco de dados

Conecte-se ao PostgreSQL e crie a base:

```sql
CREATE DATABASE orkut_clone;
```

O schema é criado/atualizado automaticamente pelo Hibernate (`ddl-auto: update`) na
primeira execução — não há migrations manuais.

### 3. Variáveis de ambiente

A aplicação lê as credenciais e o segredo JWT de variáveis de ambiente. Um arquivo
`.env` já é usado localmente (e está no `.gitignore`). As variáveis suportadas:

| Variável             | Obrigatória | Padrão                          | Descrição                                             |
|----------------------|-------------|---------------------------------|-------------------------------------------------------|
| `DB_USERNAME`        | não         | `postgres`                      | Usuário do PostgreSQL.                                 |
| `DB_PASSWORD`        | não         | `postgres`                      | Senha do PostgreSQL.                                   |
| `JWT_SECRET`         | **sim**     | —                               | Chave secreta (Base64) usada para assinar os tokens.  |
| `AVATAR_STORAGE_DIR` | não         | `uploads/avatars`               | Diretório local onde os avatares são armazenados.     |
| `AVATAR_PUBLIC_URL`  | não         | `/uploads/avatars`              | Caminho público servido para os avatares.             |
| `CORS_ALLOWED_ORIGINS` *(via `cors.allowed-origins`)* | não | `http://localhost:3000,http://localhost:5173` | Origens permitidas para CORS. |

> ⚠️ **`JWT_SECRET` não tem padrão** — a aplicação não sobe sem ele. Gere um segredo
> forte em Base64, por exemplo:
>
> ```bash
> openssl rand -base64 32
> ```

Crie um arquivo `.env` na raiz (ou exporte as variáveis no seu shell):

```dotenv
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=<cole-aqui-o-segredo-gerado>
```

> A leitura automática do `.env` depende do seu ambiente/IDE. Se rodar direto pelo
> Maven, garanta que as variáveis estejam exportadas no shell (veja abaixo).

O JWT tem validade de **3 dias** (`259200000` ms), configurável em
`src/main/resources/application.yml`.

---

## Executando o projeto

Exportando as variáveis e subindo a aplicação:

```bash
# Linux/macOS
export DB_USERNAME=postgres DB_PASSWORD=postgres JWT_SECRET=<seu-segredo>
mvn spring-boot:run
```

```powershell
# Windows PowerShell
$env:DB_USERNAME="postgres"; $env:DB_PASSWORD="postgres"; $env:JWT_SECRET="<seu-segredo>"
mvn spring-boot:run
```

A API sobe em **`http://localhost:8080`**.

Para gerar o artefato e executar o `.jar`:

```bash
mvn clean package
java -jar target/api-0.0.1-SNAPSHOT.jar
```

---

## Documentação da API (Swagger)

Com a aplicação em execução, a documentação interativa fica disponível em:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

Esses caminhos são públicos (não exigem autenticação).

---

## Autenticação

Todos os endpoints, exceto `/auth/**`, os avatares (`GET /uploads/**`) e a documentação,
exigem um **token JWT**.

1. **Cadastre-se** — `POST /auth/register`
2. **Faça login** — `POST /auth/login` → retorna o token.
3. Envie o token no header em todas as demais chamadas:

```http
Authorization: Bearer <token>
```

A API é **stateless** (sem sessão de servidor); cada requisição é autenticada pelo token.

---

## Principais endpoints

> Prefixo base: `http://localhost:8080`

### Autenticação — `/auth`
| Método | Rota             | Descrição                    |
|--------|------------------|------------------------------|
| POST   | `/auth/register` | Cadastra um novo usuário     |
| POST   | `/auth/login`    | Autentica e retorna o token  |

### Usuários — `/users`
| Método | Rota          | Descrição                        |
|--------|---------------|----------------------------------|
| GET    | `/users/{id}` | Busca usuário por ID             |
| GET    | `/users/me`   | Dados do usuário autenticado     |
| PUT    | `/users/me`   | Atualiza o usuário autenticado   |

### Perfil — `/api/profile`
| Método | Rota                                          | Descrição                              |
|--------|-----------------------------------------------|----------------------------------------|
| GET    | `/api/profile/overview`                       | Visão consolidada do perfil            |
| GET/PATCH | `/api/profile/general`                     | Seção geral                            |
| GET/PATCH | `/api/profile/social`                      | Seção social                           |
| GET/PATCH | `/api/profile/contact`                     | Seção de contato                       |
| GET/PATCH | `/api/profile/professional`                | Seção profissional                     |
| GET/PATCH | `/api/profile/personal`                    | Seção pessoal                          |
| POST   | `/api/profile/avatar`                         | Envia avatar (multipart)               |
| DELETE | `/api/profile/avatar`                         | Remove avatar                          |
| POST   | `/api/profile/friends/{friendUserId}`         | Envia solicitação de amizade           |
| GET    | `/api/profile/friends/requests`               | Solicitações recebidas                 |
| GET    | `/api/profile/friends/requests/sent`          | Solicitações enviadas                  |
| POST   | `/api/profile/friends/requests/{id}/accept`   | Aceita solicitação                     |
| DELETE | `/api/profile/friends/requests/{id}`          | Recusa/cancela solicitação             |
| DELETE | `/api/profile/friends/{friendUserId}`         | Remove amizade                         |
| POST   | `/api/profile/communities`                    | Cria comunidade                        |
| POST   | `/api/profile/communities/{id}/join`          | Entra na comunidade                    |
| DELETE | `/api/profile/communities/{id}/leave`         | Sai da comunidade                      |
| POST   | `/api/profile/ratings/{targetUserId}`         | Avalia um usuário (estrelinhas)        |
| GET    | `/api/profile/ratings/{targetUserId}/average` | Média de avaliações                    |
| POST   | `/api/profile/testimonials/{targetUserId}`    | Envia depoimento                       |
| PATCH  | `/api/profile/testimonials/{id}/decision`     | Aprova/recusa depoimento               |
| GET    | `/api/profile/testimonials/sent`              | Depoimentos enviados                   |
| GET    | `/api/profile/testimonials/received`          | Depoimentos recebidos                  |

### Scraps (recados)
| Método | Rota                          | Descrição                          |
|--------|-------------------------------|------------------------------------|
| POST   | `/scraps`                     | Envia um scrap                     |
| GET    | `/scraps/sent`                | Scraps enviados                    |
| GET    | `/scraps/{id}`                | Detalhe de um scrap                |
| PUT    | `/scraps/{id}`                | Edita um scrap                     |
| DELETE | `/scraps/{id}`                | Exclui um scrap                    |
| DELETE | `/scraps`                     | Exclui scraps                      |
| GET    | `/scraps/{id}/thread`         | Thread de respostas                |
| PATCH  | `/scraps/mark-read`           | Marca scraps como lidos            |
| GET    | `/scraps/unread-count`        | Quantidade de não lidos            |
| GET    | `/users/{userId}/scraps`      | Scraps de um usuário               |

### Busca — `/search`
| Método | Rota      | Parâmetros                                                        |
|--------|-----------|------------------------------------------------------------------|
| GET    | `/search` | `q` (obrigatório), `type` (`all`), `location`, `language` (`pt-BR`), `page` (`1`), `size` (`12`) |

---

## Estrutura do projeto

```
src/main/java/com/orkutclone/api
├── ApiApplication.java        # classe principal (Spring Boot)
├── config/                    # segurança, cache, web, inicializador de busca
├── controller/                # endpoints REST
├── dto/                       # objetos de request/response
├── exception/                 # tratamento global de erros
├── model/                     # entidades JPA (+ enums)
├── repository/                # Spring Data + queries customizadas de busca
├── security/                  # filtro e utilidades JWT
├── service/                   # regras de negócio
├── support/                   # armazenamento de avatar, normalização de texto
└── validation/                # validações customizadas
```

---

## Testes

Os testes usam um banco **H2 em memória** (perfil `test`), sem necessidade de PostgreSQL:

```bash
mvn test
```

A configuração de teste fica em `src/test/resources/application-test.yml`.

---

## Notas sobre a busca (`unaccent`)

A busca é insensível a acentos. Para isso a aplicação precisa da função `unaccent`:

- **PostgreSQL** — na inicialização, a aplicação tenta habilitar a extensão automaticamente
  (`CREATE EXTENSION IF NOT EXISTS unaccent`). Se o usuário do banco não tiver permissão,
  um aviso é registrado e você deve rodar manualmente (uma vez) com um superusuário:

  ```sql
  CREATE EXTENSION unaccent;
  ```

- **H2 (testes)** — um alias equivalente é registrado automaticamente, apontando para
  `SearchText.unaccent`, para que a query nativa se comporte igual à do PostgreSQL.

---

## Upload de avatares

Os avatares são armazenados **localmente** (por padrão em `uploads/avatars`, ignorado
pelo Git) e servidos publicamente em `/uploads/avatars`. Ajuste o local e a URL pública
pelas variáveis `AVATAR_STORAGE_DIR` e `AVATAR_PUBLIC_URL`. O tamanho máximo de arquivo
é **10MB** (requisição até 12MB), configurável em `application.yml`.
