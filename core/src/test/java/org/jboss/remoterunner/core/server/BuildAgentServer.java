/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.remoterunner.core.server;

import org.jboss.pnc.buildagent.BuildAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BuildAgentServer {

    private static Thread serverThread;

    private static final Logger log = LoggerFactory.getLogger(BuildAgentServer.class);

    /**
     * Try to start the build agent and block until it is up and running.
     *
     * @return
     * @throws InterruptedException
     * @param host
     * @param port
     * @param bindPath
     */
    public static void startServer(String host, int port, String bindPath) throws InterruptedException {
        System.setProperty("java.net.preferIPv4Stack" , "true");
        Semaphore mutex = new Semaphore(1);
        Runnable onStart = () ->  {
            log.info("Server started.");
            mutex.release();
        };
        mutex.acquire();
        serverThread = new Thread(() -> {
            Optional<Path> logFolder = Optional.of(Paths.get("").toAbsolutePath());
            new BuildAgent().start(host, port, bindPath, logFolder, onStart);
        }, "termd-serverThread-thread");
        serverThread.start();

        mutex.acquire(); //wait server to boot
    }

    public static void stopServer() {
        log.info("Stopping server...");
        serverThread.interrupt();
    }

}
