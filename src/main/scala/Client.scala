import akka.actor._
import akka.actor.Actor
import akka.actor.ActorSystem

import spray.httpx.SprayJsonSupport
import spray.httpx.SprayJsonSupport._
import spray.json.AdditionalFormats
import spray.json.{ JsonFormat, DefaultJsonProtocol }
import spray.client.pipelining._
import spray.http._

import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{ read, write }

import scala.util.{ Success, Failure }
import scala.concurrent.Future
import scala.util.Random
import scala.collection.immutable.HashMap

import org.json4s._

import Nodes._
import Common._
import spray.json._

object Client {

  implicit val system = ActorSystem()
  import system.dispatcher
  implicit val formats = Serialization.formats(NoTypeHints)

  var url: String = "http://localhost:8082"

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  class User extends Actor {

    var user: String = new String()

    def receive = {

      case registerUser(newUser: caseUser) =>
        val reqUrl = url + "/registerUser"
        val registerUserFuture: Future[HttpResponse] = pipeline(Post(reqUrl, newUser))
        registerUserFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString + " registered successfully !!!")
              user = newUser.createdBy
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(fail) => {
            println("Registration failed for user: " + newUser.createdBy)
            println(fail.getMessage)
          }
        }

      case getUserInfo(userId: String) =>
        val reqUrl = url + "/" + userId + "/userInfo"
        val responseFuture: Future[HttpResponse] = pipeline(Get(reqUrl))
        responseFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to fetch user details")
            println(f.getMessage)
          }
        }

      case sendFriendRequest(toUser: String) => {

        val reqUrl = url + "/" + user + "/" + toUser + "/sendRequest"
        println(reqUrl)

        val sendRequestFuture: Future[HttpResponse] = pipeline(Post(reqUrl))
        sendRequestFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println("Friend request sent successfully to " + httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(fail) => {
            println("Friend request sent to " + toUser + " failed.")
            println(fail.getMessage)
          }
        }
      }

      case manageFriendRequest(ofUser: String, action: String) => {

        val reqUrl = url + "/" + user + "/" + ofUser + "/manageRequest"
        println(reqUrl)
        val manageRequestFuture: Future[HttpResponse] = pipeline(Post(reqUrl).withEntity(action))
        manageRequestFuture.onComplete {

          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println("Friend request from " + ofUser + " was accepted by " + user)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to accept friend request")
            println(f.getMessage)
          }
        }
      }

      //Post related functionality

      case postOnOwnWall(newStatus: casePost) => {
        val reqUrl = url + "/" + user + "/" + user + "/" + "postStatus"
        print(reqUrl)
        val postStatusFuture: Future[HttpResponse] = pipeline(Post(reqUrl, newStatus))
        postStatusFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Status update failed")
            println(f.getMessage)
          }
        }
      }

      case getUserPosts(ofUser: String) => {
        val getUserPostsFuture: Future[HttpResponse] = pipeline(Get(url + "/" + ofUser + "/posts"))
        getUserPostsFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              val data = parse(httpResponse.entity.asString).extract[List[Long]]
              print("Retreived user posts: " + data)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to retrieve posts for user: " + ofUser)
            println(f.getMessage)
          }
        }
      }

      case postOnWall(newStatus: casePost) => {
        val reqUrl = url + "/" + user + "/" + newStatus.sentTo + "/" + "postOnWall"
        print(reqUrl)
        val postOnWallFuture: Future[HttpResponse] = pipeline(Post(reqUrl, newStatus))
        postOnWallFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("post On " + user + " wall by " + user + "failed with error message")
            println(f.getMessage)
          }
        }
      }

      case commentOnPost(ofUser: String) => {
        val reqUrl = url + "/" + ofUser + "/postIds"
        println(reqUrl)
        val getUserPostsFuture: Future[HttpResponse] = pipeline(Get(reqUrl))
        getUserPostsFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              var postIds: List[Long] = parse(httpResponse.entity.asString).extract[List[Long]]

              if (!postIds.isEmpty) {
                var randPost = postIds(Random.nextInt(postIds.size))
                val commentOnPostFuture: Future[HttpResponse] = pipeline(Post(url + "/" + user + "/" + ofUser + "/commentOnPost", new caseComment(ofUser, Constants.today, randPost, "comment" + ofUser)))
                commentOnPostFuture.onComplete {
                  case Success(httpResponse) => {
                    if (httpResponse.status.isSuccess) {
                      println(httpResponse.entity.asString)
                    } else {
                      println(httpResponse.entity.asString)
                    }
                  }
                  case Failure(f) => {
                    println("Comment of " + ofUser + " failed")
                    println(f.getMessage)
                  }
                }
              }
            } else {
              println("Failed to retrieve posts for user: " + ofUser)
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to retrieve posts for user: " + ofUser)
            println(f.getMessage)
          }
        }
      }

      // Page related functionality

      case createPage(newPage: casePage) =>
        val reqUrl = url + "/createPage"
        println(reqUrl)
        val responseFuture: Future[HttpResponse] = pipeline(Post(reqUrl, newPage))
        responseFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Page creation failed")
            println(f.getMessage)
          }
        }

      case createPagePost(pageId: String, casePagePost: casePost) =>
        val reqUrl = url + "/" + pageId + "/pagePost"
        val pagePostFuture: Future[HttpResponse] = pipeline(Post(reqUrl, casePagePost))
        pagePostFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to post on Page")
            println(f.getMessage)
          }
        }

      case likePage(pageId: String, byUser: String) =>
        val reqUrl = url + "/" + pageId + "/" + byUser + "/likepage"
        println(reqUrl)
        val likePageFuture: Future[HttpResponse] = pipeline(Post(reqUrl))
        likePageFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to like the page")
            println(f.getMessage)
          }
        }

    }
  }
}


