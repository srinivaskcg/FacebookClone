import akka.actor._
import akka.actor.Actor
import akka.actor.ActorSystem
import spray.httpx.SprayJsonSupport
import spray.json.AdditionalFormats
import spray.json.{ JsonFormat, DefaultJsonProtocol }
import spray.client.pipelining._
import spray.http._
import Nodes._
import scala.util.{ Success, Failure }
import scala.concurrent.Future

import Common._

class Client extends Actor with ActorLogging with SprayJsonSupport {

  case class createUser(newUser : caseUser)
  case class getUserData(id: BigInt)

  implicit val system = ActorSystem()
  import system.dispatcher
  
  val pipeline : HttpRequest => Future[HttpResponse] = sendReceive 

  def receive = {

    //var newUser : caseUser =  new caseUser("Srinivas", "Gubbala")
    
    case createUser(newUser) =>
      val responseFuture = pipeline ( Put("localhost.com/8080/") )
      responseFuture onComplete {
        case Success(user: User) =>
          log.info("User registered successfully")
          //sender ! userCreated(user)

        case Failure(error) =>
          log.error(error, "Error in user registration")

      }
  }
}


