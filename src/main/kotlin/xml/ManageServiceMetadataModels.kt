package be.endevops.xml

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonRootName
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

// POJOs for Manage Service Metadata SOAP operations using Jackson XML annotations.
// Field localNames match the XML element names used in the existing SerializationModels and tests.

@JsonRootName(value = "PublisherEndpoint")
data class PublisherEndpointPojo(
    @param:JacksonXmlProperty(localName = "LogicalAddress")
    @param:JsonAlias("LogicalAddress")
    val logicalAddress: String,
    @param:JacksonXmlProperty(localName = "PhysicalAddress")
    @param:JsonAlias("PhysicalAddress")
    val physicalAddress: String,
)

// Create request
@JsonRootName(value = "CreateServiceMetadataPublisherService")
data class CreateServiceMetadataPublisherServiceRequestPojo(
    @param:JacksonXmlProperty(localName = "PublisherEndpoint")
    @param:JsonAlias("PublisherEndpoint")
    val publisherEndpoint: PublisherEndpointPojo,
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @param:JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String,
)

// Create response
@JsonRootName(value = "CreateServiceMetadataPublisherServiceResponse")
data class CreateServiceMetadataPublisherServiceResponsePojo(
    @param:JacksonXmlProperty(localName = "Result")
    @param:JsonAlias("Result")
    val result: String? = null,
    @param:JacksonXmlProperty(localName = "DatabaseId")
    @param:JsonAlias("DatabaseId")
    val databaseId: Int? = null,
    @param:JacksonXmlProperty(localName = "FaultMessage")
    @param:JsonAlias("FaultMessage")
    val faultMessage: String? = null,
)

// Read request
@JsonRootName(value = "ReadServiceMetadataPublisherService")
data class ReadServiceMetadataPublisherServiceRequestPojo(
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @param:JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String,
)

// Read response
@JsonRootName(value = "ReadServiceMetadataPublisherServiceResponse")
data class ReadServiceMetadataPublisherServiceResponsePojo(
    @param:JacksonXmlProperty(localName = "Result")
    @param:JsonAlias("Result")
    val result: String? = null,
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @param:JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String? = null,
    @param:JacksonXmlProperty(localName = "PublisherEndpoint")
    @param:JsonAlias("PublisherEndpoint")
    val publisherEndpoint: PublisherEndpointPojo? = null,
    @param:JacksonXmlProperty(localName = "FaultMessage")
    @param:JsonAlias("FaultMessage")
    val faultMessage: String? = null,
)

// Update request (same structure as Create)
@JsonRootName(value = "UpdateServiceMetadataPublisherService")
data class UpdateServiceMetadataPublisherServiceRequestPojo(
    @param:JacksonXmlProperty(localName = "PublisherEndpoint")
    @param:JsonAlias("PublisherEndpoint")
    val publisherEndpoint: PublisherEndpointPojo,
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @param:JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String,
)

// Update response
@JsonRootName(value = "UpdateServiceMetadataPublisherServiceResponse")
data class UpdateServiceMetadataPublisherServiceResponsePojo(
    @param:JacksonXmlProperty(localName = "Result")
    @param:JsonAlias("Result")
    val result: String? = null,
    @param:JacksonXmlProperty(localName = "FaultMessage")
    @param:JsonAlias("FaultMessage")
    val faultMessage: String? = null,
)

// Delete request (same as Read request shape)
@JsonRootName(value = "DeleteServiceMetadataPublisherService")
data class DeleteServiceMetadataPublisherServiceRequestPojo(
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID")
    @param:JsonAlias("ServiceMetadataPublisherID")
    val serviceMetadataPublisherID: String,
)

// Shared Update/Delete response type
@JsonRootName(value = "UpdateDeleteServiceResponse")
data class UpdateDeleteServiceResponsePojo(
    @param:JacksonXmlProperty(localName = "Result")
    @param:JsonAlias("Result")
    val result: String? = null,
    @param:JacksonXmlProperty(localName = "FaultMessage")
    @param:JsonAlias("FaultMessage")
    val faultMessage: String? = null,
)
