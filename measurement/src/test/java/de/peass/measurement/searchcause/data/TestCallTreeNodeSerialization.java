package de.peass.measurement.searchcause.data;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import de.peass.utils.Constants;

public class TestCallTreeNodeSerialization {
   @Test
   public void testName() throws Exception {
      final CallTreeNode parent = buildExampleTree();
      
      final File resultFile = new File("test.json");
      Constants.OBJECTMAPPER.writeValue(resultFile, parent);
      
      final CallTreeNode deserialized = Constants.OBJECTMAPPER.readValue(resultFile, CallTreeNode.class);
      Assert.assertEquals("public void test()", deserialized.getKiekerPattern());
      final CallTreeNode firstChild = deserialized.getChildren().get(0);
      Assert.assertEquals("child1()", firstChild.getCall());
      
      final CallTreeNode thirdChild = firstChild.getChildren().get(0);
      Assert.assertEquals("public void child3(int, String)", thirdChild.getKiekerPattern());
      
      final CallTreeNode secondChild = deserialized.getChildren().get(1);
      Assert.assertEquals("child2()", secondChild.getCall());
   }

   private CallTreeNode buildExampleTree() {
      final CallTreeNode parent = new CallTreeNode("test()", "public void test()", null);
      final CallTreeNode child2 = parent.appendChild("child1()", "public void child1()");
      child2.appendChild("child3(int, String)", "public void child3(int, String)");
      parent.appendChild("child2()", "public void child2()");
      return parent;
   }
}
