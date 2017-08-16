/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.ml.r

import org.apache.hadoop.fs.Path
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.attribute.AttributeGroup
import org.apache.spark.ml.feature.RFormula
import org.apache.spark.ml.r.RWrapperUtils._
import org.apache.spark.ml.regression._
import org.apache.spark.ml.util._
import org.apache.spark.sql._

private[r] class GeneralizedLinearRegressionWrapper private (
    val pipeline: PipelineModel,
    val rFeatures: Array[String],
    val rCoefficients: Array[Double],
    val rDispersion: Double,
    val rNullDeviance: Double,
    val rDeviance: Double,
    val rResidualDegreeOfFreedomNull: Long,
    val rResidualDegreeOfFreedom: Long,
    val rAic: Double,
    val rNumIterations: Int,
    val isLoaded: Boolean = false) extends MLWritable {

  private val glm: GeneralizedLinearRegressionModel =
    pipeline.stages(1).asInstanceOf[GeneralizedLinearRegressionModel]

  lazy val rDevianceResiduals: DataFrame = glm.summary.residuals()

  lazy val rFamily: String = glm.getFamily

  def residuals(residualsType: String): DataFrame = glm.summary.residuals(residualsType)

  def transform(dataset: Dataset[_]): DataFrame = {
    pipeline.transform(dataset).drop(glm.getFeaturesCol)
  }

  override def write: MLWriter =
    new GeneralizedLinearRegressionWrapper.GeneralizedLinearRegressionWrapperWriter(this)
}

