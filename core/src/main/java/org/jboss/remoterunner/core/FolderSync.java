package org.jboss.remoterunner.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.Properties;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class FolderSync {

    Logger log = LoggerFactory.getLogger(FolderSync.class);

    private static final String UPLOAD_PATH = "servlet/upload";
    private final URI baseServerUri;
    private final String remoteRootPath;
    private boolean copyNonUpdated;
    private final Path localRoot = Paths.get(".").toAbsolutePath();

    List<String> ignoredPaths = new ArrayList<>();
    Properties properties;
    private long lastSync;

    public FolderSync(URI baseServerUri, String remoteRootPath, boolean copyNonUpdated) {
        this.baseServerUri = baseServerUri;
        this.remoteRootPath = remoteRootPath;
        this.copyNonUpdated = copyNonUpdated;
        initializeIgnoredPaths();
        loadAndUpdateProperties();
    }

    private void loadAndUpdateProperties() {
        Properties defaults = new Properties();
        defaults.put("lastSync", "0");

        properties = new Properties(defaults);
        File propertiesFile = new File("remote-runner.properties");
        if (propertiesFile.exists()) {
            try (InputStream propertiesStream = new FileInputStream(propertiesFile)) {
                properties.load(propertiesStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        lastSync = Long.parseLong(properties.getProperty("lastSync"));
        properties.put("lastSync", Long.toString(System.currentTimeMillis()));

        try (OutputStream propertiesStream = new FileOutputStream(propertiesFile)){
            properties.store(propertiesStream, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void push() throws IOException {
        Path rootDir = Paths.get(".");
        Files.walkFileTree(rootDir, dirUploader());
    }

    private void upload(Path file) throws IOException {
        log.info("Uploading file {} ...", file);

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
            log.debug("File {} uploaded.", file);
        }
    }

    private SimpleFileVisitor<Path> dirUploader() {
        return new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(!attrs.isDirectory()){
                    if (!isIgnored(file)) {
                        if (copyNonUpdated || wasUpdated(attrs)) {
                            upload(file);
                        } else {
                            log.debug("Skipping non updated file: {}", file);
                        }
                    } else {
                        log.debug("Skipping ignored file: {}", file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        };
    }

    private boolean wasUpdated(BasicFileAttributes attrs) {
        return attrs.lastModifiedTime().toMillis() > lastSync;
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
