package com.oblako

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.oblako.models._
import spray.json._

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait JsonSupport extends DefaultJsonProtocol {
  implicit val instantFormat: JsonFormat[java.time.Instant] = new JsonFormat[java.time.Instant] {
    def write(instant: java.time.Instant): JsValue = JsString(instant.toString)
    def read(value: JsValue): java.time.Instant = value match {
      case JsString(s) => java.time.Instant.parse(s)
      case _ => throw new DeserializationException("Expected ISO-8601 date string")
    }
  }

  implicit val uuidFormat: JsonFormat[UUID] = new JsonFormat[UUID] {
    def write(uuid: UUID): JsValue = JsString(uuid.toString)
    def read(value: JsValue): UUID = value match {
      case JsString(s) => UUID.fromString(s)
      case _ => throw new DeserializationException("Expected UUID string")
    }
  }

  implicit val todoFormat: RootJsonFormat[Todo] = jsonFormat7(Todo)
  implicit val createTodoRequestFormat: RootJsonFormat[CreateTodoRequest] = jsonFormat3(CreateTodoRequest)
  implicit val updateTodoRequestFormat: RootJsonFormat[UpdateTodoRequest] = jsonFormat4(UpdateTodoRequest)
}

class TodoRoutes(service: TodoService)(implicit ec: ExecutionContext) extends JsonSupport {
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  val routes: Route = {
    pathPrefix("api") {
      concat(
        // GET /api/todos - получить все задачи
        path("todos") {
          get {
            onComplete(service.getAllTodos()) {
              case Success(todos) => complete(todos)
              case Failure(ex) =>
                println(s"❌ Ошибка: ${ex.getMessage}")
                complete(StatusCodes.InternalServerError, s"Ошибка: ${ex.getMessage}")
            }
          }
        },

        // POST /api/todos - создать задачу
        path("todos") {
          post {
            entity(as[CreateTodoRequest]) { request =>
              onComplete(service.createTodo(request)) {
                case Success(todo) => complete(StatusCodes.Created, todo)
                case Failure(ex) =>
                  println(s"❌ Ошибка создания: ${ex.getMessage}")
                  complete(StatusCodes.BadRequest, s"Ошибка: ${ex.getMessage}")
              }
            }
          }
        },

        // GET /api/todos/:id - получить задачу по ID
        path("todos" / JavaUUID) { id =>
          get {
            onComplete(service.getTodoById(id)) {
              case Success(Some(todo)) => complete(todo)
              case Success(None) => complete(StatusCodes.NotFound, "Задача не найдена")
              case Failure(ex) => complete(StatusCodes.InternalServerError, s"Ошибка: ${ex.getMessage}")
            }
          }
        },

        // PUT /api/todos/:id - обновить задачу
        path("todos" / JavaUUID) { id =>
          put {
            entity(as[UpdateTodoRequest]) { request =>
              onComplete(service.updateTodo(id, request)) {
                case Success(Some(todo)) => complete(todo)
                case Success(None) => complete(StatusCodes.NotFound, "Задача не найдена")
                case Failure(ex) => complete(StatusCodes.BadRequest, s"Ошибка: ${ex.getMessage}")
              }
            }
          }
        },

        // DELETE /api/todos/:id - удалить задачу
        path("todos" / JavaUUID) { id =>
          delete {
            onComplete(service.deleteTodo(id)) {
              case Success(true) => complete(StatusCodes.NoContent)
              case Success(false) => complete(StatusCodes.NotFound, "Задача не найдена")
              case Failure(ex) => complete(StatusCodes.InternalServerError, s"Ошибка: ${ex.getMessage}")
            }
          }
        },

        // GET /api/stats - статистика
        path("stats") {
          get {
            onComplete(service.getStatistics()) {
              case Success(stats) => complete(stats)
              case Failure(ex) => complete(StatusCodes.InternalServerError, s"Ошибка: ${ex.getMessage}")
            }
          }
        }
      )
    }
  }
}