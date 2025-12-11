package com.oblako

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpMethods, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "todo-system")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    // Настройка подключения к Supabase PostgreSQL
    val dbUrl = "jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:6543/postgres"
    val dbUser = "postgres.hojkxyreebuvrwfccexu"
    val dbPassword = "ТУТ_ТВОЙ_ПАРОЛЬ_ОТ_SUPABASE" // Замени на свой пароль!

    val db = Database.forURL(
      url = dbUrl,
      user = dbUser,
      password = dbPassword,
      driver = "org.postgresql.Driver"
    )

    // Создание слоёв приложения
    val repository = new TodoRepository(db)
    val service = new TodoService(repository)
    val todoRoutes = new TodoRoutes(service)

    // CORS для работы с фронтендом
    val corsHeaders = List(
      `Access-Control-Allow-Origin`.*,
      `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.OPTIONS),
      `Access-Control-Allow-Headers`("Content-Type")
    )

    def addCorsHeaders(route: Route): Route = {
      respondWithHeaders(corsHeaders) {
        options {
          complete(StatusCodes.OK)
        } ~ route
      }
    }

    // Общий роутинг
    val routes: Route = addCorsHeaders {
      concat(
        todoRoutes.routes,
        pathEndOrSingleSlash {
          getFromResource("webapp/index.html")
        },
        getFromResourceDirectory("webapp")

      )
    }

    val port = sys.env.getOrElse("PORT", "8080").toInt
    val bindingFuture = Http().newServerAt("0.0.0.0", port).bind(routes)


    bindingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println(s"""
                   |🚀 ========================================
                   |   Сервер запущен!
                   |   http://${address.getHostString}:${address.getPort}/
                   |========================================
                   |📝 API endpoints:
                   |   GET    /api/todos       - все задачи
                   |   POST   /api/todos       - создать задачу
                   |   GET    /api/todos/:id   - получить задачу
                   |   PUT    /api/todos/:id   - обновить задачу
                   |   DELETE /api/todos/:id   - удалить задачу
                   |   GET    /api/stats       - статистика
                   |========================================
                   |Нажми ENTER для остановки сервера...
                   |""".stripMargin)

      case Failure(ex) =>
        println(s"❌ Не удалось запустить сервер: ${ex.getMessage}")
        system.terminate()
    }

    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete { _ =>
        db.close()
        system.terminate()
        println("👋 Сервер остановлен")
      }
  }
}