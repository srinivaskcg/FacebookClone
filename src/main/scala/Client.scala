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

import java.security._
import javax.crypto.{ KeyGenerator, SecretKey }

object Client {

  implicit val system = ActorSystem()
  import system.dispatcher
  implicit val formats = Serialization.formats(NoTypeHints)

  var url: String = "http://localhost:8082"

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
  var userKeyDB: Map[String, PublicKey] = new HashMap[String, PublicKey]
  var userRandomKeyDB: Map[String, String] = new HashMap[String, String]

  class User extends Actor {

    var clientUser: String = new String()

    //AES Encryption
    //var aesKey: String = "Bar12345Bar12345";

    //RSA Encryption
    val keyPair = RSA.generateKey
    val publicKey = keyPair.getPublic
    val privateKey = keyPair.getPrivate

    //val symmKey = RSA.generateSymetricKey
    val initVector = "RandomInitVector"
    //val key = "Bar12345Bar12345";

    val random: SecureRandom = SecureRandom.getInstance("SHA1PRNG")
    val randomVal = random.nextInt().toString()

    val key = sha256Method(randomVal).substring(0, 16)

    //println(key)

    def sha256Method(input: String): String =
      {
        val md = MessageDigest.getInstance("SHA-256")
        val hexString = md.digest(input.getBytes("UTF-8")).map("%02x".format(_)).mkString
        return hexString
      }

    def receive = {

      case registerUser(userID: String, creationDate: String, firstName: String, lastName: String, gender: String, dateOfBirth: String, email: String) =>

        val newUser: caseUser = new caseUser(userID, creationDate, firstName, lastName, gender, dateOfBirth, email,
          RSA.encodePublicKey(publicKey))

        val reqUrl = url + "/registerUser"

        val registerUserFuture: Future[HttpResponse] = pipeline(Post(reqUrl, newUser))
        registerUserFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println(httpResponse.entity.asString + " registered successfully !!!")
              userKeyDB += (newUser.createdBy -> publicKey)
              clientUser = newUser.createdBy
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(fail) => {
            println("Registration failed for user: " + newUser.createdBy)
            println(fail.getMessage)
          }
        }

      case authorize(userId: String) =>
        val reqUrl = url + "/" + userId + "/authorize"
        val responseFuture: Future[HttpResponse] = pipeline(Get(reqUrl))
        responseFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              userRandomKeyDB += (userId -> httpResponse.entity.asString)
              println(httpResponse.entity.asString)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to authorize")
            println(f.getMessage)
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

        val reqUrl = url + "/" + clientUser + "/" + toUser + "/sendRequest"
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

        val reqUrl = url + "/" + clientUser + "/" + ofUser + "/manageRequest"
        println(reqUrl)
        val manageRequestFuture: Future[HttpResponse] = pipeline(Post(reqUrl).withEntity(action))
        manageRequestFuture.onComplete {

          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println("Friend request from " + ofUser + " was accepted by " + clientUser)
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

      case getFriendList(userId: String) =>
        val reqUrl = url + "/" + userId + "/friendList"
        val responseFuture: Future[HttpResponse] = pipeline(Get(reqUrl))
        responseFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              var friendIds: List[String] = parse(httpResponse.entity.asString).extract[List[String]]
              friendIds.foreach(println)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to fetch friend list")
            println(f.getMessage)
          }
        }

