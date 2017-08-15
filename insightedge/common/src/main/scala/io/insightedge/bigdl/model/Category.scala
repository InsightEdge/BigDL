//   scalastyle:off

package io.insightedge.bigdl.model

import org.insightedge.scala.annotation.SpaceId

import scala.beans.BeanProperty

case class Category(
                     @BeanProperty
                     @SpaceId(autoGenerate = true)
                     var id: String,

                     @BeanProperty
                     var name: String,

                     @BeanProperty
                     var label: Int) {
  def this() = this(null, null, -1)

}
