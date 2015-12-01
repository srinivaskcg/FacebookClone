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

  class Server extends Actor with userTrait with postTrait with commentTrait with pageTrait {

    def actorRefFactory = context
    implicit val routerSettings = RoutingSettings.default(context)

    def receive = runRoute(receivePathUser
      ~ receivePathPost
      ~ receivePathPage
      ~ receivePathComment)
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
          requestContext =>
            val userActor = serverActorSystem.actorOf(Props(new UserActor()))
            userActor ! serverGetUserInfo(requestContext, ofUser)
            userActor ! PoisonPill
        }
      } ~ post {
        path(Segment / Segment / "sendRequest") { (sender, receiver) =>
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
      }
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

      case serverGetUserInfo(reqContext: RequestContext, tempUserID: String) =>
        
        var returnCU = new caseUser(userDB(tempUserID).userID, userDB(tempUserID).creationDate,
          userDB(tempUserID).firstName, userDB(tempUserID).lastName, userDB(tempUserID).gender,
          userDB(tempUserID).dateOfBirth, userDB(tempUserID).email)
        
        var returnCaseUser = returnCU.toJson
        reqContext.complete(returnCaseUser.toString())

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
          userDB.get(receiver).get.friends += (pendingReq.get(sender).get -> pendingReq.get(sender).get)
          userDB.get(receiver).get.pendingRequests -= (sender)
          requestContext.complete("Request accepted")
        } else {
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
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverPostStatus(requestContext, sender, receiver, newCasePost)
            postActor ! PoisonPill
        }
      } ~ path(Segment / Segment / "postOnWall") { (sender, receiver) =>
        entity(as[casePost]) { newCasePost =>
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverPostOnWall(requestContext, sender, receiver, newCasePost)
            postActor ! PoisonPill
        }
      } ~ get {
        path(Segment / "posts") { ofUser =>
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverGetUserPosts(requestContext, ofUser)
            postActor ! PoisonPill
        } ~ path(Segment / "postIds") { ofUser =>
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverGetUserPostIds(requestContext, ofUser)
            postActor ! PoisonPill
        }
      }
    }
  }

  class PostActor() extends Actor {

    def receive = {

      case serverPostOnWall(requestContext: RequestContext, sender: String, receiver: String, newCasePost: casePost) =>

        var newPost: Post = new Post()

        newPost.postID = uniqueCurrentTimeMS()
        newPost.createdBy = sender
        newPost.createdTo = receiver
        newPost.creationDate = newCasePost.creationDate
        newPost.content = newCasePost.content
        newPost.location = newCasePost.location

        userDB(newPost.createdTo).posts += (newPost.postID -> newPost)
        requestContext.complete("Posted on wall")

      case serverPostStatus(requestContext: RequestContext, sender: String, receiver: String, newCasePost: casePost) =>

        var newPost: Post = new Post()

        newPost.postID = uniqueCurrentTimeMS()
        newPost.createdBy = sender
        newPost.creationDate = newCasePost.creationDate
        newPost.content = newCasePost.content
        newPost.location = newCasePost.location

        userDB(newPost.createdBy).status += (newPost.postID -> newPost)
        requestContext.complete("Created Status by " + sender)

      case serverGetUserPosts(requestContext: RequestContext, ofUser: String) =>
        import spray.httpx.SprayJsonSupport._
        var postMap: Map[Long, Post] = userDB(ofUser).posts
        var returnPostMap: Map[Long, casePost] = new HashMap[Long, casePost]

        for ((k, v) <- postMap) {
          var varCP = new casePost(v.createdTo, v.creationDate, v.content, v.location)
          returnPostMap += (k -> varCP)
        }
        requestContext.complete(returnPostMap.keys.toList)

      case serverGetUserPostIds(requestContext: RequestContext, ofUser: String) =>
        import spray.httpx.SprayJsonSupport._
        var postMap: Map[Long, Post] = userDB(ofUser).posts
        var returnPostMap: Map[Long, casePost] = new HashMap[Long, casePost]

        for ((k, v) <- postMap) {
          var varCP = new casePost(v.createdTo, v.creationDate, v.content, v.location)
          returnPostMap += (k -> varCP)
        }
        requestContext.complete(returnPostMap.keys.toList)
    }
  }

  trait pageTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathPage = {
      path("createPage") {
        entity(as[casePage]) { newCasePage =>
          requestContext =>
            val pageActor = serverActorSystem.actorOf(Props(new PageActor()))
            pageActor ! serverCreatePage(requestContext, newCasePage)
            pageActor ! PoisonPill
        }
      } ~ post {
        path(Segment / "pagePost") { pageId =>
          entity(as[casePost]) { newCasePost =>
            requestContext =>
              val pageActor = serverActorSystem.actorOf(Props(new PageActor()))
              pageActor ! serverCreatePagePost(requestContext, pageId, newCasePost)
              pageActor ! PoisonPill
          }
        } ~ path(Segment / Segment / "likePage") { (pageId, byUser) =>
          entity(as[casePost]) { newCasePost =>
            requestContext =>
              val pageActor = serverActorSystem.actorOf(Props(new PageActor()))
              pageActor ! serverLikepage(requestContext, pageId, byUser)
              pageActor ! PoisonPill
          }
        }
      }
    }
  }

  class PageActor() extends Actor {

    def receive = {

      case serverCreatePage(requestContext: RequestContext, newCasePage: casePage) =>
        var newPage: Page = new Page()

        newPage.pageID = newCasePage.name
        newPage.pageCreationDate = newCasePage.creationDate
        newPage.pageName = newCasePage.name
        newPage.description = newCasePage.description

        pageDB.+=(newPage.pageID -> newPage)
        requestContext.complete("Page " + newPage.pageName + " created")

      case serverCreatePagePost(requestContext: RequestContext, pageId: String, newCasePost: casePost) =>

        var newPost: Post = new Post()

        newPost.postID = uniqueCurrentTimeMS()
        newPost.createdTo = newCasePost.sentTo
        newPost.creationDate = newCasePost.creationDate
        newPost.content = newCasePost.content
        newPost.location = newCasePost.location

        pageDB.get(pageId).get.posts += (newPost.postID -> newPost)

        requestContext.complete("Created Post on Page : " + pageId)

      case serverLikepage(requestContext: RequestContext, pageId: String, byUser: String) =>
        pageDB.get(pageId).get.likes += (byUser)
        requestContext.complete(byUser + " liked the page " + pageId)
    }
  }

  trait commentTrait extends HttpService {
    import spray.httpx.SprayJsonSupport._

    val receivePathComment = {
      path(Segment / Segment / "commentOnPost") { (commentBy, commentOn) =>
        entity(as[caseComment]) { newCaseComment =>
          requestContext =>
            val commentActor = serverActorSystem.actorOf(Props(new CommentActor()))
            commentActor ! serverCommentOnPost(requestContext, newCaseComment)
            commentActor ! PoisonPill
        }
      }
    }
  }

  class CommentActor() extends Actor {

    def receive = {

      case serverCommentOnPost(reqContext: RequestContext, newCaseComment: caseComment) =>

        var newComment: Comment = new Comment()

        newComment.commentID = uniqueCurrentTimeMS()
        newComment.postID = newCaseComment.userPageID
        newComment.content = newCaseComment.content
        newComment.creationDate = newCaseComment.creationDate
        newComment.createdBy = newCaseComment.createdOn

        userDB(newComment.createdBy).posts(newComment.postID).comments += (newComment.commentID -> newComment)
        reqContext.complete("Comment posted on " + newComment.postID)
    }
  }

}


