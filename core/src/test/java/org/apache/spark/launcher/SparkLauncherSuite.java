/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.launcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import static org.junit.Assert.*;

import org.apache.spark.internal.config.package$;

/**
 * These tests require the Spark assembly to be built before they can be run.
 */
public class SparkLauncherSuite {

  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private static final Logger LOG = LoggerFactory.getLogger(SparkLauncherSuite.class);
  private static final NamedThreadFactory TF = new NamedThreadFactory("SparkLauncherSuite-%d");

  private SparkLauncher launcher;

  @Before
  public void configureLauncher() {
    launcher = new SparkLauncher().setSparkHome(System.getProperty("spark.test.home"));
  }

  @Test
  public void testSparkArgumentHandling() throws Exception {
    SparkSubmitOptionParser opts = new SparkSubmitOptionParser();

    launcher.addSparkArg(opts.HELP);
    try {
      launcher.addSparkArg(opts.PROXY_USER);
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    launcher.addSparkArg(opts.PROXY_USER, "someUser");
    try {
      launcher.addSparkArg(opts.HELP, "someValue");
      fail("Expected IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    launcher.addSparkArg("--future-argument");
    launcher.addSparkArg("--future-argument", "someValue");

    launcher.addSparkArg(opts.MASTER, "myMaster");
    assertEquals("myMaster", launcher.builder.master);

    launcher.addJar("foo");
    launcher.addSparkArg(opts.JARS, "bar");
    assertEquals(Arrays.asList("bar"), launcher.builder.jars);

    launcher.addFile("foo");
    launcher.addSparkArg(opts.FILES, "bar");
    assertEquals(Arrays.asList("bar"), launcher.builder.files);

    launcher.addPyFile("foo");
    launcher.addSparkArg(opts.PY_FILES, "bar");
    assertEquals(Arrays.asList("bar"), launcher.builder.pyFiles);

    launcher.setConf("spark.foo", "foo");
    launcher.addSparkArg(opts.CONF, "spark.foo=bar");
    assertEquals("bar", launcher.builder.conf.get("spark.foo"));

    launcher.setConf(SparkLauncher.PYSPARK_DRIVER_PYTHON, "python3.4");
    launcher.setConf(SparkLauncher.PYSPARK_PYTHON, "python3.5");
    assertEquals("python3.4", launcher.builder.conf.get(
      package$.MODULE$.PYSPARK_DRIVER_PYTHON().key()));
    assertEquals("python3.5", launcher.builder.conf.get(package$.MODULE$.PYSPARK_PYTHON().key()));
  }

  @Test(expected=IllegalStateException.class)
  public void testRedirectTwiceFails() throws Exception {
    launcher.setAppResource("fake-resource.jar")
      .setMainClass("my.fake.class.Fake")
      .redirectError()
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .launch();
  }

  @Test(expected=IllegalStateException.class)
  public void testRedirectToLogWithOthersFails() throws Exception {
    launcher.setAppResource("fake-resource.jar")
      .setMainClass("my.fake.class.Fake")
      .redirectToLog("fakeLog")
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .launch();
  }

  @Test
  public void testRedirectErrorToOutput() throws Exception {
    launcher.redirectError();
    assertTrue(launcher.redirectErrorStream);
  }

  @Test
  public void testRedirectsSimple() throws Exception {
    launcher.redirectError(ProcessBuilder.Redirect.PIPE);
    assertNotNull(launcher.errorStream);
    assertEquals(launcher.errorStream.type(), ProcessBuilder.Redirect.Type.PIPE);

    launcher.redirectOutput(ProcessBuilder.Redirect.PIPE);
    assertNotNull(launcher.outputStream);
    assertEquals(launcher.outputStream.type(), ProcessBuilder.Redirect.Type.PIPE);
  }

  @Test
  public void testRedirectLastWins() throws Exception {
    launcher.redirectError(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.INHERIT);
    assertEquals(launcher.errorStream.type(), ProcessBuilder.Redirect.Type.INHERIT);

    launcher.redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectOutput(ProcessBuilder.Redirect.INHERIT);
    assertEquals(launcher.outputStream.type(), ProcessBuilder.Redirect.Type.INHERIT);
  }

  @Test
  public void testRedirectToLog() throws Exception {
    launcher.redirectToLog("fakeLogger");
    assertTrue(launcher.redirectToLog);
    assertTrue(launcher.builder.getEffectiveConfig()
      .containsKey(SparkLauncher.CHILD_PROCESS_LOGGER_NAME));
  }

  @Test
  public void testChildProcLauncher() throws Exception {
    SparkSubmitOptionParser opts = new SparkSubmitOptionParser();
    Map<String, String> env = new HashMap<>();
    env.put("SPARK_PRINT_LAUNCH_COMMAND", "1");

    launcher
      .setMaster("local")
      .setAppResource(SparkLauncher.NO_RESOURCE)
      .addSparkArg(opts.CONF,
        String.format("%s=-Dfoo=ShouldBeOverriddenBelow", SparkLauncher.DRIVER_EXTRA_JAVA_OPTIONS))
      .setConf(SparkLauncher.DRIVER_EXTRA_JAVA_OPTIONS,
        "-Dfoo=bar -Dtest.appender=childproc")
      .setConf(SparkLauncher.DRIVER_EXTRA_CLASSPATH, System.getProperty("java.class.path"))
      .addSparkArg(opts.CLASS, "ShouldBeOverriddenBelow")
      .setMainClass(SparkLauncherTestApp.class.getName())
      .addAppArgs("proc");
    final Process app = launcher.launch();

    new OutputRedirector(app.getInputStream(), TF);
    new OutputRedirector(app.getErrorStream(), TF);
    assertEquals(0, app.waitFor());
  }

  public static class SparkLauncherTestApp {

    public static void main(String[] args) throws Exception {
      assertEquals(1, args.length);
      assertEquals("proc", args[0]);
      assertEquals("bar", System.getProperty("foo"));
      assertEquals("local", System.getProperty(SparkLauncher.SPARK_MASTER));
    }

  }

}
