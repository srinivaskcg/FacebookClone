import akka.actor._
import akka.actor.{ ActorSystem, Actor, Props, ActorRef }
import akka.util.Timeout
import spray.httpx.SprayJsonSupport
import spray.json.AdditionalFormats
import spray.json.{ JsonFormat, DefaultJsonProtocol }
import spray.client.pipelining._
import spray.http._
import scala.util.Random
import scala.util.{ Success, Failure }
import scala.concurrent.Future
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.collection.mutable.HashMap
import java.util.Calendar
import java.util.Date
import Common._
import Nodes._
import shapeless.ToInt

object Constants {
  var possibleGender: List[String] = List("Male", "Female")
  val today = Calendar.getInstance().getTime().toString()
  val birthDate = Calendar.getInstance().getTime().toString()
  val action = "accept"
}

object Simulation extends App {

  implicit val timeout = Timeout(Duration.create(5000, TimeUnit.MILLISECONDS))

  var userCount = args(0).toInt
  var pageIndex = 0

  implicit val facebookUserSystem = ActorSystem()

  var simActorMap: HashMap[String, ActorRef] = new HashMap[String, ActorRef]

  for (actor <- 0 to userCount - 1) {

    var UserActor = facebookUserSystem.actorOf(Props[Client.User], "UserActor" + actor)

    val newUser: caseUser = new caseUser("userId" + actor, Constants.today, "firstName" + actor, "lastName" + actor,
      Constants.possibleGender(Random.nextInt(Constants.possibleGender.length)), Constants.birthDate, "email" + actor)

    println("User creation requested by " + "userId" + actor)

    UserActor ! registerUser(newUser)
    simActorMap.+=("userId" + actor -> UserActor)
  }

  Thread.sleep(userCount * 2)

  facebookUserSystem.scheduler.schedule(
    Duration.create(5000, TimeUnit.MILLISECONDS), Duration.create(10000, TimeUnit.MILLISECONDS))(scheduleFriendRequest)
    
  facebookUserSystem.scheduler.schedule(
    Duration.create(5000, TimeUnit.MILLISECONDS), Duration.create(10000, TimeUnit.MILLISECONDS))(scheduleGetUserInfo)

  facebookUserSystem.scheduler.schedule(
    Duration.create(5000, TimeUnit.MILLISECONDS), Duration.create(2000, TimeUnit.MILLISECONDS))(scheduleUpdateStatus)

  facebookUserSystem.scheduler.schedule(
    Duration.create(5000, TimeUnit.MILLISECONDS), Duration.create(2000, TimeUnit.MILLISECONDS))(scheduleUpdatePost)

  facebookUserSystem.scheduler.schedule(
      Duration.create(2000, TimeUnit.MILLISECONDS), Duration.create(2000, TimeUnit.MILLISECONDS))(scheduleGetUserPosts)

  facebookUserSystem.scheduler.schedule(
    Duration.create(5000, TimeUnit.MILLISECONDS), Duration.create(2000, TimeUnit.MILLISECONDS))(schedulePostComments)

  facebookUserSystem.scheduler.schedule(
    Duration.create(5000, TimeUnit.MILLISECONDS), Duration.create(10000, TimeUnit.MILLISECONDS))(schedulePageCreation)

  facebookUserSystem.scheduler.schedule(
    Duration.create(5000, TimeUnit.MILLISECONDS), Duration.create(4000, TimeUnit.MILLISECONDS))(schedulePagePosts)

  facebookUserSystem.scheduler.schedule(
    Duration.create(5000, TimeUnit.MILLISECONDS), Duration.create(4000, TimeUnit.MILLISECONDS))(schedulePageLikes)

  def scheduleFriendRequest() = {

    var randomVar = getRandomUsers()
    var sender = randomVar._2._1
    var randomReceiver = randomVar._1._2
    var randomSender = randomVar._1._1

    sender ! sendFriendRequest(randomReceiver)

    Thread.sleep(50)

    simActorMap(randomReceiver) ! manageFriendRequest(randomSender, Constants.action)
  }
  
