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
                       var text: String,

                       @BeanProperty
                       var category: String,

                       @BeanProperty
                       var label: Int,

                       @BeanProperty
                       var timeInMilliseconds: Long,

                       @BeanProperty
                       var flag: Int) {
  def this() = this(null, null, null, -1, -1l, -1)

}
