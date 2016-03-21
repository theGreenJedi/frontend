package common.commercial

import model.facia.PressedCollection
import model.pressed.PressedContent
import views.support.{CardWithSponsorDataAttributes, SponsorDataAttributes}

case class ContainerModel(
                           id: String,
                           content: ContainerContent,
                           metaData: ContainerMetaData
                         )

case class ContainerContent(
                             title: String,
                             description: Option[String],
                             targetUrl: Option[String],
                             cardContents: Seq[CardContent]
                           )

case class ContainerMetaData(
                              sponsorData: Option[SponsorDataAttributes],
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
                        targetUrl: Option[String],
                        sponsorData: Option[SponsorDataAttributes]
)

object CardContent {

  def fromPressedContent(content: PressedContent): CardContent = {

    val metaDataCommercial: Option[SponsorDataAttributes] =
      CardWithSponsorDataAttributes.sponsorDataAttributes(content)

    CardContent(
      headline = content.properties.webTitle,
      description = content.card.trailText,
      // todo: this is probably wrong size and not suitable for video etc
      // todo: see facia_cards.image.scala.html for props need to pass through
      imageUrl = content.properties.maybeContent.flatMap(_.elements.mainPicture.flatMap(_.images.largestImageUrl)),
      // todo: is weburl just prod?
      targetUrl = content.properties.webUrl,
      sponsorData = metaDataCommercial
    )
  }
}

object ContainerModel {

  def fromPressedCollection(collection: PressedCollection): ContainerModel = {

    val cardContents = collection.curatedPlusBackfillDeduplicated map CardContent.fromPressedContent

    val singleSponsorContainer = cardContents.forall(card => card.sponsorData == cardContents.head.sponsorData)
    val maybeSponsorDataAttributes = if (singleSponsorContainer) cardContents.head.sponsorData else None

    val content = ContainerContent(
      title = collection.displayName,
      description = collection.description,
      targetUrl = collection.href,
      cardContents
    )

    val metaData = ContainerMetaData(
      sponsorData = maybeSponsorDataAttributes,
      showTags = collection.showTags,
      showSections = collection.showSections,
      hideKickers = collection.hideKickers,
      showDateHeader = collection.showDateHeader,
      showLatestUpdate = collection.showLatestUpdate,
      showTimestamps = collection.config.showTimestamps,
      hideShowMore = collection.config.hideShowMore,
      layoutName = collection.collectionType)

    ContainerModel(id = collection.id, content, metaData)
  }
}
