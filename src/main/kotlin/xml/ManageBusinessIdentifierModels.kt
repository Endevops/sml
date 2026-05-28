package be.endevops.xml

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlIgnoreWhitespace
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
data class ParticipantIdentifierType(
    @XmlValue
    @XmlIgnoreWhitespace
    val identifier: String = "",
    @XmlSerialName(value = "scheme")
    @XmlElement(value = false)
    val scheme: String = "iso6523-actorid-upis",
)

@Serializable
@XmlSerialName(value = "CreateParticipantIdentifier", namespace = SERVICE_METADATA_LOCATOR_NS)
data class CreateParticipantIdentifierRequestPojo(
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
    @XmlElement(value = true)
    val participantIdentifier: ParticipantIdentifierType,
)

@Serializable
data class DeleteParticipantIdentifierRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
    val participantIdentifier: ParticipantIdentifierType,
)

@Serializable
data class PageRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    val serviceMetadataPublisherID: String,
)

@Serializable
@XmlSerialName(value = "ParticipantIdentifierPage", SERVICE_METADATA_LOCATOR_NS)
data class PageResponsePojo(
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
    val participantIdentifier: List<ParticipantIdentifierType>,
)

@Serializable
@XmlSerialName(value = "CreateList", SERVICE_METADATA_LOCATOR_NS)
data class CreateListRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
    val participantIdentifier: List<ParticipantIdentifierType>,
)

@Serializable
@XmlSerialName(value = "DeleteList", SERVICE_METADATA_LOCATOR_NS)
data class DeleteListRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
    val participantIdentifier: List<ParticipantIdentifierType>,
)

@Serializable
@XmlSerialName(value = "PrepareMigrationRecord", namespace = SERVICE_METADATA_LOCATOR_NS)
data class MigrationRecordType(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
    val participantIdentifier: ParticipantIdentifierType,
    @XmlElement(value = true)
    @XmlSerialName(value = "MigrationKey", namespace = SERVICE_METADATA_LOCATOR_NS)
    val migrationKey: String,
)

@Serializable
@XmlSerialName(value = "CompleteMigrationRecord", namespace = SERVICE_METADATA_LOCATOR_NS)
data class CompleteMigrationRecordRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "MigrationKey", namespace = IDENTIFIER_NS)
    val migrationKey: String,
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
    val participantIdentifier: ParticipantIdentifierType,
)
