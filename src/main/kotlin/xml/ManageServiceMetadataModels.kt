package be.endevops.xml

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// POJOs for Manage Service Metadata SOAP operations using Jackson XML annotations.
// Field localNames match the XML element names used in the existing SerializationModels and tests.

@Serializable
@XmlSerialName(value = "PublisherEndpoint")
data class PublisherEndpointPojo(
    @XmlSerialName(value = "LogicalAddress")
    @XmlElement(value = true)
    val logicalAddress: String,
    @XmlSerialName(value = "PhysicalAddress")
    @XmlElement(value = true)
    val physicalAddress: String,
)

// Create request
@Serializable
@XmlSerialName(value = "CreateServiceMetadataPublisherService")
data class CreateServiceMetadataPublisherServiceRequestPojo(
    @XmlSerialName(value = "PublisherEndpoint")
    @XmlElement(value = true)
    val publisherEndpoint: PublisherEndpointPojo,
    @XmlSerialName(value = "ServiceMetadataPublisherID")
    @XmlElement(value = true)
    val serviceMetadataPublisherID: String,
)

// Create response
@Serializable
@XmlSerialName(value = "CreateServiceMetadataPublisherServiceResponse")
data class CreateServiceMetadataPublisherServiceResponsePojo(
    @XmlSerialName(value = "Result")
    @XmlElement(value = true)
    val result: String? = null,
    @XmlSerialName(value = "DatabaseId")
    @XmlElement(value = true)
    val databaseId: Int? = null,
    @XmlSerialName(value = "FaultMessage")
    @XmlElement(value = true)
    val faultMessage: String? = null,
)

// Read request
@Serializable
@XmlSerialName(value = "ReadServiceMetadataPublisherService")
data class ReadServiceMetadataPublisherServiceRequestPojo(
    @XmlSerialName(value = "ServiceMetadataPublisherID")
    @XmlElement(value = true)
    val serviceMetadataPublisherID: String,
)

// Read response
@Serializable
@XmlSerialName(value = "ReadServiceMetadataPublisherServiceResponse")
data class ReadServiceMetadataPublisherServiceResponsePojo(
    @XmlSerialName(value = "Result")
    @XmlElement(value = true)
    val result: String? = null,
    @XmlSerialName(value = "ServiceMetadataPublisherID")
    @XmlElement(value = true)
    val serviceMetadataPublisherID: String? = null,
    @XmlSerialName(value = "PublisherEndpoint")
    @XmlElement(value = true)
    val publisherEndpoint: PublisherEndpointPojo? = null,
    @XmlSerialName(value = "FaultMessage")
    @XmlElement(value = true)
    val faultMessage: String? = null,
)

// Update request (same structure as Create)
@Serializable
@XmlSerialName(value = "UpdateServiceMetadataPublisherService")
data class UpdateServiceMetadataPublisherServiceRequestPojo(
    @XmlSerialName(value = "PublisherEndpoint")
    @XmlElement(value = true)
    val publisherEndpoint: PublisherEndpointPojo,
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String,
)

// Update response
@Serializable
@XmlSerialName(value = "UpdateServiceMetadataPublisherServiceResponse")
data class UpdateServiceMetadataPublisherServiceResponsePojo(
    @XmlSerialName(value = "Result")
    @XmlElement(value = true)
    val result: String? = null,
    @XmlElement(value = true)
    @XmlSerialName(value = "FaultMessage")
    val faultMessage: String? = null,
)

// Delete request (same as Read request shape)
@Serializable
@XmlSerialName(value = "DeleteServiceMetadataPublisherService")
data class DeleteServiceMetadataPublisherServiceRequestPojo(
    @XmlSerialName(value = "ServiceMetadataPublisherID")
    @XmlElement(value = true)
    val serviceMetadataPublisherID: String,
)

// Shared Update/Delete response type
@Serializable
@XmlSerialName(value = "UpdateDeleteServiceResponse")
data class UpdateDeleteServiceResponsePojo(
    @XmlSerialName(value = "Result")
    @XmlElement(value = true)
    val result: String? = null,
    @XmlSerialName(value = "FaultMessage")
    @XmlElement(value = true)
    val faultMessage: String? = null,
)