      case getUserList(userId: String) =>
        val reqUrl = url + "/" + clientUser + "/userList"
        val responseFuture: Future[HttpResponse] = pipeline(Get(reqUrl))
        responseFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              var userIds: List[String] = parse(httpResponse.entity.asString).extract[List[String]]
              userIds.foreach(println)
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Failed to fetch user list")
            println(f.getMessage)
          }
        }

      //Post related functionality
      case postOnOwnWall(sentTo: String, creationDate: String, content: String, location: String, shareWith: String) => {
        val reqUrl = url + "/" + clientUser + "/" + clientUser + "/" + "postStatus"
        val keysMap: Map[String, String] = getKeys(shareWith, key)
        val newStatus: casePost = new casePost(sentTo, creationDate, RSA.encryptAES(key, initVector, "Status" + clientUser), location, keysMap, RSA.decrypt(userRandomKeyDB(clientUser), privateKey))
        val postOnOwnWallFuture: Future[HttpResponse] = pipeline(Post(reqUrl, newStatus))
        postOnOwnWallFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println("\n" + clientUser + " posted status - " + RSA.decryptAES(key, initVector, httpResponse.entity.asString))
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("Status On " + clientUser + " wall by " + clientUser + "failed with error message")
            println(f.getMessage)
          }
        }
      }

      case postOnWall(sentTo: String, creationDate: String, content: String, location: String, shareWith: String) => {
        val reqUrl = url + "/" + clientUser + "/" + sentTo + "/" + "postOnWall"
        val keysMap: Map[String, String] = getKeys(shareWith, key)
        val newStatus: casePost = new casePost(sentTo, creationDate, RSA.encryptAES(key, initVector, "Post" + clientUser), location, keysMap, RSA.decrypt(userRandomKeyDB(clientUser), privateKey))
        val postOnOwnWallFuture: Future[HttpResponse] = pipeline(Post(reqUrl, newStatus))
        postOnOwnWallFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              println("\n" + clientUser + " posted Post - " + RSA.decryptAES(key, initVector, httpResponse.entity.asString))
            } else {
              println(httpResponse.entity.asString)
            }
          }
          case Failure(f) => {
            println("post On " + sentTo + " wall by " + clientUser + "failed with error message")
            println(f.getMessage)
          }
        }
      }

      case getUserStatus(ofUser: String) =>
        val reqUrl = url + "/" + ofUser + "/statusIds"
        val getUserStatusFuture: Future[HttpResponse] = pipeline(Get(reqUrl))
        getUserStatusFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              var statusIds: List[Long] = parse(httpResponse.entity.asString).extract[List[Long]]
              if (!statusIds.isEmpty) {
                for (id <- statusIds) {
                  val reqUrl = url + "/" + ofUser + "/" + clientUser + "/" + id.toString() + "/statusContent"
                  print(reqUrl)
                  val getStatusContentFuture: Future[HttpResponse] = pipeline(Get(reqUrl))
                  getStatusContentFuture.onComplete {
                    case Success(httpResponse) => {
                      if (httpResponse.status.isSuccess) {
                        if (httpResponse.entity.asString == "Access Denied")
                          println("Access Denied to view the Status")
                        else {
                          val strSet = httpResponse.entity.asString.split(",")
                          print(strSet(0) + "***" + strSet(1))
                          println("\nDecrypted Status:" + RSA.decryptAES(RSA.decrypt(strSet(1), privateKey), initVector, strSet(0)))
                        }
                      } else {
                        println("\n" + httpResponse.entity.asString)
                      }
                    }
                    case Failure(f) => {
                      print("Failed to retrieve status for user: " + ofUser)
                      println(f.getMessage)
                    }
                  }
                }
              }
            }
          }
          case Failure(f) => {
            print("Failed to retrieve status for user: " + ofUser)
            println(f.getMessage)
          }
        }

      case getUserPosts(ofUser: String, byUser: String) =>
        val reqUrl = url + "/" + ofUser + "/postIds"
        val getUserPostsFuture: Future[HttpResponse] = pipeline(Get(reqUrl))
        getUserPostsFuture.onComplete {
          case Success(httpResponse) => {
            if (httpResponse.status.isSuccess) {
              var postIds: List[Long] = parse(httpResponse.entity.asString).extract[List[Long]]
              if (!postIds.isEmpty) {
                for (id <- postIds) {
                  val reqUrl = url + "/" + ofUser + "/" + byUser + "/" + id.toString() + "/postContent"
                  print(reqUrl)
                  val getPostContentFuture: Future[HttpResponse] = pipeline(Get(reqUrl))
                  getPostContentFuture.onComplete {
                    case Success(httpResponse) => {
                      if (httpResponse.status.isSuccess) {
                        if (httpResponse.entity.asString == "Access Denied")
                          println("Access Denied to view the Post")
                        else {
                          val strSet = httpResponse.entity.asString.split(",")
                          print(strSet(0) + "***" + strSet(1))
                          println("\nDecrypted Post:" + RSA.decryptAES(RSA.decrypt(strSet(1), privateKey), initVector, strSet(0)))
                        }
                      } else {
                        println("\n" + httpResponse.entity.asString)
                      }
                    }
                    case Failure(f) => {
                      print("Failed to retrieve posts for user: " + ofUser)
                      println(f.getMessage)
                    }
                  }
                }
              }
            }
          }
          case Failure(f) => {
            print("Failed to retrieve posts for user: " + ofUser)
            println(f.getMessage)
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
                val commentOnPostFuture: Future[HttpResponse] = pipeline(Post(url + "/" + clientUser + "/" + ofUser + "/commentOnPost", new caseComment(ofUser, Constants.today, randPost, "comment" + ofUser)))
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

  def getKeys(input: String, key: String): Map[String, String] = {

    var keysMap: Map[String, String] = new HashMap[String, String]
    if (input == "All") {
      //  println("All")
      for ((k, v) <- userKeyDB) {
        keysMap += (k -> RSA.encrypt(key, v))
      }
    } else if (input == "Friends") {
      // println("Friends")
      for ((k, v) <- userKeyDB) {
        keysMap += (k -> RSA.encrypt(key, v))
      }
    } else {
      //println("Else")
      for ((k, v) <- userKeyDB) {
        keysMap += (k -> RSA.encrypt(key, v))
      }
    }
    keysMap
  }
}