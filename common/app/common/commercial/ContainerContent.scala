package common.commercial

import model.facia.PressedCollection

case class ContainerContent(
  title: String,
  targetUrl: String,
  trails: Seq[CommercialTrail]
)

object ContainerContent {

  def fromPressedCollection(collection: PressedCollection): ContainerContent = ???
}

case class CommercialTrail(
  headline: String,
  description: Option[String],
  imageUrl: Option[String],
  targetUrl: String
)
