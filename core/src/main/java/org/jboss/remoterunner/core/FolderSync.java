package org.jboss.remoterunner.core;

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
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class FolderSync {

    Logger log = LoggerFactory.getLogger(FolderSync.class);

    private static final String UPLOAD_PATH = "servlet/upload";
    private final URI baseServerUri;
    private final String remoteRootPath;
    private final Path localRoot = Paths.get(".").toAbsolutePath();

    List<String> ignoredPaths = new ArrayList<>();

    public FolderSync(URI baseServerUri, String remoteRootPath) {
        this.baseServerUri = baseServerUri;
        this.remoteRootPath = remoteRootPath;
        initializeIgnoredPaths();
    }

    public void push() throws IOException {
        Path rootDir = Paths.get(".");
        Files.walkFileTree(rootDir, dirUploader());
    }

    private void upload(Path file) throws IOException {
        String targetPath = UPLOAD_PATH + remoteRootPath + "/" + localRoot.relativize(file.toAbsolutePath()).toString();
        URI uploadUri = baseServerUri.resolve(targetPath);

        HttpURLConnection connection = (HttpURLConnection) uploadUri.toURL().openConnection();
        connection.setRequestMethod("PUT");

        connection.setDoOutput(true);
        connection.setDoInput(true);

        connection.setRequestProperty("Content-Length", "" + Long.toString(file.toFile().length()));

        try (OutputStream outputStream = connection.getOutputStream()) {
            try (InputStream inputStream = new FileInputStream(file.toFile())) {
                byte[] buffer = new byte[512];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
            }
        }

        if(200 != connection.getResponseCode()) {
            log.error("File upload failed with code {}.", connection.getResponseCode());
            log.debug("File upload failed: {}.", connection.getResponseMessage());
        } else {
            log.info("Uploaded file {}.", file);
        }
    }

    private SimpleFileVisitor<Path> dirUploader() {
        return new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(!attrs.isDirectory()){
                    if (!isIgnored(file)) {
                        upload(file);
                    } else {
                        log.debug("Skipping ignored file: {}", file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        };
    }

    private void initializeIgnoredPaths() {
        ignoredPaths.add(".git/");
    }

    private boolean isIgnored(Path file) {
        for (String ignored : ignoredPaths) {
            return file.normalize().startsWith(ignored);
        };
        return false;
    }


}
