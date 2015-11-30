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
import org.json4s.jackson.Serialization.{read,write}


import scala.util.{ Success, Failure }
import scala.concurrent.Future

import org.json4s._

import Nodes._
import Common._
import spray.json._

object Client {

  class User extends Actor {
    implicit val system = ActorSystem()
    import system.dispatcher
    implicit val formats = Serialization.formats(NoTypeHints)

    var userName: String = new String()
    var url: String = "http://localhost:8082/"

    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    def receive = {

      case createUser(newUser: caseUser) =>
        val reqUrl = url + "registerUser"
        println(reqUrl)
        //println("In client"+newUser)
        val responseFuture: Future[HttpResponse] = pipeline(Post(reqUrl, newUser))
        responseFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("User registration failed")
            println(f.getMessage)
          }
        }

      /*  case updateUser(userID: Long, newUser: caseUser) =>
        val reqUrl = "url" + "registerUser"
        val responseFuture: Future[HttpResponse] = pipeline(Post(reqUrl, newUser))
        responseFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("User registration failed")
            println(f.getMessage)
          }
        }

      case deleteUser(userID: Long) =>
        val reqUrl = "url" + "registerUser"
        val responseFuture: Future[HttpResponse] = pipeline(Post(reqUrl))
        responseFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("User registration failed")
            println(f.getMessage)
          }
        }*/

      case getUserInfo(userId: String) =>
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
        }

      case sendFriendRequest(fromUserID: String, toUserID: String) => {

        val reqUrl = url + fromUserID + "/" + toUserID + "/friendRequest"

        val responseFuture: Future[HttpResponse] = pipeline(Post(reqUrl))
        responseFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println("\n " + httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Friend Request to " + toUserID + " from " + fromUserID + " failed with error message")
            println(f.getMessage)
          }
        }
      }

      case manageFriendRequest(fromUserID: String, toUserID: String, action: String) => {

        val reqUrl = url + toUserID + "/" + fromUserID + "/handleRequest"
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
        val reqUrl = url + "postStatus"

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
            println("Status update by " + newStatus.createdBy + " failed with error message")
            println(f.getMessage)
          }
        }
      }

      case getUserPosts(userID: String) => {
        val getUserPostsFuture: Future[HttpResponse] = pipeline(Get(url + userID + "/posts"))
        getUserPostsFuture.onComplete {
          case Success(httpResponse) => {
            println(httpResponse)
            println(httpResponse.status)
            if (httpResponse.status.isSuccess) {
              println("1" + httpResponse.entity.asString)
              val data = parse(httpResponse.entity.asString).extract[List[Long]]
              println(data.getClass)
            } else {
              println("2" + httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to retrieve posts for user: " + userID)
            println(f.getMessage)
          }
        }
      }

      case postOnWall(newStatus: casePost) => {
        val reqUrl = url + "postOnWall"

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
            println("post On " + newStatus.createdBy + " wall by " + newStatus.createdTo + "failed with error message")
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

      case commentOnPost(newCaseComment: caseComment) => {
        
        val reqUrl = url + "commentOnPost"
        
        val postCommentFuture: Future[HttpResponse] = pipeline(Post(reqUrl,newCaseComment))
        postCommentFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString)
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
            println("Post of " + userName + " on the page failed with error message")
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


