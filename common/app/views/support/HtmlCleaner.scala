package views.support

import java.text.Normalizer
import java.util.regex.{Matcher, Pattern}

import common.{Edition, LinkTo}
import conf.switches.Switches._
import layout.ContentWidths
import layout.ContentWidths._
import model._
import model.content.{Atom, Atoms}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, TextNode}
import play.api.mvc.RequestHeader
import scala.collection.JavaConversions._

trait HtmlCleaner {
  def clean(d: Document): Document
}

object BlockNumberCleaner extends HtmlCleaner {

  private val Block = """<!-- Block (\d*) -->""".r

  override def clean(document: Document): Document = {
    document.getAllElements.foreach { element =>
      val blockComments = element.childNodes.flatMap { node =>
        node.toString.trim match {
          case Block(num) =>
            Option(node.nextSibling).foreach(_.attr("id", s"block-$num"))
            Some(node)
          case _ => None
        }
      }
      blockComments.foreach(_.remove())
    }
    document
  }
}

object BlockquoteCleaner extends HtmlCleaner {

  override def clean(document: Document): Document = {
    val quotedBlockquotes = document.getElementsByTag("blockquote").filter(_.hasClass("quoted"))
    val quoteSvg = views.html.fragments.inlineSvg("quote", "icon").toString()
    val wrapBlockquoteChildren = (blockquoteElement: Element) => {
      val container = document.createElement("div")
      container.addClass("quoted__contents")
      // Get children before mutating
      val children = blockquoteElement.children()
      blockquoteElement.prependChild(container)
      container.insertChildren(0, children)

      blockquoteElement.prepend(quoteSvg)
    }
    quotedBlockquotes.foreach(wrapBlockquoteChildren)
    document
  }
}

object PullquoteCleaner extends HtmlCleaner {

  override def clean(document: Document): Document = {
    val pullquotes = document.getElementsByTag("aside").filter(_.hasClass("element-pullquote"))
    val openingQuoteSvg = views.html.fragments.inlineSvg("quote", "icon", List("inline-tone-fill")).toString()
    val closingQuoteSvg = views.html.fragments.inlineSvg("quote", "icon", List("closing", "inline-tone-fill")).toString()

    pullquotes.foreach { element: Element =>
      element.prepend(openingQuoteSvg)
      element.append(closingQuoteSvg)
      element.getElementsByTag("p").addClass("pullquote-paragraph")
      element.getElementsByTag("cite").addClass("pullquote-cite")
    }

    document
  }
}

case object R2VideoCleaner extends HtmlCleaner {

  override def clean(document: Document): Document = {

    val legacyVideos = document.getElementsByTag("video").filter(_.hasClass("gu-video")).filter(_.parent().tagName() != "figure")

    legacyVideos.foreach( videoElement => {
      videoElement.wrap("<figure class=\"test element element-video\"></figure>")
    })

    document
  }

}

case class PictureCleaner(article: Article, amp: Boolean)(implicit request: RequestHeader) extends HtmlCleaner with implicits.Numbers {

  def clean(body: Document): Document = {
    for {
      figure <- body.getElementsByTag("figure")
      image <- figure.getElementsByTag("img").headOption
      if !(figure.hasClass("element-comment") ||
           figure.hasClass("element-witness") ||
           figure.hasClass("element-atom"))
      container <- findContainerFromId(figure.attr("data-media-id"), image.attr("src"))
      image <- container.images.largestImage
    }{
      val hinting = findBreakpointWidths(figure)

      val relation = {
        if (article.isLiveBlog) LiveBlogMedia
        else if (article.isImmersive) ImmersiveMedia
        else if (article.isUSMinute) MinuteMedia
        else BodyMedia
      }

      val widths = ContentWidths.getWidthsFromContentElement(hinting, relation)

      val orientationClass = image.orientation match {
        case Portrait => Some("img--portrait")
        case _ => Some("img--landscape")
      }

      val smallImageClass = hinting match {
        case Thumbnail => None
        case _ if image.width <= 220 => Some("img--inline")
        case _ => None
      }

      val inlineClass = if (article.isUSMinute && !figure.hasClass("element--thumbnail")) Some("element--inline") else None

      val figureClasses = List(orientationClass, smallImageClass, hinting.className, inlineClass).flatten.mkString(" ")

      // lightbox uses the images in the order mentioned in the header array
      val lightboxInfo: Option[(Int, ImageAsset)] = for {
        index <- Some(article.lightbox.lightboxImages.indexOf(container)).flatMap(index => if (index == -1) None else Some(index + 1))
        crop <- container.images.largestEditorialCrop
        if !article.isLiveBlog
      } yield (index, crop)

      val html = views.html.fragments.img(
        container.images,
        lightboxIndex = lightboxInfo.map(_._1),
        widthsByBreakpoint = widths,
        image_figureClasses = Some(image, figureClasses),
        shareInfo = lightboxInfo.map{case (index, crop) => (article.sharelinks.elementShares(s"img-$index", crop.url), article.metadata.contentType) },
        amp = amp
      ).toString()

      figure.replaceWith(Jsoup.parseBodyFragment(html).body().child(0))
    }

    body
  }

  def findContainerFromId(id: String, src: String): Option[ImageElement] = {
    // It is possible that a single data media id can appear multiple times in the elements array.
    val srcImagePath = new java.net.URL(src).getPath()
    val imageContainers = article.elements.bodyImages.filter(_.properties.id == id)

    // Try to match the container based on both URL and media ID.
    val fullyMatchedImage: Option[ImageElement] = {
      for {
        container <- imageContainers
        asset <- container.images.imageCrops
        url <- asset.url
        if url.contains(srcImagePath)
      } yield { container }
    }.headOption

    fullyMatchedImage.orElse(imageContainers.headOption)
  }

  def findBreakpointWidths(figure: Element): ContentHinting = {

    figure.classNames().map(Some(_)) match {
      case classes if classes.contains(Supporting.className) => Supporting
      case classes if classes.contains(Showcase.className) => Showcase
      case classes if classes.contains(Thumbnail.className) => Thumbnail
      case classes if classes.contains(Immersive.className) => Immersive
      case _ => Inline
    }
  }
}

