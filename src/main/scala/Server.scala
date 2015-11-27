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

import java.util.Date
import scala.math.BigInt

import Nodes._

object Project4 extends App {

  case class caseUser(userID: BigInt, creationDate: String, firstName: String, lastName: String, dateOfBirth: String, friends: Map[Int,BigInt],
                  friendRequests: Map[Int,BigInt], posts: Map[Int,BigInt])

  case class casePost(postID: BigInt, createdBy: BigInt, creationDate: String, content: String, location: String)

  case class casePage(pageID: BigInt, createdBy: BigInt, creationDate: String, name: String, description: String, likesList: Map[Int,BigInt],
                  posts: Map[Int,BigInt])
                  
  case class caseComment(commentID: BigInt, createdBy: BigInt, creationDate: String, content: String, likesCount : BigInt, likesList: Map[Int,BigInt])

  object caseUser extends DefaultJsonProtocol {
    implicit val iPerson = jsonFormat8(caseUser.apply)
  }

  object casePost extends DefaultJsonProtocol {
    implicit val iPost = jsonFormat5(casePost.apply)
  }

  object casePage extends DefaultJsonProtocol {
    implicit val iPage = jsonFormat7(casePage.apply)
  }
  
  object caseComment extends DefaultJsonProtocol {
    implicit val iComment = jsonFormat6(caseComment.apply)
  }
  
  case class registerNewUser(reqContext: RequestContext, byUser: BigInt, newCaseUser: caseUser)
  case class getUserInfo(reqContext: RequestContext, ofUser: BigInt)

  implicit val serverActorSystem = ActorSystem()

  val receiverActor = serverActorSystem.actorOf(Props[Server], "receiverRequests")

  IO(Http) ! Http.Bind(receiverActor, interface = "localhost", port = 8080)

  var userDB: Map[BigInt, User] = new HashMap[BigInt, User]()

  val userActor = serverActorSystem.actorOf(Props(new UserActor()), "userActor")
  
  val userIdSeq = BigInt("64", 1);

  class Server extends Actor with userTrait {

    def actorRefFactory = context
    implicit val rSettings = RoutingSettings.default(context)

    def receive = runRoute(receivePathPost
      ~ receivePathUser
      ~ receivePathPage)
  }

  trait userTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._
    
    val receivePathPost = {
      get {
        path(Segment / Segment / "userInfo") { (byUser, ofUser) =>
          requestContext =>
            userActor ! getUserInfo(requestContext, ofUser)
        }
      } ~
        post {
          path(Segment / "registerUser") { (byUser) =>
            entity(as[caseUser]) { newUserInfo =>
              requestContext =>
                userActor ! registerNewUser(requestContext, byUser, newUserInfo)
            }
          }
        }
    }
  }

  class UserActor() extends Actor {

    def receive = {
      
      case registerNewUser(reqContext: RequestContext, byUser: BigInt, newCaseUser: caseUser) =>
        var newUser: User = new User(newCaseUser.userID, newCaseUser.creationDate, newCaseUser.firstName,
                                     newCaseUser.lastName, newCaseUser.dateOfBirth, newCaseUser.friends,
                                     newCaseUser.friendRequests, newCaseUser.posts)
        
        userDB += (userIdSeq+1) -> newUser
        
      
      case getUserInfo(reqContext: RequestContext, ofUser: BigInt) =>
        
        if(userDB.contains(ofUser)){
          
           reqContext.complete(returnJson.toString())
        }        
        
    }
    
  }

}


