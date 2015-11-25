package Server

import java.security.SecureRandom
import java.text.SimpleDateFormat

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import spray.http.HttpRequest

import scala.collection.mutable.Map


class Common(userHandle: ActorRef, pageHandle: ActorRef) extends Actor with ActorLogging {

  def receive: Receive = {
  }
}
