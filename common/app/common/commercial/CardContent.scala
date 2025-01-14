package common.commercial

import conf.switches.Switches
import model.pressed.PressedContent
import model.{ContentType, ImageOverride, ImageMedia}
import views.support.{CardWithSponsorDataAttributes, ImgSrc, SponsorDataAttributes}

case class CardContent(
                        icon: Option[String],
                        headline: String,
                        kicker: Option[String],
                        description: Option[String],
                        image: Option[ImageMedia],
                        fallbackImageUrl: Option[String],
                        targetUrl: String,
                        group: Option[DynamicGroup],
                        branding: Option[SponsorDataAttributes]
                      )

object CardContent {

  def fromPressedContent(content: PressedContent): CardContent = {

    val header = content.header

    val image = {
      val properties = content.properties
      val maybeContent = properties.maybeContent
      lazy val videoImageMedia = maybeContent flatMap (_.elements.mainVideo.map(_.images))
      lazy val imageOverride = properties.image flatMap ImageOverride.createImageMedia
      lazy val defaultTrailPicture = maybeContent flatMap (_.trail.trailPicture)
      videoImageMedia.orElse(imageOverride).orElse(defaultTrailPicture)
    }

    var fallbackImageUrl = image flatMap {
      ImgSrc.getFallbackUrl
    }

    CardContent(
      icon = {
        if (header.isVideo) Some("video-icon")
        else if (header.isGallery) Some("camera")
        else if (header.isAudio) Some("volume-high")
        else None
      },
      headline = header.headline,
      kicker = content.header.kicker flatMap (_.properties.kickerText),
      description = content.card.trailText,
      image,
      fallbackImageUrl,
      targetUrl = header.url,
      group = content.card.group match {
        case "3" => Some(HugeGroup)
        case "2" => Some(VeryBigGroup)
        case "1" => Some(BigGroup)
        case _ => None
      },
      branding = {
        if (Switches.cardsDecidePaidContainerBranding.isSwitchedOn) {
          CardWithSponsorDataAttributes.sponsorDataAttributes(content)
        } else {
          None
        }
      }
    )
  }

  def fromContentItem(item: ContentType, clickMacro: Option[String]): CardContent = {
    val tags = item.tags
    CardContent(
      icon = {
        if (tags.isVideo) Some("video-icon")
        else if (tags.isGallery) Some("camera")
        else if (tags.isAudio) Some("volume-high")
        else None
      },
      headline = item.trail.headline,
      kicker = None,
      description = item.fields.trailText,
      image = item.trail.trailPicture,
      fallbackImageUrl = item.trail.trailPicture flatMap ImgSrc.getFallbackUrl,
      targetUrl = {
        val url = item.metadata.url
        clickMacro map { cm => s"$cm$url" } getOrElse url
      },
      group = None,
      branding = None
    )
  }
}

sealed trait DynamicGroup

case object HugeGroup extends DynamicGroup

case object VeryBigGroup extends DynamicGroup

case object BigGroup extends DynamicGroup