private[r] object GeneralizedLinearRegressionWrapper
  extends MLReadable[GeneralizedLinearRegressionWrapper] {

  def fit(
      formula: String,
      data: DataFrame,
      family: String,
      link: String,
      tol: Double,
      maxIter: Int,
      weightCol: String,
      regParam: Double): GeneralizedLinearRegressionWrapper = {
    val rFormula = new RFormula().setFormula(formula)
    checkDataColumns(rFormula, data)
    val rFormulaModel = rFormula.fit(data)
    // get labels and feature names from output schema
    val schema = rFormulaModel.transform(data).schema
    val featureAttrs = AttributeGroup.fromStructField(schema(rFormula.getFeaturesCol))
      .attributes.get
    val features = featureAttrs.map(_.name.get)
    // assemble and fit the pipeline
    val glr = new GeneralizedLinearRegression()
      .setFamily(family)
      .setLink(link)
      .setFitIntercept(rFormula.hasIntercept)
      .setTol(tol)
      .setMaxIter(maxIter)
      .setWeightCol(weightCol)
      .setRegParam(regParam)
      .setFeaturesCol(rFormula.getFeaturesCol)
    val pipeline = new Pipeline()
      .setStages(Array(rFormulaModel, glr))
      .fit(data)

    val glm: GeneralizedLinearRegressionModel =
      pipeline.stages(1).asInstanceOf[GeneralizedLinearRegressionModel]
    val summary = glm.summary

    val rFeatures: Array[String] = if (glm.getFitIntercept) {
      Array("(Intercept)") ++ features
    } else {
      features
    }

    val rCoefficients: Array[Double] = if (summary.isNormalSolver) {
      val rCoefficientStandardErrors = if (glm.getFitIntercept) {
        Array(summary.coefficientStandardErrors.last) ++
          summary.coefficientStandardErrors.dropRight(1)
      } else {
        summary.coefficientStandardErrors
      }

      val rTValues = if (glm.getFitIntercept) {
        Array(summary.tValues.last) ++ summary.tValues.dropRight(1)
      } else {
        summary.tValues
      }

      val rPValues = if (glm.getFitIntercept) {
        Array(summary.pValues.last) ++ summary.pValues.dropRight(1)
      } else {
        summary.pValues
      }

      if (glm.getFitIntercept) {
        Array(glm.intercept) ++ glm.coefficients.toArray ++
          rCoefficientStandardErrors ++ rTValues ++ rPValues
      } else {
        glm.coefficients.toArray ++ rCoefficientStandardErrors ++ rTValues ++ rPValues
      }
    } else {
      if (glm.getFitIntercept) {
        Array(glm.intercept) ++ glm.coefficients.toArray
      } else {
        glm.coefficients.toArray
      }
    }

    val rDispersion: Double = summary.dispersion
    val rNullDeviance: Double = summary.nullDeviance
    val rDeviance: Double = summary.deviance
    val rResidualDegreeOfFreedomNull: Long = summary.residualDegreeOfFreedomNull
    val rResidualDegreeOfFreedom: Long = summary.residualDegreeOfFreedom
    val rAic: Double = summary.aic
    val rNumIterations: Int = summary.numIterations

    new GeneralizedLinearRegressionWrapper(pipeline, rFeatures, rCoefficients, rDispersion,
      rNullDeviance, rDeviance, rResidualDegreeOfFreedomNull, rResidualDegreeOfFreedom,
      rAic, rNumIterations)
  }

  override def read: MLReader[GeneralizedLinearRegressionWrapper] =
    new GeneralizedLinearRegressionWrapperReader

  override def load(path: String): GeneralizedLinearRegressionWrapper = super.load(path)

  class GeneralizedLinearRegressionWrapperWriter(instance: GeneralizedLinearRegressionWrapper)
    extends MLWriter {

    override protected def saveImpl(path: String): Unit = {
      val rMetadataPath = new Path(path, "rMetadata").toString
      val pipelinePath = new Path(path, "pipeline").toString

      val rMetadata = ("class" -> instance.getClass.getName) ~
        ("rFeatures" -> instance.rFeatures.toSeq) ~
        ("rCoefficients" -> instance.rCoefficients.toSeq) ~
        ("rDispersion" -> instance.rDispersion) ~
        ("rNullDeviance" -> instance.rNullDeviance) ~
        ("rDeviance" -> instance.rDeviance) ~
        ("rResidualDegreeOfFreedomNull" -> instance.rResidualDegreeOfFreedomNull) ~
        ("rResidualDegreeOfFreedom" -> instance.rResidualDegreeOfFreedom) ~
        ("rAic" -> instance.rAic) ~
        ("rNumIterations" -> instance.rNumIterations)
      val rMetadataJson: String = compact(render(rMetadata))
      sc.parallelize(Seq(rMetadataJson), 1).saveAsTextFile(rMetadataPath)

      instance.pipeline.save(pipelinePath)
    }
  }

  class GeneralizedLinearRegressionWrapperReader
    extends MLReader[GeneralizedLinearRegressionWrapper] {

    override def load(path: String): GeneralizedLinearRegressionWrapper = {
      implicit val format = DefaultFormats
      val rMetadataPath = new Path(path, "rMetadata").toString
      val pipelinePath = new Path(path, "pipeline").toString

      val rMetadataStr = sc.textFile(rMetadataPath, 1).first()
      val rMetadata = parse(rMetadataStr)
      val rFeatures = (rMetadata \ "rFeatures").extract[Array[String]]
      val rCoefficients = (rMetadata \ "rCoefficients").extract[Array[Double]]
      val rDispersion = (rMetadata \ "rDispersion").extract[Double]
      val rNullDeviance = (rMetadata \ "rNullDeviance").extract[Double]
      val rDeviance = (rMetadata \ "rDeviance").extract[Double]
      val rResidualDegreeOfFreedomNull = (rMetadata \ "rResidualDegreeOfFreedomNull").extract[Long]
      val rResidualDegreeOfFreedom = (rMetadata \ "rResidualDegreeOfFreedom").extract[Long]
      val rAic = (rMetadata \ "rAic").extract[Double]
      val rNumIterations = (rMetadata \ "rNumIterations").extract[Int]

      val pipeline = PipelineModel.load(pipelinePath)

      new GeneralizedLinearRegressionWrapper(pipeline, rFeatures, rCoefficients, rDispersion,
        rNullDeviance, rDeviance, rResidualDegreeOfFreedomNull, rResidualDegreeOfFreedom,
        rAic, rNumIterations, isLoaded = true)
    }
  }
}
