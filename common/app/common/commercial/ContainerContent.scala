package common.commercial

import model.facia.PressedCollection
import org.joda.time.DateTime

case class ContainerModel(content: ContainerContent, metaData: ContainerMetaData)
case class ContainerContent(
                             id: String,
                             title: String,
                             description: String,
                             targetUrl: String,
                             cardContents: Seq[CardContent]
                           )
case class ContainerMetaData(
                              uneditable: Boolean,
                              showTags: Boolean,
                              showSections: Boolean,
                              hideKickers: Boolean,
                              showDateHeader: Boolean,
                              showLatestUpdate: Boolean,
                              excludefromRss: Boolean,
                              showTimestamps: Boolean,
                              hideShowMore: Boolean,
                              lastUpdated: Option[DateTime],
                              updatedBy: Option[String],
                              updatedEmail: Option[String],
                              layoutName: String,
                              groups: Option[Seq[String]]
                            )
case class CardContent(
  headline: String,
  description: Option[String],
  imageUrl: Option[String],
  targetUrl: String
)

object ContainerModel {

  def fromPressedCollection(collection: PressedCollection): ContainerModel = ???
}
