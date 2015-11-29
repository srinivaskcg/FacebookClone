package Nodes

import scala.collection.immutable.HashMap

class Post {

  var postID: Long = 0L
  var createdBy: String = new String()
  var creationDate: String = new String()
  var content: String = new String()
  var location: String = new String()
  var likes: HashMap[Int, String] = new HashMap[Int, String]()
  var comments: HashMap[Int, Comment] = new HashMap[Int, Comment]()
  var shares: HashMap[Int, String] = new HashMap[Int, String]()

}