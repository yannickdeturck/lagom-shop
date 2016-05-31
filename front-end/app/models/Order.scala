package models

/**
  * @author Yannick De Turck
  */
case class Order(id: Option[String], itemId: String, amount: Int, customer: String)

object Order {
}
