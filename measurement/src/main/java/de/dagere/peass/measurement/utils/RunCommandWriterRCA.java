package de.dagere.peass.measurement.utils;

import java.io.PrintStream;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.rca.RCAStrategy;

public class RunCommandWriterRCA extends RunCommandWriter {

   public RunCommandWriterRCA(MeasurementConfig config, final PrintStream goal, final String experimentId, final SelectedTests dependencies) {
      super(config, goal, experimentId, dependencies);
   }

   @Override
   public void createSingleMethodCommand(final int versionIndex, final String commit, final String testcaseName) {
      long timeoutInMinutes = config.getTimeoutInSeconds() / 60;
      goal.println("./peass searchcause "
            + "-rcaStrategy " + RCAStrategy.UNTIL_SOURCE_CHANGE + " "
            + "-propertyFolder results/properties_" + name + " "
            + "-test " + testcaseName + " "
            + "-warmup " + config.getWarmup() + " "
            + "-iterations " + config.getIterations() + " "
            + "-repetitions " + config.getRepetitions() + " "
            + "-vms " + config.getVms() + " "
            + "-timeout " + timeoutInMinutes + " "
            + "-type1error 0.2 "
            + "-type2error 0.1 "
            + "-measurementStrategy " + config.getMeasurementStrategy() + " "
            + (config.getExecutionConfig().isExcludeLog4jToSlf4j() ? "-excludeLog4jToSlf4j " : "")
            + (config.getExecutionConfig().isExcludeLog4jSlf4jImpl() ? "-excludeLog4jSlf4jImpl " : "")
            + "-commit " + commit + " "
            + "-folder " + DEFAULT_PROJECT_FOLDER_LOCATION + name + "/ "
            + "-executionFile results/" + ResultsFolders.TRACE_SELECTION_PREFIX + name + ".json "
            + " &> rca_" + commit.substring(0, 6) + "_" + testcaseName + ".txt");
   }

}
