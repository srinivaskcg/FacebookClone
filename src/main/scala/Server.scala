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

  IO(Http) ! Http.Bind(receiverActor, interface = "localhost", port = 8082)

  var userDB: Map[String, User] = new HashMap[String, User]()

  val userActor = serverActorSystem.actorOf(Props(new UserActor()), "userActor")

  class Server extends Actor with userTrait //with postTrait with pageTrait with commentTrait 
  {

    def actorRefFactory = context
    implicit val routerSettings = RoutingSettings.default(context)

    def receive = runRoute(receivePathUser
      //~ receivePathPost
      //~ receivePathPage
      //~ receivePathComment
        )
  }

  trait userTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathUser = {
        path("registerUser") {
          entity(as[caseUser]) { newUserInfo =>

            var newUser: User = new User()

            newUser.userID = newUserInfo.userID
            newUser.firstName = newUserInfo.firstName
            newUser.lastName = newUserInfo.lastName
            newUser.dateOfBirth = newUserInfo.dateOfBirth
            newUser.creationDate = newUserInfo.creationDate
            newUser.email = newUserInfo.email
          
            userDB .+= (newUser.userID -> newUser)

            userDB.+=(newUserInfo.userID -> newUser)
            complete("Registered New User")
          }
        } ~ get {
          path(Segment/"userInfo"){(ofUser)=>
            println(ofUser)
            requestContext =>
              userActor ! serverGetUserInfo(requestContext, ofUser)
          }
        } ~ post {
          path(Segment / Segment / "friendRequest") { (sender, receiver) =>
            println("friend Request")
            requestContext =>
              userActor ! serverSendFriendRequest(requestContext, sender, receiver)
          } ~
            path(Segment / Segment / "handleRequest") { (sentUser, reqUser) =>
              entity(as[String]){action =>
              requestContext =>
                userActor ! serverManageFriendRequest(requestContext, sentUser, reqUser, action)
            }
          }
        } ~ delete {
          path(IntNumber / "deleteUser") { ofUser =>
            requestContext =>
              userActor ! deleteUser(ofUser)
          }
        }
    }
  }

 
  class UserActor() extends Actor {

    def receive = {
      
      case serverGetUserInfo(reqContext : RequestContext, ofUser : String)=> 
        println(ofUser)
        println(userDB.get(ofUser).get.userID)
        var returnCU = new caseUser(userDB.get(ofUser).get.userID, userDB.get(ofUser).get.firstName, userDB.get(ofUser).get.lastName,
            userDB.get(ofUser).get.dateOfBirth, userDB.get(ofUser).get.creationDate, userDB.get(ofUser).get.email)
        println(returnCU)
        var returnCaseUser = returnCU.toJson
        println(returnCaseUser)
        reqContext.complete(returnCaseUser.toString())

      case serverSendFriendRequest(reqContext: RequestContext, senderID: String, receiverID: String) =>
        println("friend Request case")
        if (receiverID.equals(senderID)) {
          println("If")
          reqContext.complete("User request sent to self")
        } else if (userDB.get(receiverID).get.pendingRequests.contains(senderID)) {
          println("else if")
          reqContext.complete(receiverID + " already has a pending friend request from " + senderID)
        } else {
          println("else")
          userDB.get(receiverID).get.pendingRequests += (senderID -> senderID)
          println("Pending friend list of " + senderID + " = " + userDB.get(receiverID).get.pendingRequests)
          reqContext.complete("Friend Request from " + senderID + " from " + receiverID + " sent successfully\n" +
            "Pending friend List of " + receiverID + " now is " + userDB.get(receiverID).get.pendingRequests)
        }

      case serverManageFriendRequest(reqContext: RequestContext, senderID: String, receiverID: String, action: String) =>
        println("processing friend request of " + senderID + " by user " + receiverID)
        println("pendingList of " + receiverID + " before processing of the response= " + userDB.get(receiverID).get.pendingRequests)
        var pendingList = userDB.get(receiverID).get.pendingRequests
        if (pendingList.contains(senderID)) {
          if (action.equalsIgnoreCase("Accept")) {
            userDB.get(receiverID).get.friends += (pendingList.get(senderID).get -> pendingList.get(senderID).get)
            userDB.get(receiverID).get.pendingRequests -= (senderID)
            reqContext.complete(receiverID + "accepted the friend request of " + senderID)
            println("Friend List of user " + receiverID + "= " + userDB(receiverID).friends)
            println("Pending Friend List of user " + receiverID + "= " + userDB.get(receiverID).get.pendingRequests)
          } else if (action.equalsIgnoreCase("Reject")) {
            userDB.get(receiverID).get.pendingRequests.-=("ofUser")
            reqContext.complete(receiverID + " rejected the friend request of " + receiverID)
          }
        } else {
          if (userDB.get(receiverID).get.friends.contains(senderID)) {
            reqContext.complete(senderID + " is already a friend of " + receiverID)
          } else {
            reqContext.complete(senderID + " is already a friend of " + receiverID)
          }
        }
    }
  }
  
