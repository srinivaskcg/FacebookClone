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

  for (act <- 0 to 5) {
    println("Create user : "+act)
    var UserActor = fbSystem.actorOf(Props[Client.User], "UserActor" + act)
    val newCaseUser: caseUser = new caseUser( "user" + act,"27112015" + act, "firstName" + act, "lastName" + act, "12101988" + act, "email" + act)
    UserActor ! createUser(newCaseUser)
    userActorArray .+= (UserActor) 
  }
  
  Thread.sleep(10000)
  
  for (act <- 0 to 5) {
    println("get user info : "+act)
    userActorArray(act) ! getUserInfo("user"+(act))
  }
  
  Thread.sleep(3000)
  
  for (act <- 0 to 4) {
    println("sending friend request from: "+ act + " to: " + (act+1) )
    userActorArray(0) ! sendFriendRequest("user" + (act), "user" + (act + 1))
    Thread.sleep(1000)
    println("accepting friend request from: "+ act + " by: " + (act+1) )
    userActorArray(act + 1) ! manageFriendRequest("user" + (act+1), "user" + (act), "Accept")
  }
  
  for (act <- 0 to 5) {
    println("Create post : "+act)
    val newCasePost: casePost = new casePost("user" + act, "Dummy", "28112015" + act, "Post" + act, "Gainesville" + act)
    userActorArray(act) ! postOnOwnWall(newCasePost)
  }
  
  //Thread.sleep(5000)
  
  for (act <- 0 to 5) {
    println("get post info : "+act)
    userActorArray(act) ! getUserPosts("user"+(act))
  }
  
  for (act <- 1 to 5) {
    println("Create post on wall : "+act)
    val newCasePost: casePost = new casePost("user" + act, "user" +(act-1), "28112015" + act, "Post" + act, "Gainesville" + act)
    userActorArray(act) ! postOnWall(newCasePost)
  }
  
  Thread.sleep(3000)
  
  for (act <- 0 to 5) {
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
  
  fbSystem.shutdown()
}