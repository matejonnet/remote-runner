package org.jboss.remotebuilder.core;

import org.jboss.pnc.buildagent.api.TaskStatusUpdateEvent;
import org.jboss.pnc.buildagent.client.BuildAgentClient;
import org.jboss.pnc.buildagent.client.Client;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class RemoteTty {

    BuildAgentClient buildAgentClient;
    Semaphore activeCommand = new Semaphore(1);

    public RemoteTty(URI remote) throws TimeoutException, InterruptedException, MalformedURLException {
        Consumer<String> responseConsumer = (message) -> System.out.print(message);

        Consumer<TaskStatusUpdateEvent> onStatusUpdate = (event) -> {
            System.out.println(event.getNewStatus());
        };

        String terminalUrl = remote + Client.WEB_SOCKET_TERMINAL_PATH;
        String statusUpdatesUrl = remote + Client.WEB_SOCKET_LISTENER_PATH;
        buildAgentClient = new BuildAgentClient(terminalUrl, statusUpdatesUrl, Optional.of(responseConsumer), onStatusUpdate, "");
        buildAgentClient.setCommandCompletionListener(() -> activeCommand.release());
    }

    public void execute(String command) throws TimeoutException, InterruptedException {
        activeCommand.acquire();
        buildAgentClient.executeCommand(command);
        activeCommand.acquire();
    }
}
