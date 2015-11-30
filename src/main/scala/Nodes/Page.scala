package Nodes

import scala.collection.immutable.HashMap
import scala.collection.mutable.MutableList

class Page {

  var pageID: String = new String()
  var pageName: String = new String()
  var pageCreatedBy: String = new String()

  var pageCreationDate: String = new String()
  var description: String = new String()

  var likes = new MutableList[String]()
  var posts = new HashMap[Long, Post]()
}