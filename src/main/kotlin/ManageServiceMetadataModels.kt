package be.endevops

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

// POJOs for Manage Service Metadata SOAP operations using Jackson XML annotations.
// Field localNames match the XML element names used in the existing SerializationModels and tests.

@JacksonXmlRootElement(localName = "PublisherEndpoint")
data class PublisherEndpointPojo(
    @JacksonXmlProperty(localName = "LogicalAddress")
    @JsonAlias("LogicalAddress")
    val logicalAddress: String? = null,
    @JacksonXmlProperty(localName = "PhysicalAddress")
    @JsonAlias("PhysicalAddress")
    val physicalAddress: String? = null,
)

// Create request
@JacksonXmlRootElement(localName = "CreateServiceMetadataPublisherService")
data class CreateServiceMetadataPublisherServiceRequestPojo(
    @JacksonXmlProperty(localName = "PublisherEndpoint")
    @JsonAlias("PublisherEndpoint")
    val publisherEndpoint: PublisherEndpointPojo? = null,
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
)

// Create response
@JacksonXmlRootElement(localName = "CreateServiceMetadataPublisherServiceResponse")
data class CreateServiceMetadataPublisherServiceResponsePojo(
    @JacksonXmlProperty(localName = "Result")
    @JsonAlias("Result")
    val result: String? = null,
    @JacksonXmlProperty(localName = "DatabaseId")
    @JsonAlias("DatabaseId")
    val databaseId: Int? = null,
    @JacksonXmlProperty(localName = "FaultMessage")
    @JsonAlias("FaultMessage")
    val faultMessage: String? = null,
)

// Read request
@JacksonXmlRootElement(localName = "ReadServiceMetadataPublisherService")
data class ReadServiceMetadataPublisherServiceRequestPojo(
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
)

// Read response
@JacksonXmlRootElement(localName = "ReadServiceMetadataPublisherServiceResponse")
data class ReadServiceMetadataPublisherServiceResponsePojo(
    @JacksonXmlProperty(localName = "Result")
    @JsonAlias("Result")
    val result: String? = null,
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
    @JacksonXmlProperty(localName = "PublisherEndpoint")
    @JsonAlias("PublisherEndpoint")
    val publisherEndpoint: PublisherEndpointPojo? = null,
    @JacksonXmlProperty(localName = "FaultMessage")
    @JsonAlias("FaultMessage")
    val faultMessage: String? = null,
)

// Update request (same structure as Create)
@JacksonXmlRootElement(localName = "UpdateServiceMetadataPublisherService")
data class UpdateServiceMetadataPublisherServiceRequestPojo(
    @JacksonXmlProperty(localName = "PublisherEndpoint")
    @JsonAlias("PublisherEndpoint")
    val publisherEndpoint: PublisherEndpointPojo? = null,
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
)

// Update response
@JacksonXmlRootElement(localName = "UpdateServiceMetadataPublisherServiceResponse")
data class UpdateServiceMetadataPublisherServiceResponsePojo(
    @JacksonXmlProperty(localName = "Result")
    @JsonAlias("Result")
    val result: String? = null,
    @JacksonXmlProperty(localName = "FaultMessage")
    @JsonAlias("FaultMessage")
    val faultMessage: String? = null,
)

// Delete request (same as Read request shape)
@JacksonXmlRootElement(localName = "DeleteServiceMetadataPublisherService")
data class DeleteServiceMetadataPublisherServiceRequestPojo(
    @JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
)

// Shared Update/Delete response type
@JacksonXmlRootElement(localName = "UpdateDeleteServiceResponse")
data class UpdateDeleteServiceResponsePojo(
    @JacksonXmlProperty(localName = "Result")
    @JsonAlias("Result")
    val result: String? = null,
    @JacksonXmlProperty(localName = "FaultMessage")
    @JsonAlias("FaultMessage")
    val faultMessage: String? = null,
)
