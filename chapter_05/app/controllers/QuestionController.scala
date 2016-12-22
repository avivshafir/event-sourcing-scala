package controllers

/**
  * Created by denis on 12/14/16.
  */

import play.api.libs.json.Json
import security.UserAuthAction
import play.api.mvc.Controller
import services.{QuestionEventProducer, ReadService}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class QuestionController(questionEventProducer: QuestionEventProducer,
    userAuthAction: UserAuthAction, readService: ReadService) extends Controller {

def createQuestion() = userAuthAction.async { implicit request =>
  createQuestionForm.bindFromRequest.fold(
    formWithErrors => Future.successful(BadRequest),
    data => {
      val resultF = questionEventProducer.createQuestion(
        data.title, data.details, data.tags, request.user.userId)
      resultF.map {
        case Some(error) => InternalServerError
        case None => Ok
      }
    }
  )
}

  def deleteQuestion() = userAuthAction { implicit request =>
    deleteQuestionForm.bindFromRequest.fold(
      formWithErrors => BadRequest,
      data => {
        questionEventProducer.deleteQuestion(data.id, request.user.userId); Ok
      }
    )
  }

  import scala.util.{Failure, Success}
  import play.api.mvc.Action
  def getQuestions = Action { implicit request =>
    val questionsT = readService.getAllQuestions
    questionsT match {
      case Failure(th) => InternalServerError
      case Success(questions) => Ok(Json.toJson(questions))
    }
  }

  import play.api.data.Form
  import play.api.data.Forms._
val createQuestionForm = Form {
  mapping(
    "title" -> nonEmptyText,
    "details" -> optional(text),
    "tags" -> seq(uuid)
  )(CreateQuestionData.apply)(CreateQuestionData.unapply)
}

val deleteQuestionForm = Form {
  mapping(
    "id" -> uuid
  )(DeleteQuestionData.apply)(DeleteQuestionData.unapply)
}

  import java.util.UUID
  case class CreateQuestionData(title: String,
      details: Option[String], tags: Seq[UUID])
  case class DeleteQuestionData(id: UUID)
}
