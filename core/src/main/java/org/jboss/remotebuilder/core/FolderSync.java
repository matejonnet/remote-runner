package org.jboss.remotebuilder.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class FolderSync {

    Logger log = LoggerFactory.getLogger(FolderSync.class);

    private static final String UPLOAD_PATH = "servlet/upload";
    private final URI baseServerUri;
    private final Path localRoot = Paths.get(".");

    public FolderSync(URI baseServerUri) {
        this.baseServerUri = baseServerUri;
    }

    public void push() throws IOException {
        Path rootDir = Paths.get(".");
        Files.walkFileTree(rootDir, dirUploader());
    }

    private void upload(Path file) throws IOException {
        String targetPath = UPLOAD_PATH + file.relativize(localRoot);
        URI uploadUri = baseServerUri.resolve(targetPath);

        HttpURLConnection connection = (HttpURLConnection) uploadUri.toURL().openConnection();
        connection.setRequestMethod("PUT");

        connection.setDoOutput(true);
        connection.setDoInput(true);

        connection.setRequestProperty("Content-Length", "" + Long.toString(file.toFile().length()));

        try (OutputStream outputStream = connection.getOutputStream()) {
            try (InputStream inputStream = new FileInputStream(file.toFile())) {
                int fileByte;
                while ((fileByte = inputStream.read()) != -1) {
                    outputStream.write(fileByte);
                }
            }
        }

        if(200 != connection.getResponseCode()) {
            log.error("File upload failed with code {}.", connection.getResponseCode());
        }
    }

    private SimpleFileVisitor<Path> dirUploader() {
        return new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(!attrs.isDirectory()){
                    upload(file);
                }
                return FileVisitResult.CONTINUE;
            }
        };
    }

}
