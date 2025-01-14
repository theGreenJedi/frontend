package controllers

import com.gu.contentapi.client.model.v1.{Content => ApiContent, Crossword, Section => ApiSection, ItemResponse}
import common.{Edition, ExecutionContexts, Logging}
import conf.Static
import contentapi.ContentApiClient
import crosswords.{AccessibleCrosswordRows, CrosswordPage, CrosswordSearchPage, CrosswordSvg}
import model._
import org.joda.time.{DateTime, LocalDate}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Action, Controller, RequestHeader, Result, _}
import services.{IndexPage, IndexPageItem}

import scala.concurrent.Future
import scala.concurrent.duration._

trait CrosswordController extends Controller with Logging with ExecutionContexts {
  def noResults()(implicit request: RequestHeader): Result

  def getCrossword(crosswordType: String, id: Int)(implicit request: RequestHeader): Future[ItemResponse] = {
    ContentApiClient.getResponse(ContentApiClient.item(s"crosswords/$crosswordType/$id", Edition(request)).showFields("all"))
  }

  def withCrossword(crosswordType: String, id: Int)(f: (Crossword, ApiContent) => Result)(implicit request: RequestHeader): Future[Result] = {
    getCrossword(crosswordType, id).map { response =>
       val maybeCrossword = for {
        content <- response.content
        crossword <- content.crossword }
       yield f(crossword, content)
       maybeCrossword getOrElse noResults
    } recover { case t: Throwable =>
      log.error(s"Error retrieving ${crosswordType} crossword id ${id} from API", t)
      noResults
    }
  }

  def renderCrosswordPage(crosswordType: String, id: Int)(implicit request: RequestHeader): Future[Result] = {
    withCrossword(crosswordType, id) { (crossword, content) =>
      Cached(60)(Ok(views.html.crossword(
        CrosswordPage(CrosswordContent.make(CrosswordData.fromCrossword(crossword), content)),
         CrosswordSvg(crossword, None, None, false)
      )))
    }
  }
}

object CrosswordPageController extends CrosswordController {

  def noResults()(implicit request: RequestHeader) = InternalServerError("Content API query returned an error.")

  def crossword(crosswordType: String, id: Int) = Action.async { implicit request =>
    renderCrosswordPage(crosswordType, id)
  }

  def accessibleCrossword(crosswordType: String, id: Int) = Action.async { implicit request =>
    withCrossword(crosswordType, id) { (crossword, content) =>
      Cached(60)(Ok(views.html.accessibleCrossword(
        new CrosswordPage(CrosswordContent.make(CrosswordData.fromCrossword(crossword), content)),
        AccessibleCrosswordRows(crossword)
      )))
    }
  }

  def printableCrossword(crosswordType: String, id: Int) = Action.async { implicit request =>
    withCrossword(crosswordType, id) { (crossword, content) =>
      Cached(3.days)(Ok(views.html.printableCrossword(
        CrosswordPage(CrosswordContent.make(CrosswordData.fromCrossword(crossword), content)),
        CrosswordSvg(crossword, None, None, false),
        new LocalDate().getYear()
      )))
    }
  }

  def thumbnail(crosswordType: String, id: Int) = Action.async { implicit request =>
    withCrossword(crosswordType, id) { (crossword, _) =>
      val xml = CrosswordSvg(crossword, Some("100%"), Some("100%"), trim = true)

      val globalStylesheet = Static("stylesheets/content.css")

      Cached(60) {
        Cors {
          Ok( s"""<?xml-stylesheet type="text/css" href="$globalStylesheet" ?>$xml""").as("image/svg+xml")
        }
      }
    }
  }
}

object CrosswordSearchController extends CrosswordController {
  val searchForm = Form(
    mapping(
      "crossword_type" -> nonEmptyText,
      "month" -> number,
      "year" -> number,
      "setter" -> optional(text)
    )(CrosswordSearch.apply)(CrosswordSearch.unapply)
  )

  val lookupForm = Form(
    mapping(
      "crossword_type" -> nonEmptyText,
      "id" -> number
    )(CrosswordLookup.apply)(CrosswordLookup.unapply)
  )

  def noResults()(implicit request: RequestHeader) = Cached(7.days)(Ok(views.html.crosswordsNoResults(CrosswordSearchPage.make())))

  def search() = Action.async { implicit request =>
    searchForm.bindFromRequest.fold(
      empty => Future.successful(Cached(7.days)(Ok(views.html.crosswordSearch(CrosswordSearchPage.make())))),

      params => {
        val withoutSetter = ContentApiClient.item(s"crosswords/series/${params.crosswordType}")
          .stringParam("from-date", params.fromDate.toString("yyyy-MM-dd"))
          .stringParam("to-date", params.toDate.toString("yyyy-MM-dd"))
          .pageSize(50)

        val maybeSetter = params.setter.fold(withoutSetter) { setter =>
          withoutSetter.stringParam("tag", s"profile/${setter.toLowerCase}")
        }

        ContentApiClient.getResponse(maybeSetter.showFields("all")).map { response =>
          response.results.getOrElse(Seq.empty).toList match {
            case Nil => noResults

            case results =>
              val section = Section.make(ApiSection("crosswords", "Crosswords search results", "http://www.theguardian.com/crosswords/search", "", Nil))
              val page = IndexPage(
                page = section,
                contents = results.map(IndexPageItem(_)),
                tags = Tags(Nil),
                date = DateTime.now,
                tzOverride = None
              )

              Cached(15.minutes)(Ok(views.html.index(page)))
          }
        }
      }
    )
  }

  def lookup() = Action.async { implicit request =>
    lookupForm.bindFromRequest.fold(
      formWithErrors => Future.successful(noResults),
      lookUpData => renderCrosswordPage(lookUpData.crosswordType, lookUpData.id)
    )
  }

  case class CrosswordSearch(crosswordType: String,
                             month: Int,
                             year: Int,
                             setter: Option[String]) {
    val fromDate = new LocalDate(year, month, 1)
    val toDate = fromDate.dayOfMonth.withMaximumValue.minusDays(1)
  }

  case class CrosswordLookup(crosswordType: String, id: Int)
}
