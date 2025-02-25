package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CauseSearcherConfig implements Serializable {

   private static final long serialVersionUID = 5893295648840988829L;

   private final TestMethodCall testCase;
   private final boolean ignoreEOIs;
   private final boolean splitAggregated;
   private final double minTime;
   private final boolean calibrationRun;

   @JsonInclude(Include.NON_NULL)
   private File propertyFolder;
   private final RCAStrategy rcaStrategy;
   private final int levels;

   @JsonCreator
   public CauseSearcherConfig(@JsonProperty("testcase") final TestMethodCall testCase,
         @JsonProperty("splitAggregated") final boolean splitAggregated,
         @JsonProperty("minTime") final double minTime,
         @JsonProperty("calibrationRun") final boolean calibrationRun,
         @JsonProperty("ignoreEOIs") final boolean ignoreEOIs,
         @JsonProperty("rcaStrategy") final RCAStrategy rcaStrategy,
         @JsonProperty("levels") final int levels) {
      this.testCase = testCase;
      this.splitAggregated = splitAggregated;
      this.minTime = minTime;
      this.calibrationRun = calibrationRun;
      this.ignoreEOIs = ignoreEOIs;
      this.rcaStrategy = rcaStrategy;
      this.levels = levels;
      propertyFolder = null;
      if (rcaStrategy != RCAStrategy.LEVELWISE && levels > 1) {
         throw new RuntimeException("If levels > 1, strategy must be LEVELWISE");
      }
      if (rcaStrategy == RCAStrategy.LEVELWISE && levels < 1) {
         // TODO It would be possible to refactor this to two strategies, UNTIL_SOURCE_CHANGE and LEVELWISE,
         // where -1 means complete tree and 1 and above mean constant count of levels; than this check needs to be changed
         throw new RuntimeException("Levels need to be 1 or above!");
      }
   }

   public CauseSearcherConfig(final TestMethodCall test, final CauseSearcherConfigMixin config) {
      this(test, !config.isNotSplitAggregated(), config.getMinTime(), config.isUseCalibrationRun(), !config.isUseEOIs(),
            config.getStrategy(), config.getLevels());
      this.propertyFolder = config.getPropertyFolder();
   }

   public CauseSearcherConfig(final TestMethodCall testCase, final CauseSearcherConfig causeConfig) {
      this(testCase,
            causeConfig.isSplitAggregated(), causeConfig.getMinTime(), causeConfig.useCalibrationRun(), causeConfig.isIgnoreEOIs(),
            causeConfig.getRcaStrategy(), causeConfig.getLevels());
      this.propertyFolder = causeConfig.getPropertyFolder();
   }

   public TestMethodCall getTestCase() {
      return testCase;
   }

   public boolean isSplitAggregated() {
      return splitAggregated;
   }

   public double getMinTime() {
      return minTime;
   }

   public boolean useCalibrationRun() {
      return calibrationRun;
   }

   public boolean isIgnoreEOIs() {
      return ignoreEOIs;
   }

   public RCAStrategy getRcaStrategy() {
      return rcaStrategy;
   }
   
   public void setPropertyFolder(final File propertyFolder) {
      this.propertyFolder = propertyFolder;
   }

   public File getPropertyFolder() {
      return propertyFolder;
   }

   public int getLevels() {
      return levels;
   }
}