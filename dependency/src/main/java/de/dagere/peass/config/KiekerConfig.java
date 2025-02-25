package de.dagere.peass.config;

import java.io.Serializable;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonInclude;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class KiekerConfig implements Serializable {
   
   public static final boolean USE_CIRCULAR_QUEUE_DEFAULT = false;
   
   public static final int DEFAULT_WRITE_INTERVAL = 5000;
   public static final int DEFAULT_KIEKER_QUEUE_SIZE = 10000000;
   public static final int DEFAULT_TRACE_SIZE_IN_MB = 100;
   public final static int DEFAULT_KIEKER_WAIT_TIME = 5;
   
   private static final long serialVersionUID = 3129231099963995908L;

   private boolean useKieker = false;
   private boolean useSourceInstrumentation = true;
   private boolean useSelectiveInstrumentation = true;
   private boolean useAggregation = true;
   private boolean measureAdded = false;
   private boolean useCircularQueue = USE_CIRCULAR_QUEUE_DEFAULT;
   private boolean enableAdaptiveMonitoring = false;
   private boolean adaptiveInstrumentation = false;
   private int kiekerAggregationInterval = DEFAULT_WRITE_INTERVAL;
   private AllowedKiekerRecord record = AllowedKiekerRecord.DURATION;
   private boolean onlyOneCallRecording = false;
   private boolean extractMethod = false;
   private long traceSizeInMb = DEFAULT_TRACE_SIZE_IN_MB;
   private long kiekerQueueSize = DEFAULT_KIEKER_QUEUE_SIZE;
   private boolean createDefaultConstructor = true;
   private int kiekerWaitTime = DEFAULT_KIEKER_WAIT_TIME;
   
   // We want a set that preserves insertion order, so we require a LinkedHashSet
   private LinkedHashSet<String> excludeForTracing = new LinkedHashSet<>();

   public KiekerConfig() {
   }

   public KiekerConfig(final boolean useKieker) {
      this.useKieker = useKieker;
   }

   public KiekerConfig(final KiekerConfig other) {
      this.useKieker = other.useKieker;
      this.useSourceInstrumentation = other.useSourceInstrumentation;
      this.useSelectiveInstrumentation = other.useSelectiveInstrumentation;
      this.useAggregation = other.useAggregation;
      this.measureAdded = other.measureAdded;
      this.useCircularQueue = other.useCircularQueue;
      this.enableAdaptiveMonitoring = other.enableAdaptiveMonitoring;
      this.adaptiveInstrumentation = other.adaptiveInstrumentation;
      this.kiekerAggregationInterval = other.kiekerAggregationInterval;
      this.record = other.record;
      this.onlyOneCallRecording = other.onlyOneCallRecording;
      this.extractMethod = other.extractMethod;
      this.traceSizeInMb = other.traceSizeInMb;
      this.kiekerQueueSize = other.kiekerQueueSize;
      this.excludeForTracing = other.excludeForTracing;
      this.createDefaultConstructor = other.createDefaultConstructor;
      this.kiekerWaitTime = other.kiekerWaitTime;
   }
   
   public void check() {
      if (kiekerAggregationInterval != DEFAULT_WRITE_INTERVAL && !useAggregation) {
         throw new RuntimeException("The write interval only works with aggregation, non-aggregated writing will write every record directly to hard disc");
      }
      if (!useSourceInstrumentation && extractMethod) {
         throw new RuntimeException("Deactivated source instrumentation and usage of extraction is not possible!");
      }
      if (traceSizeInMb < 1) {
         throw new RuntimeException("Trace size in MB need to be at least 1, but was " + traceSizeInMb);
      }
   }

   public boolean isUseKieker() {
      return useKieker;
   }

   public void setUseKieker(final boolean useKieker) {
      this.useKieker = useKieker;
   }
   

   public int getKiekerWaitTime() {
      return kiekerWaitTime;
   }

   public void setKiekerWaitTime(final int kiekerWaitTime) {
      this.kiekerWaitTime = kiekerWaitTime;
   }

   public boolean isUseSourceInstrumentation() {
      return useSourceInstrumentation;
   }

   public void setUseSourceInstrumentation(final boolean useSourceInstrumentation) {
      this.useSourceInstrumentation = useSourceInstrumentation;
   }

   public boolean isUseSelectiveInstrumentation() {
      return useSelectiveInstrumentation;
   }

   public void setUseSelectiveInstrumentation(final boolean useSelectiveInstrumentation) {
      this.useSelectiveInstrumentation = useSelectiveInstrumentation;
   }

   public boolean isUseAggregation() {
      return useAggregation;
   }

   public void setUseAggregation(final boolean useAggregation) {
      this.useAggregation = useAggregation;
   }
   
   @JsonInclude(JsonInclude.Include.NON_DEFAULT)
   public boolean isMeasureAdded() {
      return measureAdded;
   }
   
   public void setMeasureAdded(boolean measureAdded) {
      this.measureAdded = measureAdded;
   }

   public boolean isUseCircularQueue() {
      return useCircularQueue;
   }

   public void setUseCircularQueue(final boolean useCircularQueue) {
      this.useCircularQueue = useCircularQueue;
   }

   public boolean isEnableAdaptiveMonitoring() {
      return enableAdaptiveMonitoring;
   }

   public void setEnableAdaptiveMonitoring(final boolean enableAdaptiveMonitoring) {
      this.enableAdaptiveMonitoring = enableAdaptiveMonitoring;
   }
   
   public boolean isAdaptiveInstrumentation() {
      return adaptiveInstrumentation;
   }

   public void setAdaptiveInstrumentation(final boolean adaptiveInstrumentation) {
      this.adaptiveInstrumentation = adaptiveInstrumentation;
   }

   public int getKiekerAggregationInterval() {
      return kiekerAggregationInterval;
   }

   public void setKiekerAggregationInterval(final int kiekerAggregationInterval) {
      this.kiekerAggregationInterval = kiekerAggregationInterval;
   }

   public AllowedKiekerRecord getRecord() {
      return record;
   }
   
   public void setRecord(final AllowedKiekerRecord record) {
      if (record == null) {
         this.record = AllowedKiekerRecord.OPERATIONEXECUTION;
      } else {
         this.record = record;
      }
   }
   
   public boolean isOnlyOneCallRecording() {
      return onlyOneCallRecording;
   }
   
   public void setOnlyOneCallRecording(final boolean onlyOneCallRecording) {
      this.onlyOneCallRecording = onlyOneCallRecording;
   }

   public boolean isExtractMethod() {
      return extractMethod;
   }

   public void setExtractMethod(final boolean extractMethod) {
      this.extractMethod = extractMethod;
   }
   
   public long getTraceSizeInMb() {
      return traceSizeInMb;
   }
   
   public void setTraceSizeInMb(final long traceSizeInMb) {
      this.traceSizeInMb = traceSizeInMb;
   }
   
   public long getKiekerQueueSize() {
      return kiekerQueueSize;
   }
   
   public void setKiekerQueueSize(final long kiekerQueueSize) {
      this.kiekerQueueSize = kiekerQueueSize;
   }

   public LinkedHashSet<String> getExcludeForTracing() {
      return excludeForTracing;
   }

   public void setExcludeForTracing(final LinkedHashSet<String> excludeForTracing) {
      this.excludeForTracing = excludeForTracing;
   }
   
   public boolean isCreateDefaultConstructor() {
      return createDefaultConstructor;
   }

   public void setCreateDefaultConstructor(final boolean createDefaultConstructor) {
      this.createDefaultConstructor = createDefaultConstructor;
   }
}
