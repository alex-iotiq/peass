package de.dagere.peass.measurement.rca.treeanalysis;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.statistics.Relation;
import de.dagere.peass.measurement.statistics.StatisticUtil;
import de.dagere.peass.measurement.statistics.bimodal.CompareData;
import de.dagere.peass.measurement.statistics.bimodal.OutlierRemoverBimodal;

public abstract class DifferentNodeDeterminer {

   private static final Logger LOG = LogManager.getLogger(DifferentNodeDeterminer.class);

   protected List<CallTreeNode> measurePredecessor = new LinkedList<>();

   protected final List<CallTreeNode> levelDifferentPrecessor = new LinkedList<>();

   protected final CauseSearcherConfig causeSearchConfig;
   protected final MeasurementConfig measurementConfig;

   public DifferentNodeDeterminer(final CauseSearcherConfig causeSearchConfig, final MeasurementConfig measurementConfig) {
      this.causeSearchConfig = causeSearchConfig;
      this.measurementConfig = measurementConfig;
   }

   public void calculateDiffering() {
      final Iterator<CallTreeNode> predecessorIterator = measurePredecessor.iterator();
      for (; predecessorIterator.hasNext();) {
         final CallTreeNode currentPredecessorNode = predecessorIterator.next();
         CompareData cd = currentPredecessorNode.getComparableStatistics(measurementConfig.getFixedCommitConfig().getCommitOld(), measurementConfig.getFixedCommitConfig().getCommit());
         calculateNodeDifference(currentPredecessorNode, cd);
      }
   }

   private void calculateNodeDifference(final CallTreeNode currentPredecessorNode, final CompareData cd) {
      if (cd.getBeforeStat() == null || cd.getAfterStat() == null) {
         LOG.debug("Statistics is null, is different: {} vs {}", cd.getBeforeStat(), cd.getAfterStat());
         levelDifferentPrecessor.add(currentPredecessorNode);
      } else {
         final CompareData cleaned = removeOutliers(cd);
         printComparisonInfos(currentPredecessorNode, cleaned.getBeforeStat(), cleaned.getAfterStat());
         checkNodeDiffering(currentPredecessorNode, cleaned);
      }
   }

   private CompareData removeOutliers(final CompareData cd) {
      final CompareData cleaned;
      if (measurementConfig.getStatisticsConfig().getOutlierFactor() != 0 && cd.getAfter().length > 1 && cd.getBefore().length > 1) {
         cleaned = OutlierRemoverBimodal.removeOutliers(cd, measurementConfig.getStatisticsConfig().getOutlierFactor());
      } else {
         cleaned = cd;
      }
      return cleaned;
   }

   private void checkNodeDiffering(final CallTreeNode currentPredecessorNode, final CompareData cleaned) {
      if (cleaned.getBeforeStat().getN() > 0 && cleaned.getAfterStat().getN() > 0) {
         final Relation relation = StatisticUtil.isDifferent(cleaned, measurementConfig.getStatisticsConfig());
         boolean needsEnoughTime = needsEnoughTime(cleaned.getBeforeStat(), cleaned.getAfterStat());
         LOG.debug("Relation: {} Needs enough time: {}", relation, needsEnoughTime);
         if ((relation == Relation.UNEQUAL || relation == Relation.GREATER_THAN || relation == Relation.LESS_THAN)) {
            addChildsToMeasurement(currentPredecessorNode, cleaned.getBeforeStat(), cleaned.getAfterStat());
         } else {
            LOG.info("No remeasurement");
         }
      }
   }

   private void printComparisonInfos(final CallTreeNode currentPredecessorNode, final SummaryStatistics statisticsPredecessor, final SummaryStatistics statisticsVersion) {
      LOG.debug("Comparison {} - {}",
            currentPredecessorNode.getKiekerPattern(),
            currentPredecessorNode.getOtherCommitNode() != null ? currentPredecessorNode.getOtherCommitNode().getKiekerPattern() : null);
      LOG.debug("Predecessor: {} {} Current: {} {} ",
            statisticsPredecessor.getMean(), statisticsPredecessor.getStandardDeviation(),
            statisticsVersion.getMean(), statisticsVersion.getStandardDeviation());
   }

   private void addChildsToMeasurement(final CallTreeNode currentPredecessorNode, final SummaryStatistics statisticsPredecessor, final SummaryStatistics statisticsVersion) {
      LOG.debug("Adding {} - T={}", currentPredecessorNode, TestUtils.homoscedasticT(statisticsPredecessor, statisticsVersion));
      levelDifferentPrecessor.add(currentPredecessorNode);
   }

   private boolean needsEnoughTime(final SummaryStatistics statisticsPredecessor, final SummaryStatistics statisticsVersion) {
      double relativeDifference = Math.abs(statisticsPredecessor.getMean() - statisticsVersion.getMean()) / statisticsVersion.getMean();
      double relativeDeviationPredecessor = statisticsPredecessor.getStandardDeviation() / statisticsPredecessor.getMean();
      double relativeDeviationVersion = statisticsVersion.getStandardDeviation() / statisticsVersion.getMean();
      double relativeStandardDeviation = Math.sqrt((Math.pow(relativeDeviationPredecessor, 2) +
            Math.pow(relativeDeviationVersion, 2)) / 2);
      return relativeDifference > causeSearchConfig.getMinTime() * relativeStandardDeviation;
   }

   public List<CallTreeNode> getLevelDifferentPredecessor() {
      return levelDifferentPrecessor;
   }
   
   public List<CallTreeNode> getLevelDifferentCurrent(){
      final List<CallTreeNode> differentPredecessor = new LinkedList<>();
      levelDifferentPrecessor.forEach(node -> differentPredecessor.add(node.getOtherCommitNode()));
      return differentPredecessor;
   }
}