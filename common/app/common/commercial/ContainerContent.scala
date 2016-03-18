package common.commercial

case class ContainerContent(
  title: String,
  targetUrl: String,
  trails: Seq[CommercialTrail]
)

case class CommercialTrail(
  headline: String,
  description: Option[String],
  imageUrl: Option[String],
  targetUrl: String
)
