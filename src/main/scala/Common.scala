import akka.actor.{ ActorSystem, Actor, Props, ActorRef }
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

import Simulation._

object Common {

  case class caseUser(createdBy: String, creationDate: String, firstName: String, lastName: String, gender: String, dateOfBirth: String, email: String)

  // case class caseProfile(profileID: BigInt, friends: Map[Int, BigInt], friendRequests: Map[Int, BigInt], posts: Map[Int, BigInt])

  case class casePost(sentTo: String, creationDate: String, content: String, location: String)

  case class casePage(creationDate: String, name: String, description: String)

  case class caseComment(createdOn: String, creationDate: String, userPageID: Long, content: String)

  object caseUser extends DefaultJsonProtocol {
    implicit val implicitPerson = jsonFormat7(caseUser.apply)
  }

  /*object caseProfile extends DefaultJsonProtocol {
    implicit val implicitProfile = jsonFormat4(caseProfile.apply)
  }*/

  object casePost extends DefaultJsonProtocol {
    implicit val implicitPost = jsonFormat4(casePost.apply)
  }

  object casePage extends DefaultJsonProtocol {
    implicit val implicitPage = jsonFormat3(casePage.apply)
  }

  object caseComment extends DefaultJsonProtocol {
    implicit val implicitComment = jsonFormat4(caseComment.apply)
  }

  // User Case Classes
  case class registerUser(newCaseUser: caseUser) //done
  case class getUserInfo(userID: String) //done

  case class sendFriendRequest(toUser: String)
  case class manageFriendRequest(ofUser: String, action: String)

  // Post Case Classes  
  case class postOnWall(newCasePost: casePost) //done
  case class postOnOwnWall(newCasePost: casePost) //done
  case class commentOnPost(ofUser: String) //
  case class getUserPosts(ofUser: String) // done

  // Page Case Classes
  case class createPage(caseNewPage: casePage) //done
  case class createPagePost(pageId: String, casePagePost: casePost) //done
  case class commentOnPagePost(pageId: Int, nodeId: Int, nodeType: String, caseCommentOnPage: caseComment)
  case class likePage(pageId: String, byUser: String) //if time permits

  case class serverRegisterUser(requestContext: RequestContext, newUserInfo: caseUser)
  case class serverGetUserInfo(reqContext: RequestContext, ofUser: String)
  case class serverSendFriendRequest(reqContext: RequestContext, byUser: String, toUser: String)
  case class serverManageFriendRequest(reqContext: RequestContext, byUser: String, toUser: String, action: String)

  case class serverPostOnWall(requestContext: RequestContext, sender: String, receiver: String, newCasePost: casePost)
  case class serverPostStatus(requestContext: RequestContext, sender: String, receiver: String, newCasePost: casePost)
  case class serverGetUserPosts(reqContext: RequestContext, ofUser: String)
  case class serverGetUserPostIds(requestContext: RequestContext, ofUser: String)
  case class serverPagePost(reqContext: RequestContext, pageId: String, newCasePost: casePost)
  case class serverCommentOnPost(reqContext: RequestContext, newCaseComment: caseComment) //

  case class serverCreatePagePost(requestContext: RequestContext, pageId: String, newCasePost: casePost)
  case class serverCreatePage(requestContext: RequestContext, newCasePage: casePage)
  case class serverLikepage(requestContext: RequestContext, pageId: String, byUser: String)

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