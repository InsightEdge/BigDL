//   scalastyle:off

package io.insightedge.bigdl.model

import org.insightedge.scala.annotation.SpaceId

import scala.beans.BeanProperty

/**
  * @author Danylo_Hurin.
  */
case class CallSession(
                 @BeanProperty
                 @SpaceId(autoGenerate = false) // same as Prediction/
                 var id: String,

                 @BeanProperty
                 var text: String,

                 @BeanProperty
                 var category: String,

                 @BeanProperty // dummy data
                 var agentId: String,

                 @BeanProperty
                 var timeInMilliseconds: Long,

                 @BeanProperty
                 var counter: Long
) {
  def this() = this(null, null, null, null, -1l, -1)

}


