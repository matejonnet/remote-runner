package org.jboss.remotebuilder.core.server;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        BuildAgentServer.startServer("localhost", 8080, "");
    }
}
