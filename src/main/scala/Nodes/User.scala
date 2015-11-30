package Nodes

import spray.json.DefaultJsonProtocol
import DefaultJsonProtocol._
import scala.collection.immutable.HashMap
import java.util.Date

import Nodes._

class User() {

  var userID: String = new String()
  var firstName: String = new String()
  var lastName: String = new String()
  var dateOfBirth: String = new String()
  var creationDate: String = new String()
  var email: String = new String()
  var pendingRequests: Map[String, String] = new HashMap[String, String]
  var friends: Map[String, String] = new HashMap[String, String]
  var posts: HashMap[Long, Post] = new HashMap[Long, Post]()
  var pages: Map[String, Page] = new HashMap[String, Page]()
}
 
