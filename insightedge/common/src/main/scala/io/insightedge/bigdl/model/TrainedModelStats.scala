//   scalastyle:off

package io.insightedge.bigdl.model

import org.insightedge.scala.annotation.SpaceId

import scala.beans.BeanProperty

/**
  * @author Danylo_Hurin.
  */
case class TrainedModelStats(
                        @BeanProperty
                        @SpaceId(autoGenerate = true)
                        var id: String,

                        @BeanProperty
                        var timeInMilliseconds: Long,

                        @BeanProperty
                        var accuracy: Float
                      ) {
  def this() = this(null, -1, -1f)

}
