Example usage:
mvn clean install

run build-agent-server on localhost

java -jar core/target/core-0.1-SNAPSHOT-jar-with-dependencies.jar -c "mvn clean install -DskipTests" -p <local-path-to-build-agent-server-root>
