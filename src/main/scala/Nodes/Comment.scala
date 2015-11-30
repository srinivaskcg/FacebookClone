package Nodes

import scala.collection.immutable.HashMap

class Comment {

  var commentID: Long = 0L
  var content: String = new String()
  var createdBy: String = new String()
  var creationDate: String = new String()
  var likesCount: String = new String()
  var likesList: HashMap[String, String] = new HashMap[String, String]()

}