  def scheduleGetUserInfo() = {
    var sender = getRandomUsers()._2._1
    var send = getRandomUsers()._1._1
    sender ! getUserInfo(send)
  }

  def scheduleUpdateStatus() = {
    var sender = getRandomUsers()._2._1
    var send = getRandomUsers()._1._1

    val newStatusPost: casePost = new casePost(send, Constants.today, "Status" + sender, "Gainesville")
    sender ! postOnOwnWall(newStatusPost)
  }

  def scheduleUpdatePost() = {
    var sender = getRandomUsers()._2._1
    var receive = getRandomUsers()._1._2

    val newWallPost: casePost = new casePost(receive, Constants.today, "Status" + sender, "Gainesville")
    sender ! postOnWall(newWallPost)
  }

  def scheduleGetUserPosts() = {

    var randomVar = getRandomUsers()
    var sender = randomVar._2._1
    var receiver = randomVar._2._2
    var randomReceiver = randomVar._1._2
    var randomSender = randomVar._1._1

    sender ! getUserPosts(randomReceiver)
  }

  def schedulePostComments() = {
    var randomVar = getRandomUsers()
    var sender = randomVar._2._1
    var receiver = randomVar._2._2
    var randomReceiver = randomVar._1._2
    var randomSender = randomVar._1._1

    sender ! commentOnPost(randomReceiver)
  }

  def schedulePageCreation() = {

    pageIndex = pageIndex + 1
    var randomVar = getRandomUsers()

    var actor = randomVar._2._1
    var createdBy: String = randomVar._1._1

    var casePage = new casePage(Constants.today, "page" + pageIndex, "page created by " + createdBy)

    actor ! createPage(casePage)
  }

  def schedulePagePosts() = {
    var randomVar = getRandomUsers()

    var actor = randomVar._2._1
    var createdBy: String = randomVar._1._1

    var randPageId: String = "page" + Random.nextInt(pageIndex)

    val newPagePost: casePost = new casePost(createdBy, Constants.today, "PagePost", "Gainesville")

    actor ! createPagePost(randPageId, newPagePost)
  }

  def schedulePageLikes() = {
    var randomVar = getRandomUsers()
    var actor = randomVar._2._1
    var createdBy: String = randomVar._1._1

    var randPageId: String = "page" + Random.nextInt(pageIndex)

    actor ! likePage(randPageId, createdBy)
  }

  /*   for (act <- 0 to 5) {
    println("Create Page : "+act)
    val newCasePage: casePage = new casePage( "page" + act, "27112015" + act, "page name" + act, "page description " + act)
    userActorArray(act) ! createPage(newCasePage)
  }
  
  Thread.sleep(3000)
  
  for (act <- 0 to 5) {
    println("Create Page Post: "+act)
    val newCasePost: casePost = new casePost("user" + act, "user" +(act-1), "28112015" + act, "Page Post" + act, "Gainesville" + act)
    userActorArray(act) ! createPagePost("page" + act, newCasePost)
  }
  
   Thread.sleep(3000)

  for (act <- 1 to 10) {
    println("Comment on Post: " + act)
    val newCaseComent: caseComment = new caseComment("user" + act, "28112015" + act, "user" + (act - 1), "comment" + act)
    userActorArray(act) ! commentOnPost("user" + (act - 1), newCaseComent)
  }

  //fbSystem.shutdown()
*/
  def getRandomUsers(): ((String, String), (ActorRef, ActorRef)) = {
    var randomSender = simActorMap.keys.toList(Random.nextInt(simActorMap.size))
    var requestSender: ActorRef = simActorMap(randomSender)
    var randomReceiver = simActorMap.keys.toList(Random.nextInt(simActorMap.size))
    var requestReceiver: ActorRef = simActorMap(randomReceiver)
    return ((randomSender, randomReceiver), (requestSender, requestReceiver))
  }

}