package Nodes

import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.MutableList

class Comment(_commentID: BigInt,
    _createdBy: BigInt,
    _creationDate: String,
    _content: String,
    _likesCount: BigInt,
    _likesList: Map[Int, BigInt]) {

  val commentID: BigInt = _commentID
  val createdBy: BigInt = _createdBy
  val creationDate: String = _creationDate
  val content: String = _content
  val likesCount: BigInt = _likesCount
  val likesList: Map[Int, BigInt] = _likesList
}
