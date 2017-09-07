//   scalastyle:off

package model

/**
  * @author Danylo_Hurin.
  */
object grid {

  case class ModelStats(
                         time: String,
                         accuracy: String
                       )

}

object web {

  case class Speech(speech: String) {
    override def toString: String = speech
  }

}
