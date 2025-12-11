package com.oblako.models

import java.time.Instant
import java.util.UUID

case class Todo(
                 id: UUID,
                 title: String,
                 description: Option[String],
                 completed: Boolean,
                 priority: String,
                 createdAt: Instant,
                 updatedAt: Instant
               )

case class CreateTodoRequest(
                              title: String,
                              description: Option[String] = None,
                              priority: String = "medium"
                            )

case class UpdateTodoRequest(
                              title: Option[String] = None,
                              description: Option[String] = None,
                              completed: Option[Boolean] = None,
                              priority: Option[String] = None
                            )