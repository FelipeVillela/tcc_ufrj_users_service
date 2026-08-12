# ==========================================================================
# Build multi-stage — todo o Java 25 vive DENTRO da imagem.
# O host não precisa de JDK 25 (nem de Maven): pode seguir com o JDK 17 do
# Android. O build compila o projeto com JDK 25 e o runtime também roda no 25.
# ==========================================================================

# ---- Estágio 1: build (compila com JDK 25 usando o Maven Wrapper) --------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# OBRIGATÓRIO: sem 'unzip' o mvnw baixa o Maven em .tar.gz em vez de .zip, mas
# compara o resultado com o distributionSha256Sum do .zip — e falha com
# "Failed to validate Maven distribution SHA-256 ... might be compromised".
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copia o código e empacota (fast-jar do Quarkus em target/quarkus-app/).
# O cache mount guarda o ~/.m2 entre builds, então rebuilds não rebaixam a
# internet inteira.
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B clean package -DskipTests

# ---- Estágio 2: runtime (só o JRE 25 + o app empacotado) -----------------
# Se a tag 25-jre não existir no seu registry, troque por eclipse-temurin:25-jdk
FROM eclipse-temurin:25-jre
WORKDIR /app

# Layout fast-jar do Quarkus: lib primeiro (muda pouco) e app por último
COPY --from=build /build/target/quarkus-app/lib/ /app/lib/
COPY --from=build /build/target/quarkus-app/*.jar /app/
COPY --from=build /build/target/quarkus-app/app/ /app/app/
COPY --from=build /build/target/quarkus-app/quarkus/ /app/quarkus/

# Não roda como root (boa prática; o app não precisa de privilégio nenhum)
RUN useradd -r -u 1001 -m quarkus && chown -R quarkus:quarkus /app
USER quarkus

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
# 'exec' faz o java virar o PID 1 e receber o SIGTERM do 'docker stop'
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/quarkus-run.jar"]
