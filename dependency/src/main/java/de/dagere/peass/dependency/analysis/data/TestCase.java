package de.dagere.peass.dependency.analysis.data;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.kopeme.generated.Kopemedata.Testcases;

/**
 * Represents a testcase with its class and its method. If no method is given, the whole class with all methods is represented.
 * 
 * @author reichelt
 *
 */
public class TestCase implements Comparable<TestCase> {
   private final String module;
   private final String clazz;
   private final String method;
   private final String params;

   public TestCase(final Testcases data, final String params) {
      clazz = data.getClazz();
      method = data.getTestcase().get(0).getName();
      this.params = params;
      module = "";
   }

   public TestCase(final String clazz, final String method) {
      if (clazz.contains(File.separator)) { // possibly assertion, if speed becomes issue..
         throw new RuntimeException("Testcase should be full qualified name, not path: " + clazz);
      }
      if (clazz.contains(ChangedEntity.METHOD_SEPARATOR)) {
         throw new RuntimeException("Class and method should be separated: " + clazz);
      }
      if (clazz.contains(ChangedEntity.MODULE_SEPARATOR)) {
         module = clazz.substring(0, clazz.indexOf(ChangedEntity.MODULE_SEPARATOR));
         this.clazz = clazz.substring(clazz.indexOf(ChangedEntity.MODULE_SEPARATOR) + 1, clazz.length());
      } else {
         this.clazz = clazz;
         module = "";
      }
      this.method = method;
      this.params = null;
   }

   public TestCase(final ChangedEntity entity) {
      this(entity.getClazz(), entity.getMethod(), entity.getModule(), null);
   }

   public TestCase(final String clazz, final String method, final String module) {
      this(clazz, method, module, null);
   }

   @JsonCreator
   public TestCase(@JsonProperty("clazz") final String clazz,
         @JsonProperty("method") final String method,
         @JsonProperty("module") final String module,
         @JsonProperty("params") final String params) {
      if (clazz.contains(File.separator)) {
         throw new RuntimeException("Testcase " + clazz + " should be full qualified name, not path!");
      }
      if (clazz.contains(ChangedEntity.METHOD_SEPARATOR)) {
         throw new RuntimeException("Class and method should be separated: " + clazz);
      }
      this.clazz = clazz;
      this.method = method;
      this.module = module;
      this.params = params;
   }

   public TestCase(final String testcase) {
      if (testcase.contains(File.separator)) {
         throw new RuntimeException("Testcase should be full qualified name, not path!");
      }
      final int index = testcase.lastIndexOf(ChangedEntity.METHOD_SEPARATOR);
      if (index == -1) {
         int moduleIndex = testcase.indexOf(ChangedEntity.MODULE_SEPARATOR);
         if (moduleIndex == -1) {
            clazz = testcase;
            module = "";
         } else {
            clazz = testcase.substring(moduleIndex + 1);
            module = testcase.substring(0, moduleIndex);
         }
         method = null;
         // final int indexDot = testcase.lastIndexOf(".");
         // clazz = testcase.substring(0, indexDot);
         // method = testcase.substring(indexDot + 1);
         params = null;
      } else {
         String start = testcase.substring(0, index);
         int moduleIndex = testcase.indexOf(ChangedEntity.MODULE_SEPARATOR);
         if (moduleIndex == -1) {
            clazz = start;
            module = "";
         } else {
            clazz = start.substring(moduleIndex + 1);
            module = start.substring(0, moduleIndex);
         }

         if (testcase.contains("(")) {
            method = testcase.substring(index + 1, testcase.indexOf("("));
            params = testcase.substring(testcase.indexOf("(") + 1, testcase.length() - 1);
         } else {
            method = testcase.substring(index + 1);
            params = null;
         }
      }
   }

   public String getClazz() {
      return clazz;
   }

   public String getMethod() {
      return method;
   }

   public String getModule() {
      return module;
   }

   public String getParams() {
      return params;
   }

   @JsonIgnore
   public String getTestclazzWithModuleName() {
      String testcase;
      if (module != null && !module.equals("")) {
         testcase = module + ChangedEntity.MODULE_SEPARATOR + clazz;
      } else {
         testcase = clazz;
      }
      return testcase;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
      result = prime * result + ((method == null) ? 0 : method.hashCode());
      return result;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      final TestCase other = (TestCase) obj;
      if (clazz == null) {
         if (other.clazz != null) {
            return false;
         }
      } else if (!clazz.equals(other.clazz)) {
         final String shortClazz = clazz.substring(clazz.lastIndexOf('.') + 1);
         final String shortClazzOther = other.getClazz().substring(other.getClazz().lastIndexOf('.') + 1);
         if (!shortClazz.equals(shortClazzOther)) { // Dirty Hack - better transfer clazz-info always
            return false;
         }
      }
      if (method == null) {
         if (other.method != null) {
            return false;
         }
      } else if (!method.equals(other.method)) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      String result;
      if (module != null && !"".equals(module)) {
         result = module + ChangedEntity.MODULE_SEPARATOR + clazz + ChangedEntity.METHOD_SEPARATOR + method;
      } else {
         result = clazz + ChangedEntity.METHOD_SEPARATOR + method;
      }
      if (params != null) {
         result += "(" + params + ")";
      }
      return result;
   }

   @JsonIgnore
   public String getExecutable() {
      if (method != null) {
         return clazz + "#" + method;
      } else {
         return clazz;
      }

   }

   @JsonIgnore
   public String getShortClazz() {
      return clazz.substring(clazz.lastIndexOf('.') + 1, clazz.length());
   }

   @Override
   public int compareTo(final TestCase arg0) {
      return toString().compareTo(arg0.toString());
   }

   public ChangedEntity toEntity() {
      return new ChangedEntity(clazz, module, method);
   }

}