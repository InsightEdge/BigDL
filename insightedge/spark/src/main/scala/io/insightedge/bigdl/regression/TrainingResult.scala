package io.insightedge.bigdl.regression

import org.insightedge.scala.annotation.SpaceId
import scala.beans.BeanProperty


/**
  * @author Danylo_Hurin.
  */
case class TrainingResult(
                           @BeanProperty
                           @SpaceId(autoGenerate = true)
                           var id: String,

                           @BeanProperty
                           var inputData: String,

                           @BeanProperty
                           var features: String,

                           @BeanProperty
                           var prediction: Double,

                           @BeanProperty
                           var probabilities: String) {

  def this() = this(null, null, null, -1d, null)
}