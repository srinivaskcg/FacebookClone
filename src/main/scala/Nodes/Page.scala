package Nodes

import scala.collection.immutable.HashMap

class Page {

  var pageID: String = new String()
  var name: String = new String()
  var createdBy: String = new String()
  var creationDate: String = new String()
  var description: String = new String()
  var likesList: HashMap[String, String] = new HashMap[String, String]()
  var posts: HashMap[Long, Post] = new HashMap[Long, Post]()
}