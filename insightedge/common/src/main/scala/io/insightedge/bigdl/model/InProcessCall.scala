//   scalastyle:off

package io.insightedge.bigdl.model

import org.insightedge.scala.annotation.SpaceId
//import org.openspaces.textsearch.SpaceTextIndex

import scala.beans.BeanProperty

/**
  * @author Danylo_Hurin.
  */
case class InProcessCall(
                 @BeanProperty
                 @SpaceId(autoGenerate = true)
                 var id: String,

                 @BeanProperty
                 var speech: String
) {
  def this() = this(null, null)

}