object BulletCleaner {
  def apply(body: String): String = body.replace("•", """<span class="bullet">•</span>""")
}

object VideoEncodingUrlCleaner {
  def apply(url: String): String = url.filter(_ != '\n')
}

object AmpSrcCleaner {
  def apply(videoSrc: String) = {
    // All media sources need to start with https for AMP.
    // Temperary code until all media urls returned from CAPI are https
    if (videoSrc.startsWith("http:")) {
      val (first, last) = videoSrc.splitAt(4);
      first + "s" + last
    }
  }
}

case class InBodyLinkCleaner(dataLinkName: String, amp: Boolean = false)(implicit val edition: Edition, implicit val request: RequestHeader) extends HtmlCleaner {
  def clean(body: Document): Document = {
    val links = body.getElementsByAttribute("href")

    links.foreach { link =>
      if (link.tagName == "a") {
        link.attr("href", LinkTo(link.attr("href"), edition))
        link.attr("data-link-name", dataLinkName)
        link.attr("data-component", dataLinkName.replace(" ", "-"))
        link.addClass("u-underline")
      }
      if (amp && link.hasAttr("style")) {
        link.removeAttr("style")
      }
    }

    // Prevent text in non clickable anchors from looking like links
    // <a name="foo">bar</a> -> <a name="foo"></a>bar
    val anchors = body.getElementsByAttribute("name")

    anchors.foreach { anchor =>
      if (anchor.tagName == "a") {
        val text = anchor.ownText()
        anchor.empty().after(text)
      }
    }

    body
  }
}

case class TruncateCleaner(limit: Int)(implicit val edition: Edition, implicit val request: RequestHeader) extends HtmlCleaner {
  def clean(body: Document): Document = {

    def truncateTextNode(charLimit: Int, textNode: TextNode): Int = {
      val newCharLimit = charLimit - textNode.text.length
      if (newCharLimit < 0) {
        textNode.text(textNode.text.take(charLimit.max(0)).trim.stripSuffix(".") + (if (charLimit > 0) "…" else ""))
      }
      newCharLimit
    }

    def truncateElement(charLimit: Int, element: Element): Int = {
      element.childNodes.foldLeft(charLimit) {
        (t, node) =>
          if (node.isInstanceOf[TextNode]) {
            truncateTextNode(t, node.asInstanceOf[TextNode])
          } else if (node.isInstanceOf[Element]) {
            truncateElement(t, node.asInstanceOf[Element])
          } else {
            t
          }
      }
    }

    truncateElement(limit, body)
    body
  }
}

class TweetCleaner(content: Content, amp: Boolean) extends HtmlCleaner {

