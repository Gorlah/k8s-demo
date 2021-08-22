FROM adoptopenjdk/openjdk11:alpine-jre
WORKDIR /opt/app
COPY ./build/libs/k8s-demo.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
