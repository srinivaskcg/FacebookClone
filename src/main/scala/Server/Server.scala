package Server

import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.util.Timeout
import spray.can.Http
import Routes._

import scala.concurrent.duration._
import scala.language.postfixOps

class Server extends Actor with ActorLogging {
  implicit val timeout: Timeout = 1 second

  def receive = {
    case _: Http.Connected =>
      val httphandler = context.actorOf(Client.props())
      log.info("received connection from " + sender.path.address.host)
      sender ! Http.Register(httphandler)
  }
}
