package be.endevops.svc

import io.ktor.server.plugins.di.annotations.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import org.xbill.DNS.*

@Serializable
data class DnsConfiguration(
    @Property("dns.server") val server: String,
    @Property("dns.port") val port: Int,
    @Property("dns.keyname") val keyname: String,
    @Property("dns.keysecret") val keysecret: String,
)

class DnsClient(
    internal val dnsConfiguration: DnsConfiguration,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DnsClient::class.java)!!
    }

    fun addCNameRecord(
        zone: String,
        name: String,
        ttl: Long,
        canonicalName: String,
    ) {
        val update = Update(zoneName(zone))
        logger.info("Preparing to add CNAME record: {} {} {} {}", name, ttl, canonicalName, zone)
        update.add(cNAMERecord(name, zone, ttl, canonicalName))
        sendUpdate(update)
    }

    fun updateCNameRecord(
        zone: String,
        name: String,
        ttl: Long,
        canonicalName: String,
    ) {
        val update = Update(zoneName(zone))
        logger.info("Preparing to update CNAME record: {} {} {} {}", name, ttl, canonicalName, zone)
        update.replace(cNAMERecord(name, zone, ttl, canonicalName))
        sendUpdate(update)
    }

    private fun cNAMERecord(
        name: String,
        zone: String,
        ttl: Long,
        canonicalName: String,
    ): CNAMERecord = CNAMERecord(fqdn(name, zone), DClass.IN, ttl, Name.fromString(canonicalName, Name.root))

    fun addNaptrRecord(
        zone: String,
        name: String,
        ttl: Long,
        order: Int,
        preference: Int,
        flags: String,
        service: String,
        regexp: String,
        replacement: String,
    ) {
        val update = Update(zoneName(zone))
        logger.info(
            "Preparing to add NAPTR record: {} {} {} {} {} {} {} {}",
            name,
            ttl,
            order,
            preference,
            flags,
            service,
            regexp,
            replacement,
        )
        update.add(
            nAPTRRecord(name, zone, ttl, order, preference, flags, service, regexp, replacement),
        )
        sendUpdate(update)
    }

    fun updateNaptrRecord(
        zone: String,
        name: String,
        ttl: Long,
        order: Int,
        preference: Int,
        flags: String,
        service: String,
        regexp: String,
        replacement: String,
    ) {
        val update = Update(zoneName(zone))
        logger.info(
            "Preparing to update NAPTR record: {} {} {} {} {} {} {} {}",
            name,
            ttl,
            order,
            preference,
            flags,
            service,
            regexp,
            replacement,
        )
        update.replace(
            nAPTRRecord(name, zone, ttl, order, preference, flags, service, regexp, replacement),
        )
        sendUpdate(update)
    }

    private fun nAPTRRecord(
        name: String,
        zone: String,
        ttl: Long,
        order: Int,
        preference: Int,
        flags: String,
        service: String,
        regexp: String,
        replacement: String,
    ): NAPTRRecord =
        NAPTRRecord(
            fqdn(name, zone),
            DClass.IN,
            ttl,
            order,
            preference,
            flags,
            service,
            regexp,
            Name(replacement, Name.root),
        )

    fun deleteCNameRecord(
        zone: String,
        name: String,
    ) {
        val update = Update(zoneName(zone))
        logger.info("Preparing to delete CNAME record: {} {}", name, zone)
        update.delete(fqdn(name, zone), Type.CNAME)
        sendUpdate(update)
    }

    fun deleteNaptrRecord(
        zone: String,
        name: String,
    ) {
        val update = Update(zoneName(zone))
        logger.info("Preparing to delete NAPTR record: {} {}", name, zone)
        update.delete(fqdn(name, zone), Type.NAPTR)
        sendUpdate(update)
    }

    private fun sendUpdate(update: Update) {
        logger.info("resolve dns server {}, port {}", dnsConfiguration.server, dnsConfiguration.port)

        val resolver = SimpleResolver(dnsConfiguration.server)
        resolver.port = dnsConfiguration.port
        resolver.tcp = false
        resolver.tsigKey = TSIG(TSIG.HMAC_SHA256, dnsConfiguration.keyname, dnsConfiguration.keysecret)

        val response = resolver.send(update)
        logger.debug("dns response: {}", response)

        logger.info(
            "got response from dns server: {} {} {} {}",
            response.header,
            response.header.rcode,
            dnsConfiguration.server,
            dnsConfiguration.port,
        )
        if (response.header.rcode != Rcode.NOERROR) {
            throw BindException(message = "Bind response: ${response.header}")
        }
    }
}

class BindException(
    message: String,
) : RuntimeException(message)

private fun fqdn(
    name: String,
    zone: String,
): Name {
    val fq = if (name.endsWith(zone)) name else "$name.$zone"
    return Name.fromString(fq, Name.root)
}

private fun zoneName(zone: String): Name = Name.fromString(zone, Name.root)
