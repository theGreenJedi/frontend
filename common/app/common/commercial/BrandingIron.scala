package common.commercial

import common.Edition
import model.{Branding, TagProperties}
import org.joda.time.DateTime

object BrandingIron {

  def brand(tagProperties: Seq[TagProperties],
            publicationDate: Option[DateTime],
            edition: Edition): Option[Branding] = {

    def findBrandingBySection(): Option[Branding] = None

    def findBrandingByTag(): Option[Branding] = {
      val brandings = tagProperties.flatMap(_.activeBrandings).flatten
      brandings find (_.isTargeting(publicationDate, edition))
    }

    findBrandingBySection() orElse findBrandingByTag()
  }
}
