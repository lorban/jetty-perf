package org.eclipse.jetty.perf.jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.apache.maven.toolchain.model.io.xpp3.MavenToolchainsXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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
        try
        {
            String jdkHome = findJavaHomeFromToolchain(fileSystem, hostname);
            if (StringUtils.isNotEmpty(jdkHome))
            {
                LOG.debug("host '{}' found jdkHome '{}' from toolchain", hostname, jdkHome);
                Path javaExec = Paths.get(jdkHome).resolve("bin").resolve("java"); // *nix
                if (!Files.isExecutable(javaExec))
                    javaExec = Paths.get(jdkHome).resolve("Contents").resolve("Home").resolve("bin").resolve("java"); // OSX
                if (!Files.isExecutable(javaExec))
                    javaExec = Paths.get(jdkHome).resolve("bin").resolve("java.exe"); // Windows
                if (Files.isExecutable(javaExec))
                {
                    // it's coming from toolchains so we trust the result
                    String absolutePath = javaExec.toAbsolutePath().toString();
                    if (LOG.isDebugEnabled())
                        LOG.debug("host '{}' will use java executable {}", hostname, absolutePath);
                    return absolutePath;
                }
            }
            else
            {
                LOG.debug("cannot find jdkHome from toolchain for host {}", hostname);
            }
        }
        catch (IOException x)
        {
            LOG.debug("ignore error searching from toolchains file", x);
        }
        Path jdkFolderFile = fileSystem.getPath("jenkins_home", "tools", "hudson.model.JDK", toolName);
        if (!Files.isDirectory(jdkFolderFile))
            jdkFolderFile = fileSystem.getPath("tools", "hudson.model.JDK", toolName);
        if (!Files.isDirectory(jdkFolderFile))
            throw new RuntimeException("Jenkins tool '" + toolName + "' not found in " + jdkFolderFile.toAbsolutePath());
        try
        {
            String executable = Files.walk(jdkFolderFile, 2)
                .filter(path ->
                    Files.isExecutable(path.resolve("bin").resolve("java")) || Files.isExecutable(path.resolve("bin").resolve("java.exe")))
                .map(path ->
                {
                    Path resolved = path.resolve("bin").resolve("java");
                    if (!Files.isExecutable(resolved))
                        resolved = path.resolve("bin").resolve("java.exe");
                    return resolved.toAbsolutePath().toString();
                })
                .findAny()
                .orElseThrow(() -> new RuntimeException("Jenkins tool '" + toolName + "' not found"));
            if (LOG.isDebugEnabled())
                LOG.debug("Found java executable in Jenkins Tools '{}' of machine '{}' at {}", toolName, hostname, executable);
            LOG.debug("host {} will use java executable {}", hostname, executable);
            return executable;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Jenkins tool '" + toolName + "' not found", e);
        }
    }

    protected String findJavaHomeFromToolchain(FileSystem fileSystem, String hostname) throws IOException
    {
        String fileName = hostname + "-toolchains.xml";
        Path toolchainsPath = Paths.get(fileName);
        if (Files.exists(toolchainsPath))
        {
            MavenToolchainsXpp3Reader toolChainsReader = new MavenToolchainsXpp3Reader();
            try (InputStream inputStream = Files.newInputStream(toolchainsPath))
            {
                PersistedToolchains toolchains = toolChainsReader.read(inputStream);
                @SuppressWarnings("unchecked")
                String s = (String)toolchains.getToolchains().stream().filter(o ->
                {
                    ToolchainModel toolchainModel = ((ToolchainModel)o);
                    if ("jdk".equals(toolchainModel.getType()))
                    {
                        Xpp3Dom provides = (Xpp3Dom)toolchainModel.getProvides();
                        if (provides != null)
                        {
                            Xpp3Dom version = provides.getChild("version");
                            if (version != null && StringUtils.equals(version.getValue(), this.toolName))
                            {
                                return true;
                            }
                        }
                    }
                    return false;
                }).map(o ->
                {
                    ToolchainModel toolchainModel = ((ToolchainModel)o);
                    Xpp3Dom configuration = (Xpp3Dom)toolchainModel.getConfiguration();
                    Xpp3Dom jdkHome = configuration.getChild("jdkHome");
                    return (jdkHome != null ? jdkHome.getValue() : null);
                }).findFirst().orElse(null);
                return s;
            }
            catch (XmlPullParserException x)
            {
                throw new IOException(x);
            }
        }
        else
        {
            LOG.debug("cannot find toolchain file {}", toolchainsPath);
            LOG.debug("files in directory: {}", Arrays.asList(Paths.get( ".").toFile().listFiles()));
        }
        return null;
    }

}