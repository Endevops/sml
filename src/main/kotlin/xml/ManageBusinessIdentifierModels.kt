package be.endevops.xml

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

const val IDENTIFIER_NS = "http://busdox.org/transport/identifiers/1.0/"

@Serializable
@XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
data class ParticipantIdentifierPojo(
    @XmlValue val identifier: String = "",
    @XmlSerialName(value = "scheme") @XmlElement(value = false) val scheme: String = "iso6523-actorid-upis",
)

@Serializable
data class CreateParticipantIdentifierRequestPojo(
    @XmlSerialName(
        value = "ServiceMetadataPublisherID",
        namespace = IDENTIFIER_NS,
    )
    @XmlElement(value = true) val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS)
    @XmlElement(value = true) val participantIdentifier: ParticipantIdentifierPojo,
)

@Serializable
data class DeleteParticipantIdentifierRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = IDENTIFIER_NS) val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS) val participantIdentifier: ParticipantIdentifierPojo,
)

@Serializable
data class PageRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = IDENTIFIER_NS) val serviceMetadataPublisherID: String,
)

@Serializable
data class PageResponsePojo(
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS) val participantIdentifier: List<ParticipantIdentifierPojo>,
)

@Serializable
data class CreateListRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = IDENTIFIER_NS) val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS) val participantIdentifier: List<ParticipantIdentifierPojo>,
)

@Serializable
data class DeleteListRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = IDENTIFIER_NS) val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS) val participantIdentifier: List<ParticipantIdentifierPojo>,
)

@Serializable
data class PrepareMigrationRecordRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = IDENTIFIER_NS) val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS) val participantIdentifier: ParticipantIdentifierPojo,
    @XmlElement(value = true)
    @XmlSerialName(value = "MigrationKey", namespace = IDENTIFIER_NS) val migrationKey: String,
)

@Serializable
data class CompleteMigrationRecordRequestPojo(
    @XmlElement(value = true)
    @XmlSerialName(value = "MigrationKey", namespace = IDENTIFIER_NS) val migrationKey: String,
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = IDENTIFIER_NS) val serviceMetadataPublisherID: String,
    @XmlSerialName(value = "ParticipantIdentifier", namespace = IDENTIFIER_NS) val participantIdentifier: ParticipantIdentifierPojo,
)
