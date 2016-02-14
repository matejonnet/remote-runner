package org.jboss.remoterunner.core;

import org.jboss.remoterunner.core.server.BuildAgentServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ExecuteRemoteCommandTest {

    Logger log = LoggerFactory.getLogger(ExecuteRemoteCommandTest.class);

    @BeforeClass
    public static void startLocalServer() throws InterruptedException {
        BuildAgentServer.startServer("localhost", 8080, "");
    }

    @AfterClass
    public static void stopLocalServer() {
        //BuildAgentServer.stopServer();
    }

    @Test
    public void shouldExecuteRemoteCommandAndPrintTheOutput() throws InterruptedException, TimeoutException, IOException {
        //given
        RemoteTty remoteTty = new RemoteTty(URI.create("http://localhost:8080"));

        // Create a stream to hold the output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        hookOnStdout(baos);
        PrintStream ps = new PrintStream(baos);
        // IMPORTANT: Save the old System.out!
        PrintStream old = System.out;
        // Tell Java to use your special stream
        System.setOut(ps);


        //when
        log.info("Executing command pwd...");
        remoteTty.execute("pwd");

        //expect
        String console = baos.toString();

        // Put things back
        System.out.flush();
        System.setOut(old);

        String expected = Paths.get(".").toAbsolutePath().normalize().toString();
        Assert.assertTrue("Invalid response: [" + console + "] Expected {" + expected + "}", console.contains(expected));
    }

    private void hookOnStdout(ByteArrayOutputStream baos) {
        PrintStream ps = new PrintStream(baos);
        // IMPORTANT: Save the old System.out!
        PrintStream old = System.out;
        // Tell Java to use your special stream
        System.setOut(ps);

        // Put things back
        System.out.flush();
        System.setOut(old);
    }
}
