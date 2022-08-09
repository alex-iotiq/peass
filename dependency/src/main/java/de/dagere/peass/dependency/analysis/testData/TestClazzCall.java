package de.dagere.peass.dependency.analysis.testData;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestClazzCall extends TestCase {
   private static final long serialVersionUID = 7326687309901903065L;

   public TestClazzCall(String clazz) {
      super(clazz, null, "", null);
   }

   public TestClazzCall(String clazz, String module) {
      super(clazz, null, module, null);
   }
   
   public TestClazzCall(final ChangedEntity entity) {
      super(entity.getClazz(), entity.getMethod(), entity.getModule(), null);
   }

   public TestClazzCall copy() {
      TestClazzCall test = new TestClazzCall(clazz, module);
      return test;
   }

   public static TestClazzCall createFromString(String testcase) {
      String module, clazz;
      int moduleIndex = testcase.indexOf(ChangedEntity.MODULE_SEPARATOR);
      if (moduleIndex == -1) {
         clazz = testcase;
         module = "";
      } else {
         clazz = testcase.substring(moduleIndex + 1);
         module = testcase.substring(0, moduleIndex);
      }
      return new TestClazzCall(clazz, module);
   }
}