package io.insightedge.bigdl.regression

import org.insightedge.scala.annotation.SpaceId
import scala.beans.BeanProperty

case class Prediction(
                       @BeanProperty
                       @SpaceId(autoGenerate = true)
                       var id: String,

                       @BeanProperty
                       var input: String,

                       @BeanProperty
                       var prediction: Double,

                       @BeanProperty
                       var probabilities: String) {

  def this() = this(null, null, -1d, null)
}