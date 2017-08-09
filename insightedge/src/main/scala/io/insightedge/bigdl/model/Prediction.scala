//   scalastyle:off

package io.insightedge.bigdl.model

import org.insightedge.scala.annotation.SpaceId
//import org.openspaces.textsearch.SpaceTextIndex

import scala.beans.BeanProperty

/**
  * @author Danylo_Hurin.
  */
case class Prediction(
                 @BeanProperty
                 @SpaceId(autoGenerate = true)
                 var id: String,

                 @BeanProperty
//                 @SpaceTextIndex
                 var text: String,

                 @BeanProperty
                 var category: String,

                 @BeanProperty
                 var label: Int) {
  def this() = this(null, null, null, -1)

}
