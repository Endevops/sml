package be.endevops.xml

import org.intellij.lang.annotations.Language
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals

class PrepareMigrationRecordRequestPojoTest {
    @Language("XML")
    val xml = """<PrepareMigrationRecord xmlns="http://busdox.org/serviceMetadata/locator/1.0/"
                        xmlns:ns2="http://busdox.org/transport/identifiers/1.0/">
    <ServiceMetadataPublisherID>ENDEVOPS-LOCAL-SMP</ServiceMetadataPublisherID>
    <ns2:ParticipantIdentifier scheme="iso6523-actorid-upis">9915:test</ns2:ParticipantIdentifier>
    <MigrationKey>iK1^lB1@!UZwQA1Eod~wt1zR</MigrationKey>
</PrepareMigrationRecord>
    """

    @Test
    fun testDeserialize() {
        val pojo = XmlMapper.builder().nameForTextElement("text").findAndAddModules().build()
            .readValue<PrepareMigrationRecordRequestPojo>(xml)
        assertEquals(pojo.serviceMetadataPublisherID , "ENDEVOPS-LOCAL-SMP")
        assertEquals(pojo.participantIdentifier.identifier , "9915:test")
        assertEquals(pojo.participantIdentifier.scheme , "iso6523-actorid-upis")
        assertEquals(pojo.migrationKey , "iK1^lB1@!UZwQA1Eod~wt1zR")
    }
}

class CompleteMigrationRecordRequestPojoTest {
    @Language("XML")
    val xml = """<CompleteMigrationRecord xmlns="http://busdox.org/serviceMetadata/locator/1.0/"
                         xmlns:ns2="http://busdox.org/transport/identifiers/1.0/">
    <ServiceMetadataPublisherID>ENDEVOPS-LOCAL-SMP</ServiceMetadataPublisherID>
    <ns2:ParticipantIdentifier scheme="iso6523-actorid-upis">9915:test</ns2:ParticipantIdentifier>
    <MigrationKey>eN0{jB3{FpC(m+^zj=T%PW)C</MigrationKey>
</CompleteMigrationRecord>
    """

    @Test
    fun testDeserialize() {
        val pojo = XmlMapper.builder().nameForTextElement("text").findAndAddModules().build()
            .readValue<CompleteMigrationRecordRequestPojo>(xml)
        assertEquals(pojo.migrationKey , "eN0{jB3{FpC(m+^zj=T%PW)C")
        assertEquals(pojo.serviceMetadataPublisherID, "ENDEVOPS-LOCAL-SMP")
        assertEquals(pojo.participantIdentifier.scheme , "iso6523-actorid-upis")
        assertEquals(pojo.participantIdentifier.identifier , "9915:test")
    }
}