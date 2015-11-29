import akka.actor._
import akka.actor.Actor
import akka.actor.ActorSystem

import spray.httpx.SprayJsonSupport
import spray.httpx.SprayJsonSupport._
import spray.json.AdditionalFormats
import spray.json.{ JsonFormat, DefaultJsonProtocol }
import spray.client.pipelining._
import spray.http._

import scala.util.{ Success, Failure }
import scala.concurrent.Future

import Nodes._
import Common._

object Client {

  class User extends Actor {
    implicit val system = ActorSystem()
    import system.dispatcher

    var userName: String = new String()
    var url: String = "http://localhost:8082/"

    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    def receive = {

      case createUser(newUser: caseUser) =>
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

      case updateUser(userID: Long, newUser: caseUser) =>
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
        }

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

      case sendFriendRequest(toUserID: String) => {

        val reqUrl = url + userName + "/" + toUserID + "/friendRequest"
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
            println("Friend Request to " + toUserID + " from " + userName + " failed with error message")
            println(f.getMessage)
          }
        }
      }

      case manageFriendRequest(fromUserID: String, action: String) => {

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
      }

      case unFriend(fromUserID: String, action: String) => {

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
      }

      //Post related functionality

      case getUserPosts(ofUser: String) => {
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

      case postOnWall(toUser: String, post: String, timeOfPost: String) => {
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

      case postOnOwnWall(toUser: String, post: String, timeOfPost: String) => {
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

      case updatePost(toUser: String, post: String, timeOfPost: String) => {
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
      }

      case commentOnPost(ofUser: String, postId: Int, comment: String) => {
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
      }

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
      }

      // Page related functionality

      case createPage(pageId: String, caseNewPage: casePage) =>
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

      case likePage(pageId: String, caseNewPage: casePage) =>
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

      case postOnPage(pageId: Int, casePostOnPage: casePost) =>
        val postOnPageFuture: Future[HttpResponse] = pipeline(Post(url + userName + "/" + pageId + "/postOnPage", casePostOnPage))
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
        }

    }
  }
}


