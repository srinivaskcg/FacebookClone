import akka.actor._
import akka.actor.Actor
import akka.actor.ActorSystem
import spray.httpx.SprayJsonSupport
import spray.json.AdditionalFormats
import spray.json.{ JsonFormat, DefaultJsonProtocol }
import spray.client.pipelining._
import spray.http._
import Nodes._
import scala.util.{ Success, Failure }
import scala.concurrent.Future
import scala.collection.mutable.ArrayBuffer

import Common._

object Simulator extends App {

  implicit val fbSystem = ActorSystem()

  var userActorArray: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]

  for (act <- 0 to 500) {
    println(act)
    var UserActor = fbSystem.actorOf(Props[Client.User], "UserActor" + act)
    val newCaseUser: caseUser = new caseUser("userId" + act, "firstName" + act, "lastName" + act, "12101988" + act, "27112015" + act, "email" + act)
    println(newCaseUser)
    UserActor ! createUser(newCaseUser)
    userActorArray .+= (UserActor)
    Thread.sleep(10)
  }
  
  for (act <- 0 to 500) {
    println("get"+act)
    userActorArray(act) ! getUserInfo("userId"+(act))
    Thread.sleep(10)
  }
  
  /*for (act <- 0 to 999) {
    userActorArray(0) ! sendFriendRequest("userId" + (act + 1))
    Thread.sleep(10)
    userActorArray(act + 1) ! manageFriendRequest("userId"+act, "Accept")
  }*/
}