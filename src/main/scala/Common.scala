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


object Common {

  case class caseUser(userID: Option[BigInt], creationDate: Option[String], firstName: String, lastName: String, dateOfBirth: Option[String], 
      friends: Option[Map[Int, BigInt]], friendRequests: Option[Map[Int, BigInt]], posts: Option[Map[Int, BigInt]])

  case class casePost(postID: BigInt, createdBy: BigInt, creationDate: String, content: String, location: String)

  case class casePage(pageID: BigInt, createdBy: BigInt, creationDate: String, name: String, description: String, likesList: Map[Int, BigInt],
    posts: Map[Int, BigInt])

  case class caseComment(commentID: BigInt, createdBy: BigInt, creationDate: String, content: String, likesCount: BigInt, likesList: Map[Int, BigInt])

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

  case class registerNewUser(reqContext: RequestContext, newCaseUser: caseUser)
  case class getUserInfo(reqContext: RequestContext, ofUser: BigInt)
  
}