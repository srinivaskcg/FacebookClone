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
import java.security.SecureRandom

object Common {

  case class caseUser(userID: String, creationDate: String, firstName: String, lastName: String, dateOfBirth: String, email: String)

  case class caseProfile(profileID: BigInt, friends: Map[Int, BigInt], friendRequests: Map[Int, BigInt], posts: Map[Int, BigInt])

  case class casePost(postID: BigInt, createdBy: BigInt, creationDate: String, content: String, location: String)

  case class casePage(pageID: BigInt, createdBy: BigInt, creationDate: String, name: String, description: String, likesList: Map[Int, BigInt], posts: Map[Int, BigInt])

  case class caseComment(commentID: BigInt, createdBy: BigInt, creationDate: String, content: String, likesCount: BigInt, likesList: Map[Int, BigInt])

  object caseUser extends DefaultJsonProtocol {
    implicit val implicitPerson = jsonFormat6(caseUser.apply)
  }

  object caseProfile extends DefaultJsonProtocol {
    implicit val implicitProfile = jsonFormat4(caseProfile.apply)
  }

  object casePost extends DefaultJsonProtocol {
    implicit val implicitPost = jsonFormat5(casePost.apply)
  }

  object casePage extends DefaultJsonProtocol {
    implicit val implicitPage = jsonFormat7(casePage.apply)
  }

  object caseComment extends DefaultJsonProtocol {
    implicit val implicitComment = jsonFormat6(caseComment.apply)
  }

  // User Case Classes
  case class createUser(newCaseUser: caseUser) //done
  case class updateUser(userID: Long, newCaseUser: caseUser)
  case class deleteUser(userID: Long)

  case class getUserInfo(userID: String) //done
  case class sendFriendRequest(toUserID: String) //done
  case class manageFriendRequest(fromUserID: String, action: String) //done
  case class unFriend(fromUserID: String, toUserID: String)

  // Post Case Classes  
  case class getUserPosts(ofUser: String)
  case class postOnWall(toUser: String, post: String, timeOfPost: String)
  case class postOnOwnWall(toUser: String, post: String, timeOfPost: String)

  case class updatePost(toUser: String, post: String, timeOfPost: String)
  case class deletePost(toUser: String, post: String, timeOfPost: String)

  case class likePost(toUser: String, post: String, timeOfPost: String)
  case class commentOnPost(ofUser: String, postId: Int, comment: String)

  case class getPostComments(ofUser: String)
  case class likeComment(ofUser: String, postId: Int, comment: String)

  // Page Case Classes
  case class createPage(pageId: String, caseNewPage: casePage)
  case class likePage(pageId: String, caseNewPage: casePage)
  case class unLikePage(pageId: String, caseNewPage: casePage)
  case class deletePage(pageId: String, caseNewPage: casePage)

  case class postOnPage(pageId: Int, casePostOnPage: casePost)
  case class commentOnPagePost(pageId: Int, nodeId: Int, nodeType: String, caseCommentOnPage: caseComment)

  case class serverGetUserInfo(reqContext : RequestContext, ofUser : String)
  case class serverSendFriendRequest(reqContext: RequestContext, byUser: String, toUser: String)
  case class serverManageFriendRequest(reqContext: RequestContext, byUser: String, toUser: String, action: String)

  // Generating a random BigInt and not present in the map
  implicit val randomIDGenerator = new SecureRandom()

  def getUniqueRandomBigInt(map: Map[BigInt, _]): BigInt = {
    def isUnique(x: BigInt) = !map.contains(x)

    val x = BigInt(256, randomIDGenerator)
    if (isUnique(x)) x
    else getUniqueRandomBigInt(map)
  }

}