import akka.actor.{ ActorSystem, Actor, Props }
import akka.io.IO
import akka.util.Timeout

import spray.json._
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._

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
  var pageDB: Map[String, Page] = new HashMap[String, Page]()

  var userIDMap: Map[String, String] = new HashMap[String, String]()

  class Server extends Actor with userTrait with postTrait with pageTrait //with commentTrait 
  {

    def actorRefFactory = context
    implicit val routerSettings = RoutingSettings.default(context)

    def receive = runRoute(receivePathUser
      ~ receivePathPost
      ~ receivePathPage //~ receivePathComment
      )
  }

  trait userTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathUser = {
      path("registerUser") {
        entity(as[caseUser]) { newUserInfo =>

          var newUser: User = new User()

          //println("Before user creation"+ newUserInfo)
          newUser.userID = newUserInfo.createdBy //uniqueCurrentTimeMS()+""
          //println(newUser.userID)
          newUser.creationDate = newUserInfo.creationDate
          newUser.firstName = newUserInfo.firstName
          newUser.lastName = newUserInfo.lastName
          newUser.dateOfBirth = newUserInfo.dateOfBirth
          newUser.email = newUserInfo.email

          userDB.+=(newUser.userID -> newUser)

          //userIDMap .+= (newUser.firstName -> newUser.userID)

          //println("Stored in Map!!!!"+userIDMap(newUser.firstName)+"--------"+newUser.firstName)

          complete("Registered New User")
        }
      } ~ get {
        path(Segment / "userInfo") { (ofUser) =>
          println(ofUser)
          val userActor = serverActorSystem.actorOf(Props(new UserActor()), "userActor")
          requestContext =>
            userActor ! serverGetUserInfo(requestContext, ofUser)
        }
      } ~ post {
        path(Segment / Segment / "friendRequest") { (sender, receiver) =>
          println("friend Request")
          val userActor = serverActorSystem.actorOf(Props(new UserActor()), "userActor")
          requestContext =>
            userActor ! serverSendFriendRequest(requestContext, sender, receiver)
        } ~
          path(Segment / Segment / "handleRequest") { (sender, receiver) =>
            val userActor = serverActorSystem.actorOf(Props(new UserActor()), "userActor")
            entity(as[String]) { action =>
              requestContext =>
                userActor ! serverManageFriendRequest(requestContext, sender, receiver, action)
            }
          }
      } ~ delete {
        path(IntNumber / "deleteUser") { ofUser =>
          val userActor = serverActorSystem.actorOf(Props(new UserActor()), "userActor")
          requestContext =>
            userActor ! deleteUser(ofUser)
        }
      }
    }
  }

  class UserActor() extends Actor {

    def receive = {

      case serverGetUserInfo(reqContext: RequestContext, tempUserID: String) =>
        println(tempUserID)
        //println(userIDMap(ofUser))
        //var tempUserID = userIDMap(ofUser)
        var returnCU = new caseUser(userDB.get(tempUserID).get.userID, userDB.get(tempUserID).get.creationDate,
          userDB.get(tempUserID).get.firstName, userDB.get(tempUserID).get.lastName,
          userDB.get(tempUserID).get.dateOfBirth, userDB.get(tempUserID).get.email)
        //println(returnCU)
        var returnCaseUser = returnCU.toJson
        println(returnCaseUser)
        reqContext.complete(returnCaseUser.toString())

      case serverSendFriendRequest(reqContext: RequestContext, senderUserID: String, receiverUserID: String) =>
        println("friend Request case")
        //var receiverUserID = userIDMap(receiverID)
        //var senderUserID = userIDMap(senderID)

        if (receiverUserID.equals(senderUserID)) {
          println("If")
          reqContext.complete("User request sent to self")
        } else if (userDB.get(receiverUserID).get.pendingRequests.contains(senderUserID)) {
          println("else if")
          reqContext.complete(receiverUserID + " already has a pending friend request from " + senderUserID)
        } else {
          println("else")
          userDB.get(receiverUserID).get.pendingRequests += (senderUserID -> senderUserID)
          println("Pending friend list of " + senderUserID + " = " + userDB.get(receiverUserID).get.pendingRequests)
          reqContext.complete("Friend Request from " + senderUserID + " from " + receiverUserID + " sent successfully\n" +
            "Pending friend List of " + receiverUserID + " now is " + userDB.get(receiverUserID).get.pendingRequests)
        }

      case serverManageFriendRequest(reqContext: RequestContext, senderUserID: String, receiverUserID: String, action: String) =>

        //var receiverUserID = userIDMap(receiverID)
        //var senderUserID = userIDMap(senderID)

        println("processing friend request of " + senderUserID + " by user " + receiverUserID)
        println("pendingList of " + receiverUserID + " before processing of the response= " + userDB.get(receiverUserID).get.pendingRequests)
        var pendingList = userDB.get(receiverUserID).get.pendingRequests
        if (pendingList.contains(senderUserID)) {
          if (action.equalsIgnoreCase("Accept")) {
            userDB.get(receiverUserID).get.friends += (pendingList.get(senderUserID).get -> pendingList.get(senderUserID).get)
            userDB.get(receiverUserID).get.pendingRequests -= (senderUserID)
            reqContext.complete(receiverUserID + "accepted the friend request of " + senderUserID)
            println("Friend List of user " + receiverUserID + "= " + userDB(receiverUserID).friends)
            println("Pending Friend List of user " + receiverUserID + "= " + userDB.get(receiverUserID).get.pendingRequests)
          } else if (action.equalsIgnoreCase("Reject")) {
            userDB.get(receiverUserID).get.pendingRequests.-=("ofUser")
            reqContext.complete(receiverUserID + " rejected the friend request of " + receiverUserID)
          }
        } else {
          if (userDB.get(receiverUserID).get.friends.contains(senderUserID)) {
            reqContext.complete(senderUserID + " is already a friend of " + receiverUserID)
          } else {
            reqContext.complete(senderUserID + " is already a friend of " + receiverUserID)
          }
        }
    }
  }

  trait postTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathPost = {
      path("postStatus") {
        entity(as[casePost]) { newCasePost =>

          var newPost: Post = new Post()

          newPost.postID = uniqueCurrentTimeMS()
          newPost.createdBy = newCasePost.createdBy
          newPost.createdTo = newCasePost.createdTo
          newPost.creationDate = newCasePost.creationDate
          newPost.content = newCasePost.content
          newPost.location = newCasePost.location

          //if(userIDMap.contains(newPost.createdBy)){
          //var tempUserId = userIDMap.get(newPost.createdBy)
          userDB.get(newPost.createdBy).get.posts += (newPost.postID -> newPost)
          //}

          complete("Created Status")
        }
      } ~ path("postOnWall") {
        entity(as[casePost]) { newCasePost =>

          var newPost: Post = new Post()

          newPost.postID = uniqueCurrentTimeMS()
          newPost.createdBy = newCasePost.createdBy
          newPost.createdTo = newCasePost.createdTo
          newPost.creationDate = newCasePost.creationDate
          newPost.content = newCasePost.content
          newPost.location = newCasePost.location

          userDB.get(newPost.createdTo).get.posts += (newPost.postID -> newPost)

          complete("Posted on wall")
        }
      } ~ get {
        path(Segment / "posts") { ofUser =>
          println("getting posts for" + ofUser)
          
          val postActor = serverActorSystem.actorOf(Props(new PostActor()), "postActor")
          requestContext =>
            postActor ! serverGetUserPosts(requestContext, ofUser)
        }
      } /*~ post {
          path(IntNumber / IntNumber / "updatePost") { (sentUser, reqUser) =>
            requestContext =>
              postActor ! updatePost(requestContext, sentUser, reqUser)
          }
        } ~ delete {
          path(IntNumber / "deletePost") { ofUser =>
            requestContext =>
              postActor ! deletePost(requestContext, ofUser)
          }
        }*/
    }
  }

  class PostActor() extends Actor {

    def receive = {

      case serverGetUserPosts(requestContext: RequestContext, ofUser: String) =>
        import spray.httpx.SprayJsonSupport._

        println("getting posts for in function" + ofUser)
        var postMap: Map[Long, Post] = userDB(ofUser).posts
        println("got posts")
        var returnPostMap: Map[Long, casePost] = new HashMap[Long, casePost]

        for ((k, v) <- postMap) {
          println("Inside kv")
          var varCP = new casePost(v.createdBy, v.createdTo, v.creationDate, v.content, v.location)
          returnPostMap += (k -> varCP)
          println("Set kv")
        }
        println(" end of loop" + returnPostMap)
        //var retPostMap = returnPostMap.toJson
        //println(retPostMap)
        requestContext.complete(returnPostMap)
    }
  }

  trait pageTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathPage = {
      path("createPage") {
        entity(as[casePage]) { newCasePage =>

          var newPage: Page = new Page()

          newPage.pageID = newCasePage.createdBy //uniqueCurrentTimeMS()
          newPage.creationDate = newCasePage.creationDate
          newPage.name = newCasePage.name
          newPage.description = newCasePage.description

          pageDB.+=(newPage.pageID -> newPage)

          complete("Created New Page")
        }
      } ~ post {
        path(Segment / "pagePost") { pageId =>
          entity(as[casePost]) { newCasePost =>
            
          var newPost: Post = new Post()
          var pgId: String = pageId

          newPost.postID = uniqueCurrentTimeMS()
          newPost.createdBy = newCasePost.createdBy
          newPost.createdTo = newCasePost.createdTo
          newPost.creationDate = newCasePost.creationDate
          newPost.content = newCasePost.content
          newPost.location = newCasePost.location

          pageDB.get(pgId).get.posts +=(newPost.postID -> newPost)

          complete("Created Page Post")
          }
        }
      }
    }
  }

  class PageActor() extends Actor {

    def receive = {

      case serverSendFriendRequest(reqContext: RequestContext, senderUserID: String, receiverUserID: String) =>
        println("friend Request case")
        //var receiverUserID = userIDMap(receiverID)
        //var senderUserID = userIDMap(senderID)

        if (receiverUserID.equals(senderUserID)) {
          println("If")
          reqContext.complete("User request sent to self")
        } else if (userDB.get(receiverUserID).get.pendingRequests.contains(senderUserID)) {
          println("else if")
          reqContext.complete(receiverUserID + " already has a pending friend request from " + senderUserID)
        } else {
          println("else")
          userDB.get(receiverUserID).get.pendingRequests += (senderUserID -> senderUserID)
          println("Pending friend list of " + senderUserID + " = " + userDB.get(receiverUserID).get.pendingRequests)
          reqContext.complete("Friend Request from " + senderUserID + " from " + receiverUserID + " sent successfully\n" +
            "Pending friend List of " + receiverUserID + " now is " + userDB.get(receiverUserID).get.pendingRequests)
        }

      case serverManageFriendRequest(reqContext: RequestContext, senderUserID: String, receiverUserID: String, action: String) =>

        //var receiverUserID = userIDMap(receiverID)
        //var senderUserID = userIDMap(senderID)

        println("processing friend request of " + senderUserID + " by user " + receiverUserID)
        println("pendingList of " + receiverUserID + " before processing of the response= " + userDB.get(receiverUserID).get.pendingRequests)
        var pendingList = userDB.get(receiverUserID).get.pendingRequests
        if (pendingList.contains(senderUserID)) {
          if (action.equalsIgnoreCase("Accept")) {
            userDB.get(receiverUserID).get.friends += (pendingList.get(senderUserID).get -> pendingList.get(senderUserID).get)
            userDB.get(receiverUserID).get.pendingRequests -= (senderUserID)
            reqContext.complete(receiverUserID + "accepted the friend request of " + senderUserID)
            println("Friend List of user " + receiverUserID + "= " + userDB(receiverUserID).friends)
            println("Pending Friend List of user " + receiverUserID + "= " + userDB.get(receiverUserID).get.pendingRequests)
          } else if (action.equalsIgnoreCase("Reject")) {
            userDB.get(receiverUserID).get.pendingRequests.-=("ofUser")
            reqContext.complete(receiverUserID + " rejected the friend request of " + receiverUserID)
          }
        } else {
          if (userDB.get(receiverUserID).get.friends.contains(senderUserID)) {
            reqContext.complete(senderUserID + " is already a friend of " + receiverUserID)
          } else {
            reqContext.complete(senderUserID + " is already a friend of " + receiverUserID)
          }
        }
    }
  }
  /*trait commentTrait extends HttpService {
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


