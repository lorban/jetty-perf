package perf;

import org.junit.jupiter.api.Test;
import org.mortbay.jetty.orchestrator.Cluster;
import org.mortbay.jetty.orchestrator.NodeArray;
import org.mortbay.jetty.orchestrator.NodeArrayFuture;
import org.mortbay.jetty.orchestrator.configuration.ClusterConfiguration;
import org.mortbay.jetty.orchestrator.configuration.Jvm;
import org.mortbay.jetty.orchestrator.configuration.Node;
import org.mortbay.jetty.orchestrator.configuration.SimpleClusterConfiguration;
import org.mortbay.jetty.orchestrator.configuration.SimpleNodeArrayConfiguration;
import org.mortbay.jetty.orchestrator.configuration.SshRemoteHostLauncher;
import perf.jenkins.JenkinsToolJdk;

public class AllMachinesTest
{
    @Test
    public void testAllMachines() throws Exception
    {
        String jdkName = System.getProperty("test.jdk.name", "load-jdk11");
        ClusterConfiguration cfg = new SimpleClusterConfiguration()
            .jvm(new Jvm(new JenkinsToolJdk(jdkName)))
            .nodeArray(new SimpleNodeArrayConfiguration("server")
                .node(new Node("1", "load-master"))
            )
            .nodeArray(new SimpleNodeArrayConfiguration("loaders")
                .node(new Node("1", "load-1"))
                .node(new Node("2", "load-2"))
                .node(new Node("3", "load-3"))
                .node(new Node("4", "load-4"))
                .node(new Node("5", "load-5"))
                .node(new Node("6", "load-6"))
            )
            .nodeArray(new SimpleNodeArrayConfiguration("probe")
                .node(new Node("1", "zwerg"))
            );

        {
            String javaVersion = System.getProperty("java.version");
            String username = System.getProperty("user.name");
            System.out.println("jenkins running java " + javaVersion + " with user " + username);
        }

        try (Cluster cluster = new Cluster(cfg))
        {
            NodeArray serverArray = cluster.nodeArray("server");
            NodeArray loadersArray = cluster.nodeArray("loaders");
            NodeArray probeArray = cluster.nodeArray("probe");

            NodeArrayFuture serverFuture = serverArray.executeOnAll(tools ->
            {
                String javaVersion = System.getProperty("java.version");
                String username = System.getProperty("user.name");
                System.out.println("server running java " + javaVersion + " with user " + username);
            });
            NodeArrayFuture loadersFuture = loadersArray.executeOnAll(tools ->
            {
                String javaVersion = System.getProperty("java.version");
                String username = System.getProperty("user.name");
                System.out.println("loaders running java " + javaVersion + " with user " + username);
            });
            NodeArrayFuture probeFuture = probeArray.executeOnAll(tools ->
            {
                String javaVersion = System.getProperty("java.version");
                String username = System.getProperty("user.name");
                System.out.println("probe running java " + javaVersion + " with user " + username);
            });

            serverFuture.get();
            loadersFuture.get();
            probeFuture.get();
        }
    }
}
