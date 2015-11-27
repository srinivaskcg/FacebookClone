package Nodes

import spray.json.DefaultJsonProtocol
import DefaultJsonProtocol._
import scala.collection.immutable.HashMap
import java.util.Date

import Nodes._

class User(_userID: BigInt,
    _creationDate: String,
    _firstName: String,
    _lastName: String,
    _dateOfBirth: String,
    _friends: Map[Int,BigInt],
    _friendRequests: Map[Int,BigInt],
    _posts: Map[Int,BigInt]) {

  val userID: BigInt = _userID
  val creationDate: String = _creationDate
  val firstName: String = _firstName
  val lastName: String = _lastName
  val dateOfBirth: String = _dateOfBirth
  val friends: Map[Int,BigInt] = _friends
  val friendRequests: Map[Int,BigInt] = _friendRequests
  val posts: Map[Int,BigInt] = _posts
}
 
