package de.dagere.peass.dependency.analysis;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.execution.utils.TestExecutor;

public class ModuleClassMapping {

   public static final ModuleClassMapping SINGLE_MODULE_MAPPING = new ModuleClassMapping();

   private ModuleClassMapping() {
      modules = null;
   }

   private static final Logger LOG = LogManager.getLogger(ModuleClassMapping.class);

   private final Map<String, String> mapping = new HashMap<>();
   private final List<File> modules;

   public ModuleClassMapping(final File baseFolder, final ProjectModules modules, final ExecutionConfig config) {
      for (final File module : modules.getModules()) {
         ClazzFileFinder finder = new ClazzFileFinder(config);
         populateModule(baseFolder, module, finder);
      }
      this.modules = modules.getModules();
   }
   
   private void populateModule(final File baseFolder, final File module, final ClazzFileFinder finder) {
      final List<String> classes = finder.getClasses(module);
      String moduleName;
      if (module.equals(baseFolder)) {
         moduleName = "";
      } else {
         moduleName = getModuleName(baseFolder, module);
      }
      LOG.debug("Module: {} Name: {}", module.getAbsolutePath(), moduleName);
      for (final String clazz : classes) {
         mapping.put(clazz, moduleName);
      }
   }

   public ModuleClassMapping(final TestExecutor executor) {
      this(executor.getProjectFolder(), executor.getModules(), executor.getTestTransformer().getConfig().getExecutionConfig());
   }

   public String getModuleOfClass(final String clazz) {
      if (this == SINGLE_MODULE_MAPPING) {
         return "";
      }
      return mapping.get(clazz);
   }

   public static String getModuleName(final File baseFolder, final File module) {
      String moduleName;
      final int pathIndex = baseFolder.getAbsolutePath().length() + 1;
      final String modulePath = module.getAbsolutePath();
      if (modulePath.length() > pathIndex) {
         moduleName = modulePath.substring(pathIndex);
      } else {
         moduleName = "";
      }
      return moduleName;
   }

   public List<String> getAllClasses() {
      List<String> clazzes = new LinkedList<>();
      for (String clazz : mapping.values()) {
         clazzes.add(clazz);
      }
      return clazzes;
   }

   public List<File> getModules() {
      return modules;
   }
}
