# tcc_ufrj_backend

Backend do TCC - Módulo Users Service. Cada serviço roda no seu próprio container

## Serviços

| Serviço | Stack | Bases | Porta |
|---|---|---|---|
| `bank-users-service` | Quarkus (Java 25, no container) | PostgreSQL + MongoDB | 8080 |

## Pré-requisito: liberar o Docker sem `sudo` (uma vez)

Aqui o Docker é instalado via **snap** e o daemon (`snap.docker.dockerd`) já
sobe sozinho no boot. O que falta é permissão: o socket
`/var/run/docker.sock` fica `root:root` e não existe grupo `docker`, então
`docker ps` responde `permission denied`.

```bash
sudo groupadd -f docker
sudo usermod -aG docker "$USER"

# o snap reaplica o dono do socket ao reiniciar
sudo snap disable docker && sudo snap enable docker

newgrp docker      # ou faça logout/login
docker ps          # deve funcionar sem sudo
```

Enquanto não fizer isso, é só prefixar os comandos com `sudo`.

## Rodando com Docker

Na raiz deste diretório:

```bash
docker compose up --build      # -d para rodar em background
```

Isso sobe três containers:

- **postgres** (usuários, chaves pix, saldo) na 5432
- **mongo** (lista de contatos) na 27017
- **bank-users-service** na 8080 — o build compila com JDK 25 dentro da imagem

O `docker-compose.yml` injeta as variáveis de conexão
(`QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_MONGODB_CONNECTION_STRING`) apontando
para os hostnames `postgres` e `mongo` da rede interna do compose, e só inicia
a aplicação depois que os healthchecks dos dois bancos passam.

> O **primeiro build demora alguns minutos** (baixa a imagem do JDK 25, o Maven
> e as dependências do Quarkus). Os seguintes reaproveitam o cache do `~/.m2`.

### Comandos do dia a dia

```bash
docker compose logs -f bank-users-service   # acompanhar os logs da app
docker compose restart bank-users-service   # reiniciar só a app
docker compose up --build -d bank-users-service   # rebuildar após mexer no código
docker compose ps                           # ver o que está de pé
docker compose down                         # para tudo (mantém os dados)
docker compose down -v                      # ...e apaga os volumes (banco zerado)
```

### Testando

Os dados de exemplo são populados no boot pelo `DevDataSeeder` (idempotente).

```bash
# usuários, saldo e chaves pix (PostgreSQL)
curl http://localhost:8080/users
curl http://localhost:8080/users/1
curl http://localhost:8080/users/1/saldo
curl http://localhost:8080/users/1/pix-keys

# lista de contatos (MongoDB)
curl http://localhost:8080/users/1/contacts
curl "http://localhost:8080/users/1/contacts/search?termo=mãe&por=nomeAlternativo"
curl "http://localhost:8080/users/1/contacts/search?termo=carla&por=nome"
```

Os exemplos completos, incluindo os `POST`, estão em
[bank-users-service/api-examples.http](bank-users-service/api-examples.http)
(basta abrir no VS Code com a extensão REST Client).

## Como o build funciona

O [Dockerfile](bank-users-service/Dockerfile) é multi-stage e é ditado pelo
`pom.xml` do serviço:

| No `pom.xml` | Consequência no Dockerfile |
|---|---|
| `<maven.compiler.release>25</...>` | imagem de build `eclipse-temurin:25-jdk` — o Java 25 fica dentro da imagem, não no host |
| `<packaging>quarkus</packaging>` + `quarkus-maven-plugin` | `mvnw package` gera o **fast-jar** em `target/quarkus-app/`, não um jar único |
| `quarkus-jdbc-postgresql` + `quarkus-mongodb-panache` | a app precisa de dois bancos → o compose sobe Postgres e Mongo |

- **Estágio 1 (build):** instala o `unzip` (o Maven Wrapper precisa dele — veja
  *Problemas conhecidos*) e roda `./mvnw package`. Um cache mount do BuildKit
  guarda o `~/.m2` entre builds, então só a primeira vez baixa as dependências.
- **Estágio 2 (runtime):** só o `eclipse-temurin:25-jre` mais o app empacotado.
  O fast-jar é copiado em quatro camadas — `lib/` (dependências, muda raramente)
  antes de `app/` (seu código, muda sempre) — então um rebuild só reescreve as
  camadas finais. Roda como usuário não-root (`quarkus`, uid 1001).

Se a tag `eclipse-temurin:25-jre` não existir no seu registry, troque pela
`eclipse-temurin:25-jdk` no estágio de runtime (há um comentário no arquivo).

## Desenvolvimento sem Docker (opcional)

Tudo nesta seção compila **no host**. Para rodar o projeto sem o Docker  é preciso um
JDK 25 local (ex.: `sdk install java 25-tem`) — caso contrário, fique no compose.

### Live-reload (`quarkus:dev`)

Para o loop de desenvolvimento, containerizar a app atrapalha mais que ajuda —
suba só os bancos e rode o Quarkus em modo dev:

```bash
docker compose up -d postgres mongo
cd bank-users-service && ./mvnw quarkus:dev
```

Em modo dev o Quarkus oferece a Dev UI em <http://localhost:8080/q/dev/>. Sem
os containers acima, os Dev Services do Quarkus sobem Postgres e Mongo sozinhos
(também via Docker) enquanto a sessão dev durar.

