package be.endevops.xml

import com.fasterxml.jackson.annotation.JsonRootName
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty
import tools.jackson.dataformat.xml.annotation.JacksonXmlText


// POJOs for Manage Business Identifier SOAP operations

@JsonRootName(value = "ParticipantIdentifier")
data class ParticipantIdentifierPojo(
    @param:JacksonXmlText @param:JacksonXmlProperty(localName = "text") val identifier: String = "",
    @field:JacksonXmlProperty(localName = "scheme", isAttribute = true) val scheme: String = "iso6523-actorid-upis",
)

data class CreateParticipantIdentifierRequestPojo(
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID") val serviceMetadataPublisherID: String,
    @param:JacksonXmlProperty(localName = "ParticipantIdentifier") val participantIdentifier: ParticipantIdentifierPojo,
)

data class DeleteParticipantIdentifierRequestPojo(
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID") val serviceMetadataPublisherID: String,
    @param:JacksonXmlProperty(localName = "ParticipantIdentifier") val participantIdentifier: ParticipantIdentifierPojo,
)

data class PageRequestPojo(
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID") val serviceMetadataPublisherID: String,
)

data class PageResponsePojo(
    @param:JacksonXmlProperty(localName = "ParticipantIdentifier") @param:JacksonXmlElementWrapper(useWrapping = false) val participantIdentifier: List<ParticipantIdentifierPojo>,
)

data class CreateListRequestPojo(
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID") val serviceMetadataPublisherID: String,
    @param:JacksonXmlProperty(localName = "ParticipantIdentifier") @param:JacksonXmlElementWrapper(useWrapping = false) val participantIdentifier: List<ParticipantIdentifierPojo>,
)

data class DeleteListRequestPojo(
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID") val serviceMetadataPublisherID: String,
    @param:JacksonXmlProperty(localName = "ParticipantIdentifier") @param:JacksonXmlElementWrapper(useWrapping = false) val participantIdentifier: List<ParticipantIdentifierPojo>,
)

data class PrepareMigrationRecordRequestPojo(
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID") val serviceMetadataPublisherID: String,
    @param:JacksonXmlProperty(localName = "ParticipantIdentifier") val participantIdentifier: ParticipantIdentifierPojo,
    @param:JacksonXmlProperty(localName = "MigrationKey") val migrationKey: String,
)

data class CompleteMigrationRecordRequestPojo(
    @param:JacksonXmlProperty(localName = "MigrationKey") val migrationKey: String,
    @param:JacksonXmlProperty(localName = "ServiceMetadataPublisherID") val serviceMetadataPublisherID: String,
    @param:JacksonXmlProperty(localName = "ParticipantIdentifier") val participantIdentifier: ParticipantIdentifierPojo,
)
