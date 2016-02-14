package org.jboss.remoterunner.core;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Main {
    public static void main(String[] args) throws ParseException, InterruptedException, TimeoutException, IOException, URISyntaxException {

        Options options = new Options();
        options.addOption("c", true, "Command to execute.");
        options.addOption("u", true, "Remote endpoint.");
        options.addOption("p", true, "Remote root path.");
        options.addOption("s", false, "Sync folder (push local files to remote location).");
        options.addOption("h", false, "Print this help message.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("...", options);
            return;
        }

        String command = getOption(cmd, "c", "pwd");
        String remoteUri = getOption(cmd, "u", "http://localhost:8080");
        String remoteRootPath = getOption(cmd, "p", ""); //TODO update build agent to accept relative paths

        if (cmd.hasOption("s")) {
            FolderSync folderSync = new FolderSync(new URI(remoteUri + "/"), remoteRootPath);
            folderSync.push();
        }

        RemoteTty remoteTty = new RemoteTty(new URI(remoteUri));
        remoteTty.execute(command);
    }

    private static String getOption(CommandLine cmd, String opt, String defaultValue) {
        if (cmd.hasOption(opt)) {
            return cmd.getOptionValue(opt);
        } else {
            return defaultValue;
        }
    }
    private static Option longOption(String longOpt, String description) {
        return Option.builder().longOpt(longOpt)
                .desc(description)
                .build();
    }

}
