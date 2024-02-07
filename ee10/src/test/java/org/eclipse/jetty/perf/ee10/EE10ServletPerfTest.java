package org.eclipse.jetty.perf.ee10;

import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.perf.test.Jetty12ClusteredPerfTest;
import org.eclipse.jetty.perf.test.ClusteredTestContext;
import org.eclipse.jetty.perf.test.junit.ClusteredTest;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.junit.jupiter.api.Test;

public class EE10ServletPerfTest
{
    @Test
    public void testNoGzipAsync(@ClusteredTest ClusteredTestContext clusteredTestContext) throws Exception
    {
        Jetty12ClusteredPerfTest.runTest(clusteredTestContext, () ->
        {
            ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
            ServletContextHandler targetContextHandler = new ServletContextHandler();
            targetContextHandler.setContextPath("/");
            targetContextHandler.addServlet(new AsyncEE10Servlet("Hi there!".getBytes(StandardCharsets.ISO_8859_1)), "/*");
            contextHandlerCollection.addHandler(targetContextHandler);
            ServletContextHandler uselessContextHandler = new ServletContextHandler();
            uselessContextHandler.setContextPath("/useless");
            uselessContextHandler.addServlet(new Always404Servlet(), "/*");
            contextHandlerCollection.addHandler(uselessContextHandler);
            return contextHandlerCollection;
        });
    }

    @Test
    public void testNoGzipSync(@ClusteredTest ClusteredTestContext clusteredTestContext) throws Exception
    {
        Jetty12ClusteredPerfTest.runTest(clusteredTestContext, () ->
        {
            ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
            ServletContextHandler targetContextHandler = new ServletContextHandler();
            targetContextHandler.setContextPath("/");
            targetContextHandler.addServlet(new SyncEE10Servlet("Hi there!".getBytes(StandardCharsets.ISO_8859_1)), "/*");
            contextHandlerCollection.addHandler(targetContextHandler);
            ServletContextHandler uselessContextHandler = new ServletContextHandler();
            uselessContextHandler.setContextPath("/useless");
            uselessContextHandler.addServlet(new Always404Servlet(), "/*");
            contextHandlerCollection.addHandler(uselessContextHandler);
            return contextHandlerCollection;
        });
    }

    @Test
    public void testGzipSync(@ClusteredTest ClusteredTestContext clusteredTestContext) throws Exception
    {
        Jetty12ClusteredPerfTest.runTest(clusteredTestContext, () ->
        {
            ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
            ServletContextHandler targetContextHandler = new ServletContextHandler();
            targetContextHandler.setContextPath("/");
            targetContextHandler.addServlet(new SyncEE10Servlet("Hi there!".getBytes(StandardCharsets.ISO_8859_1)), "/*");
            contextHandlerCollection.addHandler(targetContextHandler);
            ServletContextHandler uselessContextHandler = new ServletContextHandler();
            uselessContextHandler.setContextPath("/useless");
            uselessContextHandler.addServlet(new Always404Servlet(), "/*");
            contextHandlerCollection.addHandler(uselessContextHandler);
            GzipHandler gzipHandler = new GzipHandler(contextHandlerCollection);
            gzipHandler.setMinGzipSize(1);
            return gzipHandler;
        });
    }
}
