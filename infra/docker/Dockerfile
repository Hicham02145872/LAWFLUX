FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
ARG SERVICE
RUN mvn -B -ntp -pl services/${SERVICE} -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S lawflux && adduser -S -G lawflux lawflux && mkdir -p /data/documents && chown lawflux:lawflux /data/documents
WORKDIR /app
ARG SERVICE
COPY --from=build /workspace/services/${SERVICE}/target/${SERVICE}-0.1.0-SNAPSHOT.jar /app/app.jar
USER lawflux
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
