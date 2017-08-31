//   scalastyle:off

package io.insightedge.bigdl.model

import org.insightedge.scala.annotation.SpaceId

import scala.beans.BeanProperty

/**
  * @author Danylo_Hurin.
  */
case class WordMetainfo(
                         @BeanProperty
                         @SpaceId(autoGenerate = true)
                         var id: String,

                         @BeanProperty
                         var word: String,

                         @BeanProperty
                         var count: Int,

                         @BeanProperty
                         var wordIndex: Int
                       ) {
  def this() = this(null, null, -1, -1)

}


