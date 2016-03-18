package common.commercial

import model.facia.PressedCollection
import model.pressed.{CuratedContent, PressedContent, CollectionConfig}
import org.scalatest.{FlatSpec, Matchers}

class PressedCollectionContainerTest extends FlatSpec with Matchers {

  "A PressedCollection" should "be marshalled into a ContainerContent" in {

    val pressedConfig: CollectionConfig = new CollectionConfig(
      displayName = Some("test-collection-displayName"),
      backfill = None,
      collectionType = "fixed/small/slow-III",
      href = Some("/am-resorts-partner-zone/2016/jan/20/be-a-hero-on-the-half-shell-release-baby-turtles-on-your-next-vacation"),
      description = None,
      groups = None,
      uneditable = false,
      showTags = false,
      showSections = false,
      hideKickers = false,
      showDateHeader = false,
      showLatestUpdate = false,
      excludeFromRss = false,
      showTimestamps = false,
      hideShowMore = false
    )

    val curatedContent = List(
      CuratedContent.make(???),
      CuratedContent.make(???),
      CuratedContent.make(???)
    )

    val backfillContent = List(
      PressedContent.make(???),
      PressedContent.make(???),
      PressedContent.make(???)
    )

    val treatsContent = List(
      PressedContent.make(???),
      PressedContent.make(???),
      PressedContent.make(???)
    )

    val pressedCollection: PressedCollection = new PressedCollection(
      id = "test-collection-id",
      displayName = pressedConfig.displayName.get,
      curated = curatedContent,
      backfill = backfillContent,
      treats = treatsContent,
      lastUpdated = None,
      updatedBy = None,
      updatedEmail = None,
      href = pressedConfig.href,
      description = pressedConfig.description,
      apiQuery = None,
      collectionType = pressedConfig.collectionType,
      groups = pressedConfig.groups,
      uneditable = pressedConfig.uneditable,
      showTags = pressedConfig.showTags,
      showSections = pressedConfig.showSections,
      hideKickers = pressedConfig.hideKickers,
      showDateHeader = pressedConfig.showDateHeader,
      showLatestUpdate = pressedConfig.showLatestUpdate,
      config = pressedConfig
    )

    val container = ContainerModel.fromPressedCollection(pressedCollection)

    "The PressedCollection basic attributes" should "match those of the ContainerContent" in {
      (container.content, pressedCollection) match {
        case (cont, collection) =>
          cont.id should be(collection.id)
          cont.title should be(collection.displayName)
          cont.description should be(collection.description)
          cont.targetUrl should be(collection.href)
      }
    }

    "The PressedCollection MetaData attributes" should "match those of the ContainerContent.MetaData" in {
      (container.metaData, pressedCollection, pressedCollection.config) match {
        case (metaData, collection, config) =>
          metaData.uneditable       should be (collection.uneditable)
          metaData.showTags         should be (collection.showTags)
          metaData.showSections     should be (collection.showSections)
          metaData.hideKickers      should be (collection.hideKickers)
          metaData.showDateHeader   should be (collection.showDateHeader)
          metaData.showLatestUpdate should be (collection.showLatestUpdate)
          metaData.excludefromRss   should be (config.excludeFromRss)
          metaData.showTimestamps   should be (config.showTimestamps)
          metaData.hideShowMore     should be (config.hideShowMore)
          metaData.layoutName       should be (collection.collectionType)
          metaData.groups           should be (collection.groups)
      }
    }

    "The PressedContents from the curated, backfill and treats collections" should
    "exist as cards in the container cardContents" in {

      val collections = (pressedCollection.curated ++ pressedCollection.backfill ++ pressedCollection.treats).distinct

      collections.foreach { content =>
        val card = container.content.cardContents.find(_.headline == content.header)
        card should not be None
      }
    }
  }

}
