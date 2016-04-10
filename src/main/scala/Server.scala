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
import scala.collection.mutable.MutableList
import java.util.Base64._
import java.security._

object Project4 extends App {

  implicit val serverActorSystem = ActorSystem()

  val receiverActor = serverActorSystem.actorOf(Props[Server], "receiverRequests")

  IO(Http) ! Http.Bind(receiverActor, interface = "localhost", port = 8082)

  var userDB: Map[String, User] = new HashMap[String, User]()
  var userKeyStore: Map[String, String] = new HashMap[String, String]()
  var userRandomStrStore: Map[String, String] = new HashMap[String, String]()
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
    import caseUser._
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
      }~ get {
        path(Segment / "authorize") { (ofUser) =>
          requestContext =>
            val userActor = serverActorSystem.actorOf(Props(new UserActor()))
            userActor ! serverAuthorize(requestContext, ofUser)
            userActor ! PoisonPill
        } ~
          path(Segment / "userList") { (ofUser) =>
            requestContext =>
              val userActor = serverActorSystem.actorOf(Props(new UserActor()))
              userActor ! serverGetUserList(requestContext, ofUser)
              userActor ! PoisonPill
          } ~
          path(Segment / "friendList") { (ofUser) =>
            requestContext =>
              val userActor = serverActorSystem.actorOf(Props(new UserActor()))
              userActor ! serverGetFriendList(requestContext, ofUser)
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
        newUser.storePublicKey = newUserInfo.storePublicKey

        userDB.+=(newUser.userID -> newUser)
        userKeyStore.+=(newUser.userID -> newUser.storePublicKey)
        //println(newUser.userID + "-----" + newUser.storePublicKey)

        requestContext.complete(newUser.userID)

      case serverGetUserInfo(reqContext: RequestContext, tempUserID: String) =>

        var returnUser = new caseUser(userDB(tempUserID).userID, userDB(tempUserID).creationDate,
          userDB(tempUserID).firstName, userDB(tempUserID).lastName, userDB(tempUserID).gender,
          userDB(tempUserID).dateOfBirth, userDB(tempUserID).email, userDB(tempUserID).storePublicKey)

        var returnCaseUser = returnUser.toJson
        reqContext.complete(returnCaseUser.toString())

      case serverGetUserList(reqContext: RequestContext, tempUserID: String) =>
        import spray.httpx.SprayJsonSupport._
        reqContext.complete(userKeyStore.values.toList)

      case serverAuthorize(reqContext: RequestContext, tempUserID: String) =>
        import spray.httpx.SprayJsonSupport._
        val random: SecureRandom = SecureRandom.getInstance("SHA1PRNG")
        val randomVal = BigInt(128, random).toString(32);
        userRandomStrStore += (tempUserID -> randomVal)
        reqContext.complete(RSA.encrypt(randomVal, userDB(tempUserID).storePublicKey))

      case serverGetFriendList(reqContext: RequestContext, tempUserID: String) =>
        import spray.httpx.SprayJsonSupport._
        var friends: Map[String, String] = userDB(tempUserID).friends
        var encKeys: Map[String, String] = new HashMap[String, String]
        for (s <- friends) {
          //println(userKeyStore(s._1))
          encKeys += (s._1 -> userKeyStore(s._1))
        }
        reqContext.complete(encKeys.keys.toList)

      case serverSendFriendRequest(requestContext: RequestContext, sender: String, receiver: String) =>
        if (userDB.get(receiver).get.pendingRequests.contains(sender)) {
          requestContext.complete(sender + " sent a friend request already to  " + receiver)
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
        path(Segment / Segment / "posts") { (ofUser, byUser) =>
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverGetUserPosts(requestContext, ofUser, byUser)
            postActor ! PoisonPill
        } ~ path(Segment / Segment / "status") { (ofUser, byUser) =>
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverGetUserStatus(requestContext, ofUser, byUser)
            postActor ! PoisonPill
        } ~ path(Segment / "postIds") { ofUser =>
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverGetUserPostIds(requestContext, ofUser)
            postActor ! PoisonPill
        } ~ path(Segment / "statusIds") { ofUser =>
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverGetUserStatusIds(requestContext, ofUser)
            postActor ! PoisonPill
        } ~ path(Segment / Segment / Segment / "postContent") { (ofUser, byUser, postId) =>
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverGetPostContent(requestContext, ofUser, byUser, postId)
            postActor ! PoisonPill
        } ~ path(Segment / Segment / Segment / "statusContent") { (ofUser, byUser, postId) =>
          requestContext =>
            val postActor = serverActorSystem.actorOf(Props(new PostActor()))
            postActor ! serverGetStatusContent(requestContext, ofUser, byUser, postId)
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

        //print(RSA.decrypt(newCasePost.encStr, RSA.decodePublicKey(userDB(sender).storePublicKey)))

        if(userRandomStrStore(sender) == newCasePost.encStr){
        //if (sender == RSA.decrypt(newCasePost.encStr, RSA.decodePublicKey(userDB(sender).storePublicKey))) {
          userDB(newPost.createdTo).posts += (newPost.postID -> newPost)
          //requestContext.complete(sender + " posted on " + receiver + "'s wall !!!")
        } else
          newPost.content = "Access denied to Post"
        requestContext.complete(newPost.content)

      case serverPostStatus(requestContext: RequestContext, sender: String, receiver: String, newCasePost: casePost) =>

        var newPost: Post = new Post()

        newPost.postID = uniqueCurrentTimeMS()
        newPost.createdBy = sender
        newPost.creationDate = newCasePost.creationDate
        newPost.content = newCasePost.content
        //println("Content:" + newPost.content)
        newPost.location = newCasePost.location
        newPost.canSeeMap = newCasePost.shareWith

        //newPost.canSeeMap.foreach(p => println(">>> key=" + p._1 + ", value=" + p._2))

        //print(RSA.decrypt(newCasePost.encStr, RSA.decodePublicKey(userDB(sender).storePublicKey)))

        if(userRandomStrStore(sender) == newCasePost.encStr){
        //if (sender == RSA.decrypt(newCasePost.encStr, RSA.decodePublicKey(userDB(sender).storePublicKey))) {
          userDB(newPost.createdBy).status += (newPost.postID -> newPost)
          //print("\n" + newPost.postID + "---" + newCasePost.content)
        } else
          newPost.content = "Access denied to Post"
        //requestContext.complete("Status posted by " + sender + " !!!")
        requestContext.complete(newPost.content)

      case serverGetUserPosts(requestContext: RequestContext, ofUser: String, byUser: String) =>
        import spray.httpx.SprayJsonSupport._
        //print(ofUser)
        var postMap: Map[Long, Post] = userDB(ofUser).posts
        //println("\nSize::"+ postMap.size)
        var returnPostMap: Map[Long, casePost] = new HashMap[Long, casePost]

        for ((k, v) <- postMap) {
          var varCP = new casePost(v.createdTo, v.creationDate, v.content, v.location, v.canSeeMap, "")
          returnPostMap += (k -> varCP)
        }
        // if (returnPostMap.size > 0)
        requestContext.complete(returnPostMap)

      case serverGetUserStatus(requestContext: RequestContext, ofUser: String, byUser: String) =>
        import spray.httpx.SprayJsonSupport._
        //print(ofUser)
        var postMap: Map[Long, Post] = userDB(ofUser).status
        //println("\nSize::"+ postMap.size)
        var returnPostMap: Map[Long, casePost] = new HashMap[Long, casePost]

        for ((k, v) <- postMap) {
          var varCP = new casePost(v.createdTo, v.creationDate, v.content, v.location, v.canSeeMap, "")
          returnPostMap += (k -> varCP)
        }
        // if (returnPostMap.size > 0)
        requestContext.complete(returnPostMap)

      case serverGetUserPostIds(requestContext: RequestContext, ofUser: String) =>
        import spray.httpx.SprayJsonSupport._
        var postMap: Map[Long, Post] = userDB(ofUser).posts
        var returnPostMap: Map[Long, casePost] = new HashMap[Long, casePost]

        for ((k, v) <- postMap) {
          var varCP = new casePost(v.createdTo, v.creationDate, v.content, v.location, v.canSeeMap, "")
          returnPostMap += (k -> varCP)
        }
        requestContext.complete(returnPostMap.keys.toList)

      case serverGetUserStatusIds(requestContext: RequestContext, ofUser: String) =>
        import spray.httpx.SprayJsonSupport._
        var postMap: Map[Long, Post] = userDB(ofUser).status
        var returnPostMap: Map[Long, casePost] = new HashMap[Long, casePost]

        for ((k, v) <- postMap) {
          var varCP = new casePost(v.createdTo, v.creationDate, v.content, v.location, v.canSeeMap, "")
          returnPostMap += (k -> varCP)
        }
        requestContext.complete(returnPostMap.keys.toList)

      case serverGetPostContent(requestContext: RequestContext, ofUser: String, byUser: String, postId: String) =>
        import spray.httpx.SprayJsonSupport._
        var content: String = new String()
        var canSeeMap: Map[String, String] = userDB(ofUser).posts(postId.toLong).canSeeMap
        canSeeMap.foreach(p => println("<<< key=" + p._1 + ", value=" + p._2))
        //print("post" + ofUser + " !!! " + byUser)
        if (canSeeMap.keySet.exists(_ == byUser)) {
          content = userDB(ofUser).posts(postId.toLong).content + "," + canSeeMap(byUser)
        } else {
          content = "Access Denied"
          //print("Access Denied")
        }
        requestContext.complete(content)

      case serverGetStatusContent(requestContext: RequestContext, ofUser: String, byUser: String, postId: String) =>
        import spray.httpx.SprayJsonSupport._
        var content: String = new String()
        var canSeeMap: Map[String, String] = userDB(ofUser).status(postId.toLong).canSeeMap
        //canSeeMap.foreach(p => println("<<< key=" + p._1 + ", value=" + p._2))
        //print("status-" + ofUser + " !!! " + byUser)
        if (canSeeMap.keySet.exists(_ == byUser)) {
          content = userDB(ofUser).status(postId.toLong).content + "," + canSeeMap(byUser)
        } else {
          content = "Access Denied"
          //print("Access Denied")
        }
        requestContext.complete(content)
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

      case serverCommentOnPost(requestContext: RequestContext, newCaseComment: caseComment) =>

        var newComment: Comment = new Comment()

        newComment.commentID = uniqueCurrentTimeMS()
        newComment.postID = newCaseComment.userPageID
        newComment.content = newCaseComment.content
        newComment.creationDate = newCaseComment.creationDate
        newComment.createdBy = newCaseComment.createdOn

        userDB(newComment.createdBy).posts(newComment.postID).comments += (newComment.commentID -> newComment)
        requestContext.complete("Comment posted on " + newComment.postID)
    }
  }

}