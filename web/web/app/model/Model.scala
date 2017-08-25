//   scalastyle:off

package model

/**
  * @author Danylo_Hurin.
  */
object grid {

  case class CallSession(
                     id: String,
                     category: String,
                     agentId: String,
                     text: String,
                     counter: Long
                   )

}

object web {

  case class Speech(speech: String) {
    override def toString: String = speech

    def asJson(id: String) = {
      s""""
        |{"id":"$id","speech":"$speech"}
      """.stripMargin
    }
  }
}
