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
import java.util.concurrent.atomic.AtomicLong

object Common {

  case class caseUser(createdBy: String, creationDate: String, firstName: String, lastName: String, dateOfBirth: String, email: String)

  // case class caseProfile(profileID: BigInt, friends: Map[Int, BigInt], friendRequests: Map[Int, BigInt], posts: Map[Int, BigInt])

  case class casePost(createdBy: String, createdTo: String, creationDate: String, content: String, location: String)

  case class casePage(createdBy: String, creationDate: String, name: String, description: String)

  case class caseComment(commentID: BigInt, createdBy: BigInt, creationDate: String, content: String, likesCount: BigInt, likesList: Map[Int, BigInt])

  object caseUser extends DefaultJsonProtocol {
    implicit val implicitPerson = jsonFormat6(caseUser.apply)
  }

  /*object caseProfile extends DefaultJsonProtocol {
    implicit val implicitProfile = jsonFormat4(caseProfile.apply)
  }*/

  object casePost extends DefaultJsonProtocol {
    implicit val implicitPost = jsonFormat5(casePost.apply)
  }

  object casePage extends DefaultJsonProtocol {
    implicit val implicitPage = jsonFormat4(casePage.apply)
  }

  object caseComment extends DefaultJsonProtocol {
    implicit val implicitComment = jsonFormat6(caseComment.apply)
  }

  // User Case Classes
  case class createUser(newCaseUser: caseUser) //done
  case class getUserInfo(userID: String) //done
  
  case class sendFriendRequest(fromUserID: String, toUserID: String) //done
  case class manageFriendRequest(fromUserID: String, toUserID: String, action: String) //done

  // Post Case Classes  
  case class postOnWall(newCasePost: casePost) //done
  case class postOnOwnWall(newCasePost: casePost) //done
  case class commentOnPost(ofUser: String, postId: Int, comment: String) //
  
  case class getUserPosts(ofUser: String) // error

  // Page Case Classes
  case class createPage(caseNewPage: casePage) //done

  case class createPagePost(pageId: String, casePagePost: casePost)//done
  case class commentOnPagePost(pageId: Int, nodeId: Int, nodeType: String, caseCommentOnPage: caseComment)

  // If time permits
  case class updateUser(userID: Long, newCaseUser: caseUser) //if time permits
  case class deleteUser(userID: Long) //if time permits

  case class unFriend(fromUserID: String, toUserID: String) //if time permits
  case class updatePost(toUser: String, post: String, timeOfPost: String) //if time permits
  case class deletePost(toUser: String, post: String, timeOfPost: String) //if time permits

  case class likePost(toUser: String, post: String, timeOfPost: String) //if time permits
  
  case class getPostComments(ofUser: String) //if time permits
  case class likeComment(ofUser: String, postId: Int, comment: String) //if time permits

  case class likePage(pageId: String, caseNewPage: casePage) //if time permits
  case class unLikePage(pageId: String, caseNewPage: casePage) //if time permits
  case class deletePage(pageId: String, caseNewPage: casePage) //if time permits

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  case class serverGetUserInfo(reqContext: RequestContext, ofUser: String)
  case class serverSendFriendRequest(reqContext: RequestContext, byUser: String, toUser: String)
  case class serverManageFriendRequest(reqContext: RequestContext, byUser: String, toUser: String, action: String)

  case class serverGetUserPosts(reqContext: RequestContext, ofUser: String)
  case class serverPagePost(reqContext: RequestContext, pageId: String, newCasePost: casePost)

  // Generating a random BigInt and not present in the map
  implicit val randomIDGenerator = new SecureRandom()

  def getUniqueRandomBigInt(map: Map[BigInt, _]): BigInt = {
    def isUnique(x: BigInt) = !map.contains(x)

    val x = BigInt(256, randomIDGenerator)
    if (isUnique(x)) x
    else getUniqueRandomBigInt(map)
  }

  var LAST_TIME_MS: AtomicLong = new AtomicLong();

  def uniqueCurrentTimeMS(): Long = {
    var now: Long = System.currentTimeMillis();

    var lastTime: Long = LAST_TIME_MS.get();
    if (lastTime >= now)
      now = lastTime + 1;
    if (LAST_TIME_MS.compareAndSet(lastTime, now)) now
    else uniqueCurrentTimeMS()
  }

}