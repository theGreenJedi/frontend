package common.commercial

import model.facia.PressedCollection
import model.pressed.PressedContent

case class ContainerModel(
                           id: String,
                           content: ContainerContent,
                           metaData: ContainerMetaData
                         )

case class ContainerContent(
                             title: String,
                             description: String,
                             targetUrl: String,
                             cardContents: Seq[CardContent]
                           )

case class ContainerMetaData(
                              showTags: Boolean,
                              showSections: Boolean,
                              hideKickers: Boolean,
                              showDateHeader: Boolean,
                              showLatestUpdate: Boolean,
                              showTimestamps: Boolean,
                              hideShowMore: Boolean,
                              layoutName: String
                            )

case class CardContent(
                        headline: String,
                        description: Option[String],
                        imageUrl: Option[String],
                        targetUrl: Option[String]
)

object CardContent {

  def fromPressedContent(content: PressedContent): CardContent = {
    CardContent(
      headline = content.properties.webTitle,
      description = None,
      // todo: this is probably wrong size and not suitable for video etc
      // todo: see facia_cards.image.scala.html for props need to pass through
      imageUrl = content.properties.maybeContent.flatMap(_.elements.mainPicture.flatMap(_.images.largestImageUrl)),
      // todo: is weburl just prod?
      targetUrl = content.properties.webUrl
    )
  }
}

object ContainerModel {

  def fromPressedCollection(collection: PressedCollection): ContainerModel = {

    // todo: dedup curated and backfill
    val cardContents = collection.curated map CardContent.fromPressedContent

    val content = ContainerContent(
      title = collection.displayName,
      description = "",
      targetUrl = "",
      cardContents
    )

    val metaData = ContainerMetaData(
      showTags = false,
      showSections = false,
      hideKickers = false,
      showDateHeader = false,
      showLatestUpdate = false,
      showTimestamps = false,
      hideShowMore = false,
      layoutName = ""
    )

    ContainerModel(id = collection.id, content, metaData)
  }
}
