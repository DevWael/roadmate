package com.roadmate.core.model.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed interface representing Bluetooth sync protocol messages.
 *
 * Defines the four message types exchanged between head unit and phone:
 * - [SyncStatus]: Device identification and last-sync timestamp
 * - [SyncPull]: Request for changes since a given timestamp
 * - [SyncPush]: Payload of entity changes to apply
 * - [SyncAck]: Acknowledgement of received sync data
 *
 * Each subtype carries a [@SerialName] discriminator to enable polymorphic
 * deserialization when the concrete type isn't known at parse time.
 */
@Serializable
sealed interface SyncMessage {

    @Serializable
    @SerialName("sync_status")
    data class SyncStatus(
        val deviceId: String,
        val timestamp: Long,
        val lastSyncTimestamp: Long,
    ) : SyncMessage

    @Serializable
    @SerialName("sync_pull")
    data class SyncPull(
        val since: Long,
        val entityTypes: List<String>,
    ) : SyncMessage

    @Serializable
    @SerialName("sync_push")
    data class SyncPush(
        val entityType: String,
        val data: String,
        val messageId: String,
        val timestamp: Long,
    ) : SyncMessage

    @Serializable
    @SerialName("sync_ack")
    data class SyncAck(
        val success: Boolean,
        val messageId: String,
        val timestamp: Long,
        val message: String?,
    ) : SyncMessage
}
