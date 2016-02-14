Remote Runner
=============

A client to sync your local workspace with a remote server, execute commands on remote server and show live command output in a local console.

No ssh required, Remote Runner uses Http and WebSockets to communicate with remote endpoint.

Example usage:
mvn clean install

run build-agent-server (https://github.com/project-ncl/pnc-build-agent) on localhost

java -jar core/target/core-0.1-SNAPSHOT-jar-with-dependencies.jar -c "mvn clean install -DskipTests" -p <local-path-to-build-agent-server-root>
