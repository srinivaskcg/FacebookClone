package Nodes

import scala.collection.mutable.MutableList

class Page(_pageID: BigInt,
    _createdBy: BigInt,
    _creationDate: String,
    _name: String,
    _description: String,
    _likesList: Map[Int, BigInt],
    _posts: Map[Int, BigInt]) {

  val pageID: BigInt = _pageID
  val createdBy: BigInt = _createdBy
  val creationDate: String = _creationDate
  val name: String = _name
  val description: String = _description
  val likesList: Map[Int, BigInt] = _likesList
  val posts: Map[Int, BigInt] = _posts
}