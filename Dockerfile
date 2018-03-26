FROM openjdk:8-jdk-slim
RUN apt-get update && apt-get install -y unzip maven curl \
    && rm -rf /var/lib/apt/lists/*
RUN curl -sLO https://github.com/google/protobuf/releases/download/v3.5.1/protoc-3.5.1-linux-x86_64.zip && \
    unzip protoc-3.5.1-linux-x86_64.zip -d protoc3 && \
    mv protoc3/bin/* /usr/local/bin/ && \
    mv protoc3/include/* /usr/local/include/ && \
    rm protoc-3.5.1-linux-x86_64.zip
COPY . .
RUN mvn package

ENTRYPOINT [ "java", "-jar", "registry/target/protoman-registry-jar-with-dependencies.jar", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap" ]