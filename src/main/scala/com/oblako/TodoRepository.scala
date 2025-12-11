package com.oblako

import com.oblako.models._
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class TodoRepository(db: Database)(implicit ec: ExecutionContext) {

  class TodoTable(tag: Tag) extends Table[Todo](tag, "todos") {
    def id = column[UUID]("id", O.PrimaryKey)
    def title = column[String]("title")
    def description = column[Option[String]]("description")
    def completed = column[Boolean]("completed")
    def priority = column[String]("priority")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")

    def * = (id, title, description, completed, priority, createdAt, updatedAt).mapTo[Todo]
  }

  private val todos = TableQuery[TodoTable]

  def findAll(): Future[Seq[Todo]] = {
    db.run(todos.sortBy(_.createdAt.desc).result)
  }

  def findById(id: UUID): Future[Option[Todo]] = {
    db.run(todos.filter(_.id === id).result.headOption)
  }

  def create(request: CreateTodoRequest): Future[Todo] = {
    val now = Instant.now()
    val todo = Todo(
      id = UUID.randomUUID(),
      title = request.title,
      description = request.description,
      completed = false,
      priority = request.priority,
      createdAt = now,
      updatedAt = now
    )
    db.run(todos += todo).map(_ => todo)
  }

  def update(id: UUID, request: UpdateTodoRequest): Future[Option[Todo]] = {
    val query = todos.filter(_.id === id)

    db.run(query.result.headOption).flatMap {
      case Some(existingTodo) =>
        val updatedTodo = existingTodo.copy(
          title = request.title.getOrElse(existingTodo.title),
          description = request.description.orElse(existingTodo.description),
          completed = request.completed.getOrElse(existingTodo.completed),
          priority = request.priority.getOrElse(existingTodo.priority),
          updatedAt = Instant.now()
        )
        db.run(query.update(updatedTodo)).map(_ => Some(updatedTodo))
      case None =>
        Future.successful(None)
    }
  }

  def delete(id: UUID): Future[Boolean] = {
    db.run(todos.filter(_.id === id).delete).map(_ > 0)
  }
}