  override def clean(document: Document): Document = {

    document.getElementsByClass("element-tweet").foreach { tweet =>

      val tweetData: Option[Tweet] = Option(tweet.attr("data-canonical-url")).flatMap { url =>
        url.split('/').lastOption.flatMap { id =>
          content.tweets.find(_.id == id)
        }
      }

      val tweetImage = tweetData.flatMap(_.firstImage)

      tweet.getElementsByClass("twitter-tweet").foreach { element =>

        if (amp) {
          tweetData.foreach { elem =>
            element.empty()
            element.tagName("amp-twitter")
            element.attr("data-tweetId", elem.id)
            element.attr("data-​c​ards", "hidden")
            element.attr("layout", "responsive")
            element.attr("width", "486")
            element.attr("data-conversation","none")
            // temporary fix to give tweets with an image a larger height
            if (elem.firstImage.size > 0) {
              element.attr("height", "437")
            } else {
              element.attr("height", "179")
            }
          }
        } else {
          val el = element.clone()

          if (el.children.size > 1) {
            val body = el.child(0).attr("class", "tweet-body")
            val date = el.child(1).attr("class", "tweet-date")
            val user = el.ownText()
            val userEl = document.createElement("span").attr("class", "tweet-user").text(user)
            val link = document.createElement("a").attr("href", date.attr("href")).attr("style", "display: none;")

            element.empty().removeClass("twitter-tweet").addClass("js-tweet tweet")

            tweetImage.foreach { image =>
              val img = document.createElement("img")
              img.attr("src", image)
              img.attr("alt", "")
              img.attr("rel", "nofollow")
              img.addClass("js-tweet-main-image tweet-main-image")
              element.appendChild(img)
            }

            element.appendChild(userEl).appendChild(date).appendChild(body).appendChild(link)
          }
        }
      }
    }
    document
  }
}

case class TagLinker(article: Article)(implicit val edition: Edition, implicit val request: RequestHeader) extends HtmlCleaner{

  private val group1 = "$1"
  private val group2 = "$2"
  private val group4 = "$4"
  private val group5 = "$5"

  private val dot = Pattern.quote(".")
  private val question = Pattern.quote("?")

  private def keywordRegex(tag: Tag) = {
    val tagName = Pattern.quote(Matcher.quoteReplacement(tag.name))
    s"""(.*)( |^)($tagName)( |,|$$|$dot|$question)(.*)""".r
  }

  def clean(doc: Document): Document = {

    if (article.content.showInRelated) {

      // Get all paragraphs which are not contained in a pullquote
      val paragraphs = doc.getElementsByTag("p").filterNot( p =>
        p.parents.exists( ancestor =>
          ancestor.tagName() == "aside" && ancestor.hasClass("element-pullquote")
        )
      )

      // order by length of name so we do not make simple match errors
      // e.g 'Northern Ireland' & 'Ireland'
      article.tags.keywords.filterNot(_.isSectionTag).sortBy(_.name.length).reverse.foreach { keyword =>

        // don't link again in paragraphs that already have links
        val unlinkedParas = paragraphs.filterNot(_.html.contains("<a"))

        // pre-filter paragraphs so we do not do multiple regexes on every single paragraph in every single article
        val candidateParagraphs = unlinkedParas.filter(_.html.contains(keyword.name))

        if (candidateParagraphs.nonEmpty) {
          val regex = keywordRegex(keyword)
          val paragraphsWithMatchers = candidateParagraphs.map(p => (regex.pattern.matcher(p.html), p)).find(_._1.matches())

          paragraphsWithMatchers.foreach { case (matcher, p) =>
            val tagLink = doc.createElement("a")
            tagLink.attr("href", LinkTo(keyword.metadata.url, edition))
            tagLink.text(keyword.name)
            tagLink.attr("data-link-name", "auto-linked-tag")
            tagLink.attr("data-component", "auto-linked-tag")
            tagLink.addClass("u-underline")
            val tagLinkHtml = tagLink.toString
            val newHtml = matcher.replaceFirst(s"$group1$group2$tagLinkHtml$group4$group5")
            p.html(newHtml)
          }
        }
      }
    }
    doc
  }
}

object InBodyElementCleaner extends HtmlCleaner {

  private val supportedElements = Set(
    "element-tweet",
    "element-video",
    "element-image",
    "element-witness",
    "element-comment",
    "element-interactive"
  )

  override def clean(document: Document): Document = {
    // this code REMOVES unsupported embeds
    if(ShowAllArticleEmbedsSwitch.isSwitchedOff) {
      val embeddedElements = document.getElementsByTag("figure").filter(_.hasClass("element"))
      val unsupportedElements = embeddedElements.filterNot(e => supportedElements.exists(e.hasClass))
      unsupportedElements.foreach(_.remove())
    }
    document
  }
}

case class Summary(amount: Int) extends HtmlCleaner {
  override def clean(document: Document): Document = {
    val children = document.body().children().toList
    val para: Option[Element] = children.filter(_.nodeName() == "p").take(amount).lastOption
    // if there is are no p's, just take the first n things (could be a blog)
    para match {
      case Some(p) => children.drop(children.indexOf(p)).foreach(_.remove())
      case _ => children.drop(amount).foreach(_.remove())
    }
    document
  }
}

