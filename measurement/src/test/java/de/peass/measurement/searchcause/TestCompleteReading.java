package de.peass.measurement.searchcause;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.searchcause.data.CallTreeNode;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.common.configuration.Configuration;
import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.monitoring.writer.filesystem.AggregatedTreeWriter;

public class TestCompleteReading {
   
   private void writeFakeMeasurements(final File kiekerTraceFolder) {
      final Configuration configuration = new Configuration();
      configuration.setProperty(AggregatedTreeWriter.CONFIG_PATH, kiekerTraceFolder.getAbsolutePath());
      configuration.setProperty(AggregatedTreeWriter.CONFIG_WARMUP, 0);
      final AggregatedTreeWriter writer = new AggregatedTreeWriter(configuration);
      writer.onStarting();
      writer.writeMonitoringRecord(new OperationExecutionRecord("public void A.parent()", "1", 1, 1, 2, "xyz", 0, 0));
      for (int i = 0; i < 100; i++) {
         writer.writeMonitoringRecord(new OperationExecutionRecord("public void A.child1()", "1", 1, 1, 2, "xyz", 0, 1));
         writer.writeMonitoringRecord(new OperationExecutionRecord("public void A.child2()", "1", 1, 1, 2, "xyz", 1, 1));
         writer.writeMonitoringRecord(new OperationExecutionRecord("public void A.child1()", "1", 1, 1, 2, "xyz", 2, 1));
      }
      writer.onTerminating();
   }
   
   private Set<CallTreeNode> buildTree() {
      final Set<CallTreeNode> includedNodes = new HashSet<>();
      final CallTreeNode root = new CallTreeNode("parent()", "public void A.parent()", null);
      includedNodes.add(root.appendChild("A.child1()", "public void A.child1()"));
      includedNodes.add(root.appendChild("A.child2()", "public void A.child2()"));
      includedNodes.add(root.appendChild("A.child1()", "public void A.child1()"));
      includedNodes.add(root);
      
      for (final CallTreeNode node : includedNodes) {
         node.setVersions("0", "1");
         node.setWarmup(0);
      }
      return includedNodes;
   }
   
   @Test
   public void testReading() throws AnalysisConfigurationException, JsonParseException, JsonMappingException, IOException {
      final File kiekerTraceFolder = new File("target/kiekerreading");
      if (kiekerTraceFolder.exists()) {
         FileUtils.deleteDirectory(kiekerTraceFolder);
      }
      kiekerTraceFolder.mkdirs();
      
      writeFakeMeasurements(kiekerTraceFolder);
      
      final Set<CallTreeNode> includedNodes = buildTree();
      
      final TestCase testcase = new TestCase("A", "parent");
      final KiekerResultReader reader = new KiekerResultReader(false, includedNodes, "0", kiekerTraceFolder, testcase, false);
      reader.setConsiderNodePosition(true);
      final File kiekerFolder = kiekerTraceFolder.listFiles()[0];
      reader.readAggregatedData(kiekerFolder);
      
      for (final CallTreeNode node : includedNodes) {
         node.createStatistics("0");
         Assert.assertEquals(node.getCall() + " not found", 1, node.getStatistics("0").getN());
      }
   }

   

   
}
