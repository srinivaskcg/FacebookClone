import akka.actor.{ ActorSystem, Actor, Props }
import akka.io.IO
import akka.util.Timeout

import spray.json._
import spray.json.DefaultJsonProtocol

import spray.can.Http
import spray.can.server.Stats
import spray.can.Http.RegisterChunkHandler

import spray.httpx.marshalling.Marshaller

import spray.routing.HttpService
import spray.routing.RequestContext
import spray.routing.RoutingSettings

import DefaultJsonProtocol._

import scala.concurrent.duration._
import scala.collection.immutable.HashMap
import scala.util.Random
import scala.math.BigInt

import java.util.Date

import Nodes._
import Common._

object Project4 extends App {



  implicit val serverActorSystem = ActorSystem()

  val receiverActor = serverActorSystem.actorOf(Props[Server], "receiverRequests")

  IO(Http) ! Http.Bind(receiverActor, interface = "localhost", port = 8080)

  var userDB: Map[BigInt, User] = new HashMap[BigInt, User]()

  val userActor = serverActorSystem.actorOf(Props(new UserActor()), "userActor")

  val userIdSeq = BigInt("64", 1);

  class Server extends Actor with userTrait {

    def actorRefFactory = context
    implicit val routerSettings = RoutingSettings.default(context)

    def receive = runRoute(receivePathPost
      //~ receivePathUser
      //~ receivePathPage
        )
  }

  trait userTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathPost = {
      get {
        path(LongNumber / LongNumber / "userInfo") { (byUser, ofUser) =>
          requestContext =>
            userActor ! getUserInfo(requestContext, ofUser)
        }
      } ~
        post {
          path("registerUser") { 
            entity(as[caseUser]) { newUserInfo =>
              requestContext =>
                userActor ! registerNewUser(requestContext, newUserInfo)
            }
          }
        }
    }
  }

  class UserActor() extends Actor {

    def receive = {

      case registerNewUser(reqContext: RequestContext, newCaseUser: caseUser) =>
        var newUser: User = new User(newCaseUser.userID, newCaseUser.creationDate, newCaseUser.firstName,
          newCaseUser.lastName, newCaseUser.dateOfBirth, newCaseUser.friends,
          newCaseUser.friendRequests, newCaseUser.posts)

        userDB += (userIdSeq + 1) -> newUser

      case getUserInfo(reqContext: RequestContext, ofUser: BigInt) =>

        if (userDB.contains(ofUser)) {

          var user: User = userDB(ofUser)

          var userInfo: caseUser = new caseUser(user.userID, user.creationDate, user.firstName,
            user.lastName, user.dateOfBirth, user.friends,
            user.friendRequests, user.posts)

          var returnJson = userInfo.toJson

          reqContext.complete(returnJson.toString())
        }

    }

  }

}


