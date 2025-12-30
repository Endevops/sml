package be.endevops

import kotlinx.serialization.Serializable

@Serializable
data class PublisherEndpointSerializable(
    val LogicalAddress: String,
    val PhysicalAddress: String,
)

@Serializable
data class CreateServiceMetadataPublisherServiceRequestSerializable(
    val PublisherEndpoint: PublisherEndpointSerializable? = null,
    val ServiceMetadataPublisherID: String? = null,
)

@Serializable
data class CreateServiceMetadataPublisherServiceResponseSerializable(
    val Result: String,
    val DatabaseId: Int? = null,
    val FaultMessage: String? = null,
)

@Serializable
data class ReadServiceMetadataPublisherServiceResponseSerializable(
    val Result: String,
    val ServiceMetadataPublisherID: String? = null,
    val PublisherEndpoint: PublisherEndpointSerializable? = null,
    val FaultMessage: String? = null,
)

@Serializable
data class UpdateDeleteServiceResponseSerializable(
    val Result: String,
    val FaultMessage: String? = null,
)
