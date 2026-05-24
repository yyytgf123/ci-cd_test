# 1) agent 다운로드 stage
FROM curlimages/curl:8.5.0 AS otel
ARG OTEL_AGENT_VER=2.20.1

WORKDIR /tmp
RUN curl -fL --retry 3 -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VER}/opentelemetry-javaagent.jar \
  && test -s opentelemetry-javaagent.jar

# 2) 런타임
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY service/user/build/libs/*.jar app.jar

COPY --from=otel /tmp/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