case class ImmersiveLinks(isImmersive: Boolean) extends HtmlCleaner {
  override def clean(document: Document): Document = {
    if(isImmersive) {
      document.getElementsByTag("a").foreach{ a =>
        a.addClass("in-body-link--immersive")
      }
    }
    document
  }
}

case class ImmersiveHeaders(isImmersive: Boolean) extends HtmlCleaner {
  override def clean(document: Document): Document = {
    if(isImmersive) {
      document.getElementsByTag("h2").foreach{ h2 =>
        val beforeH2 = h2.previousElementSibling()
        if (beforeH2 != null) {
          if(beforeH2.hasClass("element--immersive element-image")) {
            beforeH2.addClass("section-image")
            beforeH2.prepend("""<h2 class="section-title">""" + h2.text() + "</h2>")
            h2.remove()
          }
        }
      }
    }
    document
  }
}

case class DropCaps(isFeature: Boolean, isImmersive: Boolean) extends HtmlCleaner {
  private def setDropCap(p: Element): String = {
    p.html.replaceFirst(
      "^([\"'“‘]*[a-zA-Z])(.{199,})",
      """<span class="drop-cap"><span class="drop-cap__inner">$1</span></span>$2"""
    )
  }

  override def clean(document: Document): Document = {
    if(isFeature) {
      val children = document.body().children().toList
      children.headOption match {
        case Some(p) => {
          if (p.nodeName() == "p") p.html(setDropCap(p))
        }
        case _ =>
      }
    }

    document.getElementsByTag("h2").foreach{ h2 =>
        if (isImmersive && h2.text() == "* * *") {
            h2.before("""<hr class="section-rule" />""")
            val next = h2.nextElementSibling()
            if (next.nodeName() == "p") {
                next.html(setDropCap(next))
            }
            h2.remove()
        }
    }
    document
  }
}

object FigCaptionCleaner extends HtmlCleaner {
  override def clean(document: Document): Document = {
    document.getElementsByTag("figcaption").foreach{ _.addClass("caption caption--img")}
    document
  }
}

object MainFigCaptionCleaner extends HtmlCleaner {
  override def clean(document: Document): Document = {
    document.getElementsByTag("figcaption").foreach{ _.addClass("caption caption--img caption--main")}
    document
  }
}

object RichLinkCleaner extends HtmlCleaner {
  override def clean(document: Document): Document = {
    val richLinks = document.getElementsByClass("element-rich-link")
    richLinks
      .addClass("element-rich-link--not-upgraded")
      .attr("data-component", "rich-link")
      .zipWithIndex.map{ case (el, index) => el.attr("data-link-name", s"rich-link-${richLinks.length} | ${index+1}") }

    document
  }
}

object MembershipEventCleaner extends HtmlCleaner {
    override def clean(document: Document): Document = {
      val membershipEvents = document.getElementsByClass("element-membership")
      membershipEvents
        .addClass("element-membership--not-upgraded")
        .attr("data-component", "membership-events")
        .zipWithIndex.map{ case (el, index) => el.attr("data-link-name", s"membership-event-${membershipEvents.length} | ${index+1}") }

      document
    }
}

object ChaptersLinksCleaner extends HtmlCleaner {
  def slugify(text: String): String = {
    Normalizer.normalize(text, Normalizer.Form.NFKD)
      .toLowerCase
      .replaceAll("[^0-9a-z ]", "")
      .trim.replaceAll(" +", "-")
  }

  override def clean(document: Document): Document = {
    val autoaChapters = document.getElementsByClass("auto-chapter")

    autoaChapters.foreach { ch =>
      val h2 = ch.getElementsByTag("h2")
      h2.attr("id", slugify(h2.text()))
    }
    document
  }
}

case class AtomsCleaner(atoms: Option[Atoms])(implicit val request: RequestHeader) extends HtmlCleaner {
  private def findAtom(id: String): Option[Atom] = {
    atoms.flatMap(_.all.find(_.id == id))
  }

  override def clean(document: Document): Document = {
    if (UseAtomsSwitch.isSwitchedOn) {
      for {
        atomContainer <- document.getElementsByClass("element-atom")
        bodyElement <- atomContainer.getElementsByTag("gu-atom")
        atomId <- Some(bodyElement.attr("data-atom-id"))
        atomData <- findAtom(atomId)
      } {
        val html = views.html.fragments.atoms.atom(atomData).toString()
        bodyElement.remove()
        atomContainer.append(html)
      }
    }
    document
  }
}
