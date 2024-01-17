FROM openjdk:8-jdk-alpine

RUN apk add --no-cache maven
COPY ./lib/ /home/lib/
COPY ./src/ /home/src/
COPY pom.xml /home/
WORKDIR /home/
RUN mvn clean install 

EXPOSE 4001
ENTRYPOINT ["java", "-Dthorntail.http.port=4001", "-Djava.net.preferIPv4Stack=true", "-jar", "/home/target/alloy4fun-api-thorntail.jar"]


