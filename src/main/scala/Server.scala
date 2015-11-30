import akka.actor.{ ActorSystem, Actor, Props }
import akka.actor._
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
 
  class Server extends Actor with userTrait with postTrait //with pageTrait with commentTrait 
  {

    def actorRefFactory = context
    implicit val routerSettings = RoutingSettings.default(context)

    def receive = runRoute(receivePathUser
      ~ receivePathPost
     // ~ receivePathPage 
     // ~ receivePathComment
      )
  }

  trait userTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathUser = {
      path("registerUser") {
        entity(as[caseUser]) { newUserInfo =>
          requestContext =>
            val userActor = serverActorSystem.actorOf(Props(new UserActor()))
            userActor ! serverRegisterUser(requestContext, newUserInfo)
            userActor ! PoisonPill
        }
      } ~ get {
        path(Segment / "userInfo") { (ofUser) =>
          println("User Info")
          requestContext =>
            val userActor = serverActorSystem.actorOf(Props(new UserActor()))
            userActor ! serverGetUserInfo(requestContext, ofUser)
            userActor ! PoisonPill
        }
      } ~ post {
        path(Segment / Segment / "sendRequest") { (sender, receiver) =>
          println("friend Request")
          requestContext =>
            val userActor = serverActorSystem.actorOf(Props(new UserActor()))
            userActor ! serverSendFriendRequest(requestContext, sender, receiver)
            userActor ! PoisonPill
        } ~
          path(Segment / Segment / "manageRequest") { (sender, receiver) =>
            entity(as[String]) { action =>
              requestContext =>
                val userActor = serverActorSystem.actorOf(Props(new UserActor()))
                userActor ! serverManageFriendRequest(requestContext, sender, receiver, action)
                userActor ! PoisonPill
            }
          }
      }/* ~ delete {
        path(IntNumber / "deleteUser") { ofUser =>
          val userActor = serverActorSystem.actorOf(Props(new UserActor()), "userActor")
          requestContext =>
            userActor ! deleteUser(ofUser)
            userActor ! PoisonPill
        }
      }*/
    }
  }

  class UserActor() extends Actor {

    def receive = {

      case serverRegisterUser(requestContext: RequestContext, newUserInfo: caseUser) =>

        var newUser: User = new User()
        
        newUser.userID = newUserInfo.createdBy
        newUser.creationDate = newUserInfo.creationDate
        newUser.firstName = newUserInfo.firstName
        newUser.lastName = newUserInfo.lastName
        newUser.gender = newUserInfo.gender
        newUser.dateOfBirth = newUserInfo.dateOfBirth
        newUser.email = newUserInfo.email
        
        userDB.+=(newUser.userID -> newUser)
        
        requestContext.complete(newUser.userID)

      /*case serverGetUserInfo(reqContext: RequestContext, tempUserID: String) =>
        var returnCU = new caseUser(userDB.get(tempUserID).get.userID, userDB.get(tempUserID).get.creationDate,
          userDB.get(tempUserID).get.firstName, userDB.get(tempUserID).get.lastName,
          userDB.get(tempUserID).get.dateOfBirth, userDB.get(tempUserID).get.email)
        var returnCaseUser = returnCU.toJson
        reqContext.complete(returnCaseUser.toString())*/

      case serverSendFriendRequest(requestContext: RequestContext, sender: String, receiver: String) =>
        if (userDB.get(receiver).get.pendingRequests.contains(sender)) {
          requestContext.complete(sender + " has already sent a friend request to  " + receiver)
        } else {
          userDB.get(receiver).get.pendingRequests += (sender -> sender)
          requestContext.complete(receiver)
        }

      case serverManageFriendRequest(requestContext: RequestContext, receiver: String, sender: String, action: String) =>

        var pendingReq = userDB.get(receiver).get.pendingRequests
        if (pendingReq.contains(sender)) {
          println("Inside if")
            userDB.get(receiver).get.friends += (pendingReq.get(sender).get -> pendingReq.get(sender).get)
            
            userDB.get(receiver).get.pendingRequests -= (sender)
            
            requestContext.complete("Request accepted")
            
        } else {
          println("Inside else")
          if (userDB.get(receiver).get.friends.contains(sender)) {
            requestContext.complete(sender + " is already a friend of " + receiver)
          }
        }
    }
  }

  trait postTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathPost = {
      path(Segment / Segment / "postStatus") { (sender, receiver) =>
        entity(as[casePost]) { newCasePost =>

          var newPost: Post = new Post()

          newPost.postID = uniqueCurrentTimeMS()
          newPost.createdBy = sender
          newPost.creationDate = newCasePost.creationDate
          newPost.content = newCasePost.content
          newPost.location = newCasePost.location

          userDB(newPost.createdBy).status += (newPost.postID -> newPost)
          println(newPost.postID)

          complete("Created Status")
        }
      } ~ path(Segment / Segment / "postOnWall") { (sender, receiver) => 
        entity(as[casePost]) { newCasePost =>

          var newPost: Post = new Post()

          newPost.postID = uniqueCurrentTimeMS()
          newPost.createdBy = sender
          newPost.createdTo = receiver
          newPost.creationDate = newCasePost.creationDate
          newPost.content = newCasePost.content
          newPost.location = newCasePost.location

          userDB(newPost.createdTo).posts += (newPost.postID -> newPost)
          println(newPost.postID)

          complete("Posted on wall")
        }
      } ~ get {
        path(Segment / "posts") { ofUser =>
          println("getting posts for" + ofUser)
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverGetUserPosts(requestContext, ofUser)
            postActor ! PoisonPill
        }
      }
    }
  }

  class PostActor() extends Actor {

    def receive = {

      case serverGetUserPosts(requestContext: RequestContext, ofUser: String) =>
        import spray.httpx.SprayJsonSupport._
        var postMap: Map[Long, Post] = userDB(ofUser).posts
        var returnPostMap: Map[Long, casePost] = new HashMap[Long, casePost]

        for ((k, v) <- postMap) {
          var varCP = new casePost(v.createdTo, v.creationDate, v.content, v.location)
          returnPostMap += (k -> varCP)
        }
        requestContext.complete(returnPostMap.values.toList)
    }
  }
/*
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
    }
  }

  trait commentTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathComment = {
      path("createComment" / LongNumber) { postId =>
        entity(as[caseComment]) { newCaseComment =>
          requestContext =>
            val commentActor = serverActorSystem.actorOf(Props(new CommentActor()))
            println("Before commenting")
            commentActor ! serverCommentOnPost(requestContext, postId, newCaseComment)
            commentActor ! PoisonPill
        }
      }
    }
     ~ get {
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

  class CommentActor() extends Actor {

    def receive = {

      case serverCommentOnPost(reqContext: RequestContext, postId: Long, newCaseComment: caseComment) =>
        println("serverCommentOnPost case")

        var newComment: Comment = new Comment()

        newComment.commentID = uniqueCurrentTimeMS()
        newComment.postID = postId
        newComment.content = newCaseComment.content
        newComment.creationDate = newCaseComment.creationDate
        newComment.createdBy = newCaseComment.createdBy

        userDB(newCaseComment.userPageID).posts(postId).comments += (newComment.commentID -> newComment)

        reqContext.complete(newComment.commentID.toString())
    }
  }*/
  
}


