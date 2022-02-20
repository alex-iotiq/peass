package de.dagere.peass.dependency.persistence;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestExecutionData {
   @Test
   public void testConversion() {
      Dependencies dependencies = buildExampleDependencies();

      Assert.assertArrayEquals(new String[] { "000001", "000002", "000004", "000005" }, dependencies.getVersionNames());

      ExecutionData executions = new ExecutionData(dependencies);
      Assert.assertArrayEquals(new String[] { "000001", "000002", "000004", "000005" }, executions.getVersionNames());
   }

   public static Dependencies buildExampleDependencies() {
      Dependencies dependencies = new Dependencies();

      dependencies.setUrl("https://test");
      dependencies.getInitialversion().setVersion("000001");
      dependencies.getVersions().put("000002", new Version());
      dependencies.getVersions().put("000004", new Version());
      dependencies.getVersions().put("000005", new Version());
      return dependencies;
   }
}