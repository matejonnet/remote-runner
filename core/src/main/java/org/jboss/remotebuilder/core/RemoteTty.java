package org.jboss.remotebuilder.core;

import org.jboss.pnc.buildagent.api.TaskStatusUpdateEvent;
import org.jboss.pnc.buildagent.client.BuildAgentClient;
import org.jboss.pnc.buildagent.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class RemoteTty {

    Logger log = LoggerFactory.getLogger(RemoteTty.class);

    BuildAgentClient buildAgentClient;
    Semaphore activeCommand = new Semaphore(1);

    public RemoteTty(URI remote) throws TimeoutException, InterruptedException, IOException {
        Consumer<String> responseConsumer = (message) -> System.out.print(message);

        Consumer<TaskStatusUpdateEvent> onStatusUpdate = (event) -> {
            System.out.println(event.getNewStatus());
        };

        String terminalUrl = remote + Client.WEB_SOCKET_TERMINAL_PATH;
        String statusUpdatesUrl = remote + Client.WEB_SOCKET_LISTENER_PATH;
        buildAgentClient = new BuildAgentClient(terminalUrl, statusUpdatesUrl, Optional.of(responseConsumer), onStatusUpdate, "");
        buildAgentClient.setCommandCompletionListener(() -> activeCommand.release());
        new Thread(captureStdIn()).start();
    }

    private Runnable captureStdIn() throws IOException, TimeoutException {
        return () -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            StringBuilder stringBuilder = new StringBuilder();
            while (true) {
                int ch = 0;
                try {
                    ch = br.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //if (ch == 25) { //ctrl+Z (must be followed by return '10')
                if (ch == -1) { //ctrl+D
                    try {
                        buildAgentClient.executeNow('C' - 64);
                    } catch (Exception e) {
                        e.printStackTrace(); //TODO
                    }
                } else if (ch == 10) { //newLine
                    try {
                        String command = stringBuilder.toString();
                        stringBuilder = new StringBuilder();
                        if (command.length() > 0) {
                            buildAgentClient.executeNow(command);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); //TODO
                    }
                } else {
                    stringBuilder.append(Character.toChars(ch));
                }
            }
        };
    }

    public void execute(String command) throws TimeoutException, InterruptedException {
        activeCommand.acquire();
        buildAgentClient.executeCommand(command);
        activeCommand.acquire();
    }
}
