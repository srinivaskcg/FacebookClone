package Nodes

import scala.collection.mutable.MutableList
import java.util.HashMap

class Page {

  var pageID: Long = 0L
  var name: String = new String()
  var createdBy: String = new String()
  var creationDate: String = new String()
  var description: String = new String()
  var likesList: HashMap[String, String] = new HashMap[String, String]()
  var posts: HashMap[Int, Post] = new HashMap[Int, Post]()
}