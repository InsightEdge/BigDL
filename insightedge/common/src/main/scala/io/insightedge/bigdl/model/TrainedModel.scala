//   scalastyle:off

package io.insightedge.bigdl.model

import com.gigaspaces.metadata.StorageType
import com.intel.analytics.bigdl.Module
import org.insightedge.scala.annotation.{SpaceId, SpaceStorageType}

import scala.beans.BeanProperty

/**
  * @author Danylo_Hurin.
  */
case class TrainedModel(
                         @BeanProperty
                         @SpaceId(autoGenerate = true)
                         var id: String,

                         @BeanProperty
                         @SpaceStorageType(storageType = StorageType.BINARY)
                         var modelBinary: Module[Float]
                       ) {
  def this() = this(null, null)

}