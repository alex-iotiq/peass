package de.dagere.peass.config.parameters;

import de.dagere.peass.config.MeasurementStrategy;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import picocli.CommandLine.Option;

public class MeasurementConfigurationMixin {
   public static final int DEFAULT_VMS = 30;
   public static final int DEFAULT_ITERATIONS = 5;
   public static final int DEFAULT_WARMUP = 5;
   public static final int DEFAULT_REPETITIONS = 1000000;
   public static final long DEFAULT_TIMEOUT = 5L;

   @Option(names = { "-vms", "--vms" }, description = "Number of VMs to start")
   int vms = DEFAULT_VMS;

   @Option(names = { "-iterations", "--iterations" }, description = "Number of iterations")
   int iterations = DEFAULT_ITERATIONS;

   @Option(names = { "-warmup", "--warmup" }, description = "Number of warmup iterations")
   int warmup = DEFAULT_WARMUP;

   @Option(names = { "-repetitions", "--repetitions" }, description = "Number of repetitions (executions of the workload inside of one iteration)")
   int repetitions = DEFAULT_REPETITIONS;

   @Option(names = { "-processTimeout",
         "--processTimeout" }, description = "Timeout that the overall measurement process has - if one VM measurement takes so long that this timeout will be hit, the overall process is stopped (only recommended for calibration runs)")
   protected long processTimeout = DEFAULT_TIMEOUT;

   @Option(names = { "-duration", "--duration" }, description = "Which duration to use - if duration is specified, warmup and iterations are ignored")
   int duration = 0;

   @Option(names = { "-useKieker", "--useKieker", "-usekieker", "--usekieker" }, description = "Whether Kieker should be used")
   boolean useKieker = false;

   @Option(names = { "-useGC", "--useGC" }, description = "Do execute GC before each iteration (default false)")
   public boolean useGC = false;

   @Option(names = { "-earlyStop", "--earlyStop" }, description = "Whether to stop early (i.e. execute VMs until type 1 and type 2 error are met)")
   protected boolean earlyStop = false;

   @Option(names = { "-saveKieker", "--saveKieker" }, description = "Save no kieker results in order to use less space - default false")
   private boolean saveNothing = false;

   @Option(names = { "-record", "--record" }, description = "Kieker Record type to use for monitoring ")
   protected AllowedKiekerRecord record = AllowedKiekerRecord.DURATION;

   @Option(names = { "-measurementStrategy", "--measurementStrategy" }, description = "Measurement strategy (Default: PARALLEL) ")
   protected MeasurementStrategy measurementStrategy = MeasurementStrategy.PARALLEL;

   @Option(names = { "-directlyMeasureKieker",
         "--directlyMeasureKieker" }, description = "Activates measurement via Kieker instead KoPeMe (only useful it repetitions = 1 and a test runner contains huge warmup)")
   protected boolean directlyMeasureKieker = false;

   public int getVms() {
      return vms;
   }

   public int getDuration() {
      return duration;
   }

   public int getWarmup() {
      return warmup;
   }

   public boolean isSaveNothing() {
      return saveNothing;
   }

   public int getIterations() {
      return iterations;
   }

   public int getRepetitions() {
      return repetitions;
   }

   public boolean isUseKieker() {
      return useKieker;
   }

   public boolean isUseGC() {
      return useGC;
   }

   public boolean isEarlyStop() {
      return earlyStop;
   }

   public void setVms(final int vms) {
      this.vms = vms;
   }

   public void setDuration(final int duration) {
      this.duration = duration;
   }

   public void setWarmup(final int warmup) {
      this.warmup = warmup;
   }

   public void setIterations(final int iterations) {
      this.iterations = iterations;
   }

   public void setRepetitions(final int repetitions) {
      this.repetitions = repetitions;
   }

   public void setUseKieker(final boolean useKieker) {
      this.useKieker = useKieker;
   }

   public void setUseGC(final boolean useGC) {
      this.useGC = useGC;
   }

   public void setEarlyStop(final boolean earlyStop) {
      this.earlyStop = earlyStop;
   }

   public AllowedKiekerRecord getRecord() {
      return record;
   }

   public void setRecord(final AllowedKiekerRecord record) {
      this.record = record;
   }

   public void setMeasurementStrategy(final MeasurementStrategy measurementStrategy) {
      this.measurementStrategy = measurementStrategy;
   }

   public MeasurementStrategy getMeasurementStrategy() {
      return measurementStrategy;
   }

   public void setDirectlyMeasureKieker(boolean directlyMeasureKieker) {
      this.directlyMeasureKieker = directlyMeasureKieker;
   }

   public boolean isDirectlyMeasureKieker() {
      return directlyMeasureKieker;
   }

}
