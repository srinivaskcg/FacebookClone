package Nodes

import scala.collection.immutable.HashMap

class Post(_postID: BigInt,
    _createdBy: BigInt,
    _creationDate: String,
    _content: String,
    _location: String) {

  val postID: BigInt = _postID
  val createdBy: BigInt = _createdBy
  val creationDate: String = _creationDate
  val content: String = _content
  val location: String = _location

}