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
              println(newUser.firstName + " registered successfully !!!")
              println(newUser.createdBy)
              user = newUser.createdBy
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(fail) => {
            println("Registration failed for user with userId: " + newUser.createdBy)
            println(fail.getMessage)
          }
        }

      /*case getUserInfo(userId: String) =>
        val reqUrl = url + userId + "/userInfo"
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
            println("User fetch failed")
            println(f.getMessage)
          }
        }*/

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
            println(user + " failed to accept friend request from " + ofUser)
            println(f.getMessage)
          }
        }
      }

      /*  case unFriend(fromUserID: String, action: String) => {

        val reqUrl = url + userName + "/" + fromUserID + "/handleRequest"
        val responseFuture: Future[HttpResponse] = pipeline(Post(reqUrl).withEntity(action))
        responseFuture.onComplete {

          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to process Friend Request of " + fromUserID + " by " + userName + ". Failed with error message")
            println(f.getMessage)
          }
        }
      }*/

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
            println("Status update by " + user + " failed with error message")
            println(f.getMessage)
          }
        }
      }

      case getUserPosts(ofUser: String) => {
        val getUserPostsFuture: Future[HttpResponse] = pipeline(Get(url + "/" + ofUser + "/posts"))
        getUserPostsFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              val data = parse(httpResponse.entity.asString).extract[List[String]]
              print(data)
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
        val reqUrl = url + "/" + user + "/" + newStatus.sentTo + "/" +"postOnWall"

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

      /*  case updatePost(toUser: String, post: String, timeOfPost: String) => {
        val newPostToUser = new casePost(0, post, timeOfPost).toJson
        val sendPostToUserFuture: Future[HttpResponse] = pipeline(Post(url + userName + "/" + toUser + "/post", newPostToUser.asJsObject))
        sendPostToUserFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Post sent to user " + toUser + " by " + userName + " failed with error message")
            println(f.getMessage)
          }
        }
      }

      case deletePost(toUser: String, post: String, timeOfPost: String) => {
        val newPostToUser = new casePost(0, post, timeOfPost).toJson
        val sendPostToUserFuture: Future[HttpResponse] = pipeline(Post(url + userName + "/" + toUser + "/post", newPostToUser.asJsObject))
        sendPostToUserFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Post sent to user " + toUser + " by " + userName + " failed with error message")
            println(f.getMessage)
          }
        }
      }

      case likePost(toUser: String, post: String, timeOfPost: String) => {
        val newPostToUser = new casePost(0, post, timeOfPost).toJson
        val sendPostToUserFuture: Future[HttpResponse] = pipeline(Post(url + userName + "/" + toUser + "/post", newPostToUser.asJsObject))
        sendPostToUserFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Post sent to user " + toUser + " by " + userName + " failed with error message")
            println(f.getMessage)
          }
        }
      }*/

      case commentOnPost(onUser: String, newCaseComment: caseComment) => {

        val getUserPostsFuture: Future[HttpResponse] = pipeline(Get(url + onUser + "/posts"))

        getUserPostsFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
              val data = parse(httpResponse.entity.asString).extract[List[Long]]

              if (data.length > 0) {
                val random = new Random
                val randPost = data(random.nextInt(data.length))

                val getPostFuture: Future[HttpResponse] = pipeline(Get(url + "/createComment/" + randPost, newCaseComment))
                getUserPostsFuture.onComplete {
                  case Success(httpResponse) => {
                    if (httpResponse.status.isSuccess) {
                      val commentId = parse(httpResponse.entity.asString).extract[String]
                      println("Comment with " + commentId + " created")
                    }
                  }
                  case Failure(f) => {
                    println("Comment failed with error message")
                    println(f.getMessage)
                  }
                }
              }
            } else {
              println(httpResponse.entity.asString)
            }

          }
          case Failure(f) => {
            println("Comment failed with error message")
            println(f.getMessage)
          }
        }
      }

      /*
      case getPostComments(ofUser: String) => {
        val getPostsOfUserFuture: Future[HttpResponse] = pipeline(Get(url + userName + "/" + ofUser + "/posts"))
        getPostsOfUserFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println("\n " + httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Retrieval of posts of user " + ofUser + " as requested by " + userName + " failed with error message")
            println(f.getMessage)
          }
        }
      }

      case likeComment(ofUser: String, postId: Int, comment: String) => {
        val commentOnPostFuture: Future[HttpResponse] = pipeline(Post(url + userName + "/" + ofUser + "/commentOnPost", new caseComment(postId, 0, comment)))
        commentOnPostFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Comment sent to user " + ofUser + "'s post by " + userName + " failed with error message")
            println(f.getMessage)
          }
        }
      }*/

      // Page related functionality

      case createPage(newPage: casePage) =>
        val reqUrl = url + "createPage"
        println(reqUrl)
        //println("In client"+newUser)
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

        val reqUrl = url + pageId + "/pagePost"
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
            println("Post of on the page failed with error message")
            println(f.getMessage)
          }
        }

      /*case likePage(pageId: String, caseNewPage: casePage) =>
        val createNewPageFuture: Future[HttpResponse] = pipeline(Post(url + userName + "/" + pageId, caseNewPage))
        createNewPageFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Create new Page " + pageId + " failed with error message")
            println(f.getMessage)
          }
        }

      case unLikePage(pageId: String, caseNewPage: casePage) =>
        val createNewPageFuture: Future[HttpResponse] = pipeline(Post(url + userName + "/" + pageId, caseNewPage))
        createNewPageFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Create new Page " + pageId + " failed with error message")
            println(f.getMessage)
          }
        }

      case deletePage(pageId: String, caseNewPage: casePage) =>
        val createNewPageFuture: Future[HttpResponse] = pipeline(Post(url + userName + "/" + pageId, caseNewPage))
        createNewPageFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Create new Page " + pageId + " failed with error message")
            println(f.getMessage)
          }
        }

      case commentOnPagePost(pageId: Int, nodeId: Int, nodeType: String, caseCommentOnPage: caseComment) =>
        val postOnPageFuture: Future[HttpResponse] = pipeline(Post(url + userName + "/" + pageId + "/" + nodeId + "/" + nodeType, caseCommentOnPage))
        postOnPageFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Post of " + userName + " on the page failed with error message")
            println(f.getMessage)
          }
        }*/

    }
  }
}


