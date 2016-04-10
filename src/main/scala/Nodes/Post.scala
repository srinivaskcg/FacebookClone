package Nodes

import scala.collection.immutable.HashMap

class Post {

  var postID: Long = 0L
  var createdBy: String = new String()
  var createdTo: String = new String()
  var creationDate: String = new String()
  var content: String = new String()
  var shareWith: String = new String()
  var canSeeMap: Map[String, String] = new HashMap[String, String]()

  var location: String = new String()
  //var likes: HashMap[Long, String] = new HashMap[Long, String]()
  var comments: HashMap[Long, Comment] = new HashMap[Long, Comment]()
  //var shares: HashMap[Long, String] = new HashMap[Long, String]()
}