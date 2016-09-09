package mesosphere.marathon.core.event

import akka.event.EventStream
import mesosphere.marathon.core.health.HealthCheck
import mesosphere.marathon.core.instance.{ InstanceStatus, Instance }
import mesosphere.marathon.state.{ AppDefinition, PathId, Timestamp }
import mesosphere.marathon.upgrade.{ DeploymentPlan, DeploymentStep }

import scala.collection.immutable.Seq

//scalastyle:off number.of.types

sealed trait MarathonEvent {
  val eventType: String
  val timestamp: String
}

// api

case class ApiPostEvent(
  clientIp: String,
  uri: String,
  appDefinition: AppDefinition,
  eventType: String = "api_post_event",
  timestamp: String = Timestamp.now().toString) extends MarathonEvent

case class PodEvent(
    clientIp: String,
    uri: String,
    podEventType: PodEvent.Kind,
    timestamp: String = Timestamp.now().toString) extends MarathonEvent {
  override val eventType = podEventType.label
}

object PodEvent {
  sealed trait Kind {
    val label: String
  }
  case object Created extends Kind {
    val label = "pod_created_event"
  }
  case object Updated extends Kind {
    val label = "pod_updated_event"
  }
  case object Deleted extends Kind {
    val label = "pod_deleted_event"
  }
}

// scheduler messages
sealed trait MarathonSchedulerEvent extends MarathonEvent

final case class SchedulerRegisteredEvent(
  frameworkId: String,
  master: String,
  eventType: String = "scheduler_registered_event",
  timestamp: String = Timestamp.now().toString)
    extends MarathonSchedulerEvent

final case class SchedulerReregisteredEvent(
  master: String,
  eventType: String = "scheduler_reregistered_event",
  timestamp: String = Timestamp.now().toString)
    extends MarathonSchedulerEvent

final case class SchedulerDisconnectedEvent(
  eventType: String = "scheduler_disconnected_event",
  timestamp: String = Timestamp.now().toString)
    extends MarathonSchedulerEvent

// event subscriptions

sealed trait MarathonSubscriptionEvent extends MarathonEvent

case class Subscribe(
  clientIp: String,
  callbackUrl: String,
  eventType: String = "subscribe_event",
  timestamp: String = Timestamp.now().toString)
    extends MarathonSubscriptionEvent

case class Unsubscribe(
  clientIp: String,
  callbackUrl: String,
  eventType: String = "unsubscribe_event",
  timestamp: String = Timestamp.now().toString)
    extends MarathonSubscriptionEvent

case class EventStreamAttached(
  remoteAddress: String,
  eventType: String = "event_stream_attached",
  timestamp: String = Timestamp.now().toString) extends MarathonSubscriptionEvent

case class EventStreamDetached(
  remoteAddress: String,
  eventType: String = "event_stream_detached",
  timestamp: String = Timestamp.now().toString) extends MarathonSubscriptionEvent

// health checks

sealed trait MarathonHealthCheckEvent extends MarathonEvent {
  def appId(): PathId
}

case class AddHealthCheck(
  appId: PathId,
  version: Timestamp,
  healthCheck: HealthCheck,
  eventType: String = "add_health_check_event",
  timestamp: String = Timestamp.now().toString)
    extends MarathonHealthCheckEvent

case class RemoveHealthCheck(
  appId: PathId,
  eventType: String = "remove_health_check_event",
  timestamp: String = Timestamp.now().toString)
    extends MarathonHealthCheckEvent

case class FailedHealthCheck(
  appId: PathId,
  taskId: Instance.Id,
  healthCheck: HealthCheck,
  eventType: String = "failed_health_check_event",
  timestamp: String = Timestamp.now().toString)
    extends MarathonHealthCheckEvent

case class HealthStatusChanged(
  appId: PathId,
  taskId: Instance.Id,
  version: Timestamp,
  alive: Boolean,
  eventType: String = "health_status_changed_event",
  timestamp: String = Timestamp.now().toString)
    extends MarathonHealthCheckEvent

case class UnhealthyTaskKillEvent(
  appId: PathId,
  taskId: Instance.Id,
  version: Timestamp,
  reason: String,
  host: String,
  slaveId: Option[String],
  eventType: String = "unhealthy_task_kill_event",
  timestamp: String = Timestamp.now().toString) extends MarathonHealthCheckEvent

// upgrade messages

sealed trait UpgradeEvent extends MarathonEvent

case class GroupChangeSuccess(
  groupId: PathId,
  version: String,
  eventType: String = "group_change_success",
  timestamp: String = Timestamp.now().toString) extends UpgradeEvent

case class GroupChangeFailed(
  groupId: PathId,
  version: String,
  reason: String,
  eventType: String = "group_change_failed",
  timestamp: String = Timestamp.now().toString) extends UpgradeEvent

case class DeploymentSuccess(
  id: String,
  plan: DeploymentPlan,
  eventType: String = "deployment_success",
  timestamp: String = Timestamp.now().toString) extends UpgradeEvent

case class DeploymentFailed(
  id: String,
  plan: DeploymentPlan,
  eventType: String = "deployment_failed",
  timestamp: String = Timestamp.now().toString) extends UpgradeEvent

case class DeploymentStatus(
  plan: DeploymentPlan,
  currentStep: DeploymentStep,
  eventType: String = "deployment_info",
  timestamp: String = Timestamp.now().toString) extends UpgradeEvent

case class DeploymentStepSuccess(
  plan: DeploymentPlan,
  currentStep: DeploymentStep,
  eventType: String = "deployment_step_success",
  timestamp: String = Timestamp.now().toString) extends UpgradeEvent

case class DeploymentStepFailure(
  plan: DeploymentPlan,
  currentStep: DeploymentStep,
  eventType: String = "deployment_step_failure",
  timestamp: String = Timestamp.now().toString) extends UpgradeEvent

// Mesos scheduler

case class AppTerminatedEvent(
  appId: PathId,
  eventType: String = "app_terminated_event",
  timestamp: String = Timestamp.now().toString) extends MarathonEvent

case class MesosStatusUpdateEvent(
  slaveId: String,
  taskId: Instance.Id,
  taskStatus: String,
  message: String,
  appId: PathId,
  host: String,
  ipAddresses: Option[Seq[org.apache.mesos.Protos.NetworkInfo.IPAddress]],
  ports: Seq[Int],
  version: String,
  eventType: String = "status_update_event",
  timestamp: String = Timestamp.now().toString) extends MarathonEvent

case class InstanceChanged(
    id: Instance.Id,
    runSpecVersion: Timestamp,
    runSpecId: PathId,
    status: InstanceStatus,
    instance: Instance) extends MarathonEvent {
  override val eventType: String = "instance_changed_event"
  override val timestamp: String = Timestamp.now().toString
}

case class InstanceHealthChanged(
    id: Instance.Id,
    runSpecVersion: Timestamp,
    runSpecId: PathId,
    healthy: Boolean) extends MarathonEvent {
  override val eventType: String = "instance_health_changed_event"
  override val timestamp: String = Timestamp.now().toString
}

case class MesosFrameworkMessageEvent(
  executorId: String,
  slaveId: String,
  message: Array[Byte],
  eventType: String = "framework_message_event",
  timestamp: String = Timestamp.now().toString) extends MarathonEvent

case object Events {
  def maybePost(event: MarathonEvent)(implicit eventBus: EventStream): Unit =
    eventBus.publish(event)
}
