package org.eclipse.jetty.perf.jdk;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.jetty.perf.util.JvmUtil;
import org.mortbay.jetty.orchestrator.util.FilenameSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsToolJdk implements FilenameSupplier
{
    private static final Logger LOG = LoggerFactory.getLogger(JenkinsToolJdk.class);

    private final String toolName;

    public JenkinsToolJdk(String toolName)
    {
        this.toolName = toolName;
    }

    @Override
    public String get(FileSystem fileSystem, String hostname)
    {
        Path jdkFolderFile = fileSystem.getPath("jenkins_home", "tools", "hudson.model.JDK", toolName);
        if (!Files.isDirectory(jdkFolderFile))
            jdkFolderFile = fileSystem.getPath("tools", "hudson.model.JDK", toolName);
        if (!Files.isDirectory(jdkFolderFile))
            throw new RuntimeException("Jenkins tool '" + toolName + "' not found in " + jdkFolderFile.toAbsolutePath());
        try (Stream<Path> pathStream = Files.walk(jdkFolderFile, 2))
        {
            Path finalJdkFolderFile = jdkFolderFile;
            String executable = pathStream
                .map(path ->
                {
                    Path javaExecutableFromUtil = JvmUtil.findJavaExecutable(path.toString());
                    Path javaExecutable = path.resolve("bin").resolve("java"); // *nix
                    boolean exec = Files.isExecutable(javaExecutable);
                    boolean exists = Files.exists(javaExecutable);
                    System.out.println("Path: " + path + " javaExecutable: " + javaExecutable + " javaExecutableFromUtil: " + javaExecutableFromUtil + " exec? " + exec + " exists? " + exists);
                    if (!exec)
                        return null;
                    return javaExecutable;
                })
                .filter(Objects::nonNull)
                .map(path -> path.toAbsolutePath().toString())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Jenkins tool '" + toolName + "' not found in " + finalJdkFolderFile.toAbsolutePath()));
            if (LOG.isDebugEnabled())
                LOG.debug("Found java executable in Jenkins Tools '{}' of machine '{}' at {}", toolName, hostname, executable);
            return executable;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Jenkins tool '" + toolName + "' not found under " + fileSystem.getPath(".").toAbsolutePath(), e);
        }
    }
}
