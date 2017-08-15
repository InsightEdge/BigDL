//   scalastyle:off

package io.insightedge.bigdl.model

import org.insightedge.scala.annotation.SpaceId
//import org.openspaces.textsearch.SpaceTextIndex

import scala.beans.BeanProperty

/**
  * @author Danylo_Hurin.
  */
case class CallSession(
                 @BeanProperty
                 @SpaceId(autoGenerate = false) // same as Prediction/
                 var id: String,

                 @BeanProperty
//                 @SpaceTextIndex
                 var text: String,

                 @BeanProperty
                 var category: String,

                 @BeanProperty // dummy data
                 var agentId: String,

                 @BeanProperty
                 var counter: Long
) {
  def this() = this(null, null, null, null, -1)

}


