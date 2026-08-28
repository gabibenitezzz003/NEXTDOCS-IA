FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl fontconfig fonts-liberation \
	&& rm -rf /var/lib/apt/lists/* \
	&& groupadd --system --gid 10001 nextdocs \
	&& useradd --system --uid 10001 --gid nextdocs --home /app --shell /usr/sbin/nologin nextdocs
WORKDIR /app
COPY --from=compilacion /src/target/nextdocs-ai.jar app.jar
USER nextdocs
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
	CMD curl -fsS http://127.0.0.1:8090/actuator/health
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
