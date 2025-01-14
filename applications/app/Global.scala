import common.dfp.DfpAgentLifecycle
import common.{CloudWatchApplicationMetrics, ContentApiMetrics, EmailSubsciptionMetrics}
import conf.{CorsErrorHandler, Filters, SwitchboardLifecycle}
import contentapi.SectionsLookUpLifecycle
import dev.DevParametersLifecycle
import jobs.SiteMapLifecycle
import metrics.FrontendMetric
import ophan.SurgingContentAgentLifecycle
import play.api.mvc.WithFilters
import services.{ConfigAgentLifecycle, IndexListingsLifecycle}

object Global extends WithFilters(Filters.common: _*)
  with ConfigAgentLifecycle
  with DevParametersLifecycle
  with CloudWatchApplicationMetrics
  with DfpAgentLifecycle
  with SurgingContentAgentLifecycle
  with IndexListingsLifecycle
  with SectionsLookUpLifecycle
  with SwitchboardLifecycle
  with CorsErrorHandler
  with SiteMapLifecycle {
  override lazy val applicationName = "frontend-applications"

  override def applicationMetrics: List[FrontendMetric] = super.applicationMetrics ++ List(
    ContentApiMetrics.HttpTimeoutCountMetric,
    ContentApiMetrics.HttpLatencyTimingMetric,
    ContentApiMetrics.ContentApiErrorMetric,
    EmailSubsciptionMetrics.EmailSubmission,
    EmailSubsciptionMetrics.EmailFormError,
    EmailSubsciptionMetrics.NotAccepted,
    EmailSubsciptionMetrics.APIHTTPError,
    EmailSubsciptionMetrics.APINetworkError,
    EmailSubsciptionMetrics.ListIDError,
    EmailSubsciptionMetrics.AllEmailSubmission
  )
}
