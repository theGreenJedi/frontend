package controllers.commercial

import common.{ExecutionContexts, Logging}
import model.commercial.books.{BestsellersAgent, Book, BookFinder, CacheNotConfiguredException}
import model.commercial.{FeedMissingConfigurationException, FeedSwitchOffException}
import model.{Cached, NoCache}

import play.api.mvc._

import scala.concurrent.Future
import scala.util.control.NonFatal

object BookOffersController
  extends Controller
  with ExecutionContexts
  with Logging
  with implicits.Collections
  with implicits.Requests {

  def renderBook = Action.async { implicit request =>
    specificId map { isbn =>

      BookFinder.findByIsbn(isbn) map {
        _ map { book =>
          val clickMacro = request.getParameter("clickMacro")
          val omnitureId = request.getParameter("omnitureId")
          Cached(componentMaxAge) {
            jsonFormat.result(views.html.books.book(book, omnitureId, clickMacro))
          }
        } getOrElse {
          Cached(componentMaxAge)(jsonFormat.nilResult)
        }
      } recover {
        case e: FeedSwitchOffException =>
          log.warn(e.getMessage)
          NoCache(jsonFormat.nilResult)
        case e: FeedMissingConfigurationException =>
          log.warn(e.getMessage)
          NoCache(jsonFormat.nilResult)
        case e: CacheNotConfiguredException =>
          log.warn(e.getMessage)
          NoCache(jsonFormat.nilResult)
        case NonFatal(e) =>
          log.error(e.getMessage)
          NoCache(jsonFormat.nilResult)
      }

    } getOrElse {
      Future.successful(NoCache(jsonFormat.nilResult))
    }
  }

  def renderBooks = Action.async { implicit request =>

    def result(books: Seq[Book]): Result = books.distinctBy(_.isbn).take(5).toList match {
      case Nil =>
        NoCache(jsonFormat.nilResult)
      case someBooks =>
        Cached(componentMaxAge) {
          val clickMacro = request.getParameter("clickMacro")
          val omnitureId = request.getParameter("omnitureId")
          request.getParameter("layout") match {
            case Some("prominent") =>
              jsonFormat.result(views.html.books.booksProminent(someBooks, omnitureId, clickMacro))
            case _ =>
              if (conf.switches.Switches.v2BooksTemplate.isSwitchedOn) {
                jsonFormat.result(views.html.books.booksStandardV2(someBooks, omnitureId, clickMacro))
              } else {
                jsonFormat.result(views.html.books.booksStandard(someBooks, omnitureId, clickMacro))
              }
          }
        }
    }

    val isbns = request.queryString.getOrElse("t", Nil)
    BestsellersAgent.getSpecificBooks(isbns) map { specificBooks =>
      result(specificBooks ++ BestsellersAgent.bestsellersTargetedAt(segment))
    }
  }
}
