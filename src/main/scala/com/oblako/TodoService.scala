package com.oblako

import com.oblako.models._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class TodoService(repository: TodoRepository)(implicit ec: ExecutionContext) {

  def getAllTodos(): Future[Seq[Todo]] = {
    println("📋 Получение всех задач")
    repository.findAll()
  }

  def getTodoById(id: UUID): Future[Option[Todo]] = {
    println(s"🔍 Поиск задачи с ID: $id")
    repository.findById(id)
  }

  def createTodo(request: CreateTodoRequest): Future[Todo] = {
    // Валидация
    if (request.title.trim.isEmpty) {
      throw new IllegalArgumentException("Название задачи не может быть пустым")
    }

    if (!Seq("low", "medium", "high").contains(request.priority)) {
      throw new IllegalArgumentException("Неверный приоритет")
    }

    println(s"✅ Создание новой задачи: ${request.title}")
    repository.create(request)
  }

  def updateTodo(id: UUID, request: UpdateTodoRequest): Future[Option[Todo]] = {
    // Валидация
    request.title.foreach { title =>
      if (title.trim.isEmpty) {
        throw new IllegalArgumentException("Название не может быть пустым")
      }
    }

    request.priority.foreach { priority =>
      if (!Seq("low", "medium", "high").contains(priority)) {
        throw new IllegalArgumentException("Неверный приоритет")
      }
    }

    println(s"✏️ Обновление задачи с ID: $id")
    repository.update(id, request)
  }

  def deleteTodo(id: UUID): Future[Boolean] = {
    println(s"🗑️ Удаление задачи с ID: $id")
    repository.delete(id)
  }

  def getStatistics(): Future[Map[String, Int]] = {
    getAllTodos().map { todos =>
      Map(
        "total" -> todos.length,
        "active" -> todos.count(!_.completed),
        "completed" -> todos.count(_.completed),
        "high_priority" -> todos.count(_.priority == "high")
      )
    }
  }
}