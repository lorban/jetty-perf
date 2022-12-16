package org.eclipse.jetty.perf.ee10;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.perf.test.FlatPerfTest;
import org.eclipse.jetty.perf.test.PerfTestParams;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DelayedHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Disabled
public class AsyncEE10ServletPerfTest
{
    private static final Duration WARMUP_DURATION = Duration.ofSeconds(60);
    private static final Duration RUN_DURATION = Duration.ofSeconds(180);

    private static Stream<PerfTestParams> params()
    {
        return Stream.of(
            new PerfTestParams(PerfTestParams.Protocol.http, 60_000, 100, 5_000, 110_000, 10.0),
            new PerfTestParams(PerfTestParams.Protocol.https, 60_000, 100, 6_500, 130_000, 10.0),
            new PerfTestParams(PerfTestParams.Protocol.h2c, 60_000, 100, 13_000, 120_000, 15.0),
            new PerfTestParams(PerfTestParams.Protocol.h2, 60_000, 100, 30_000, 130_000, 15.0)
        );
    }

    private String testName;

    @BeforeEach
    protected void beforeEach(TestInfo testInfo)
    {
        // Generate test name
        String className = testInfo.getTestClass().orElseThrow().getName();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        String methodName = testInfo.getTestMethod().orElseThrow().getName();
        testName = simpleClassName + "_" + methodName;
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testComplete(PerfTestParams params) throws Exception
    {
        boolean succeeded = FlatPerfTest.runTest(testName, params, WARMUP_DURATION, RUN_DURATION, () ->
        {
            DelayedHandler.UntilContent untilContentHandler = new DelayedHandler.UntilContent();
            GzipHandler gzipHandler = new GzipHandler();
            untilContentHandler.setHandler(gzipHandler);
            ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
            gzipHandler.setHandler(contextHandlerCollection);
            ServletContextHandler targetContextHandler = new ServletContextHandler();
            targetContextHandler.setContextPath("/");
            targetContextHandler.addServlet(new AsyncEE10Servlet("Hi there!".getBytes(StandardCharsets.ISO_8859_1)), "/*");
            contextHandlerCollection.addHandler(targetContextHandler);
            ServletContextHandler uselessContextHandler = new ServletContextHandler();
            uselessContextHandler.setContextPath("/useless");
            uselessContextHandler.addServlet(new Always404Servlet(), "/*");
            contextHandlerCollection.addHandler(uselessContextHandler);
            return untilContentHandler;
        });
        assertThat("Performance assertions failure for " + params, succeeded, is(true));
    }
}