/*   trait pageTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathPage = {
      pathPrefix("page") {
        path("createPage") {
          entity(as[casePage]) { newPageInfo =>
            println("Finally Inside")
            var newPage: Page = new Page(newUserInfo.userID, newUserInfo.creationDate, newUserInfo.firstName,
              newUserInfo.lastName, newUserInfo.dateOfBirth)

            userDB.+=(newUserInfo.userID -> newUser)
            complete("Registered New User")
          }
        } ~ get {
          path(IntNumber / "pageInfo") { ofUser =>
            requestContext =>
              pageActor ! getPageInfo(ofUser)
          }
        } ~ post {
          path(IntNumber / IntNumber / "updatePage") { (pageID, chgCasePage) =>
            requestContext =>
              pageActor ! updatePageInfo(pageID, chgCasePage)
          }
        } ~ delete {
          path(IntNumber / "deletePage") { ofUser =>
            requestContext =>
              pageActor ! deletePage(ofUser)
          }
        }
      }
    }
  }

  trait postTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathPost = {
      pathPrefix("post") {
        path("createPost") {
          entity(as[caseUser]) { newUserInfo =>
            println("Finally Inside")
            var newUser: User = new User(newUserInfo.userID, newUserInfo.creationDate, newUserInfo.firstName,
              newUserInfo.lastName, newUserInfo.dateOfBirth)

            userDB.+=(newUserInfo.userID -> newUser)
            complete("Registered New User")
          }
        } ~ get {
          path(IntNumber / "postInfo") { ofUser =>
            requestContext =>
              postActor ! getPostInfo(requestContext, ofUser)
          }
        } ~ post {
          path(IntNumber / IntNumber / "updatePost") { (sentUser, reqUser) =>
            requestContext =>
              postActor ! updatePost(requestContext, sentUser, reqUser)
          }
        } ~ delete {
          path(IntNumber / "deletePost") { ofUser =>
            requestContext =>
              postActor ! deletePost(requestContext, ofUser)
          }
        }
      }
    }
  }

  trait commentTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathComment = {
      pathPrefix("comment") {
        path("createComment") {
          entity(as[caseUser]) { newUserInfo =>
            println("Finally Inside")
            var newUser: User = new User(newUserInfo.userID, newUserInfo.creationDate, newUserInfo.firstName,
              newUserInfo.lastName, newUserInfo.dateOfBirth)

            userDB.+=(newUserInfo.userID -> newUser)
            complete("Registered New User")
          }
        } ~ get {
          path(IntNumber / "getComment") { ofUser =>
            requestContext =>
              commentActor ! getPostInfo(requestContext, ofUser)
          }
        } ~ post {
          path(IntNumber / IntNumber / "updateComment") { (sentUser, reqUser) =>
            requestContext =>
              commentActor ! updateComment(requestContext, sentUser, reqUser)
          }
        } ~ delete {
          path(IntNumber / "deleteComment") { ofUser =>
            requestContext =>
              commentActor ! deleteComment(requestContext, ofUser)
          }
        }
      }
    }
  }
  
  class PostActor() extends Actor {

    def receive = {
      case getUserInfo(reqContext: RequestContext, ofUser: Int) =>

        if (userDB.contains(ofUser)) {

          var user: User = userDB(ofUser)

          var userInfo: caseUser = new caseUser(user.userID, user.creationDate, user.firstName,
            user.lastName, user.dateOfBirth)

          var returnJson = userInfo.toJson

          reqContext.complete(returnJson.toString())
        }
    }
  }

  class PageActor() extends Actor {

    def receive = {
      case getUserInfo(reqContext: RequestContext, ofUser: Int) =>

        if (userDB.contains(ofUser)) {

          var user: User = userDB(ofUser)

          var userInfo: caseUser = new caseUser(user.userID, user.creationDate, user.firstName,
            user.lastName, user.dateOfBirth)

          var returnJson = userInfo.toJson

          reqContext.complete(returnJson.toString())
        }
    }
  }

  class CommentActor() extends Actor {

    def receive = {
      case getUserInfo(ofUser: BigInt) =>

        if (userDB.contains(ofUser)) {

          var user: User = userDB(ofUser)

          var userInfo: caseUser = new caseUser(user.userID, user.creationDate, user.firstName,
            user.lastName, user.dateOfBirth)

          var returnJson = userInfo.toJson

          reqContext.complete(returnJson.toString())
        }
    }
  }
*/

}


