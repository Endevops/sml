package be.endevops

import io.ktor.server.plugins.di.annotations.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import org.xbill.DNS.*

@Serializable
data class DnsConfiguration(
    @Property("dns.server") val server: String,
    @Property("dns.port") val port: Int,
    @Property("dns.keyname") val dnsKeyName: String,
    @Property("dns.keysecret") val dnsKey: String,
)

class DnsClient(internal val dnsConfiguration: DnsConfiguration) {
    val logger = KtorSimpleLogger("DnsClient")
}

suspend fun DnsClient.addCNameRecord(
    zone: String,
    name: String,
    ttl: Long,
    canonicalName: String,
) {
    val update = Update(zoneName(zone))
    logger.info("Preparing to add CNAME record: {} {} {} {}", name, ttl, canonicalName, zone)
    update.add(CNAMERecord(fqdn(name, zone), DClass.IN, ttl, fqdn(canonicalName, zone)))
    sendUpdate(update)
}

suspend fun DnsClient.addNaptrRecord(
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
        NAPTRRecord(
            fqdn(name, zone),
            DClass.IN,
            ttl,
            order,
            preference,
            flags,
            service,
            regexp,
            if (replacement != ".") fqdn(replacement, zone) else Name(replacement),
        ),
    )
    sendUpdate(update)
}

suspend fun DnsClient.updateCnameRecord(
    zone: String,
    name: String,
    ttl: Long,
    canonicalName: String,
) {
    val update = Update(zoneName(zone))
    logger.info("Preparing to update CNAME record: {} {} {} {}", name, ttl, canonicalName, zone)
    update.replace(CNAMERecord(fqdn(name, zone), DClass.IN, ttl, fqdn(canonicalName, zone)))
    sendUpdate(update)
}

suspend fun DnsClient.updateNaptrRecord(
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
        NAPTRRecord(
            fqdn(name, zone),
            DClass.IN,
            ttl,
            order,
            preference,
            flags,
            service,
            regexp,
            if (replacement != ".") fqdn(replacement, zone) else Name(replacement),
        ),
    )
    sendUpdate(update)
}

suspend fun DnsClient.deleteCnameRecord(
    zone: String,
    name: String,
) {
    val update = Update(zoneName(zone))
    logger.info("Preparing to delete CNAME record: {} {}", name, zone)
    update.delete(fqdn(name, zone), Type.CNAME)
    sendUpdate(update)
}

suspend fun DnsClient.deleteNaptrRecord(
    zone: String,
    name: String,
) {
    val update = Update(zoneName(zone))
    logger.info("Preparing to delete NAPTR record: {} {}", name, zone)
    update.delete(fqdn(name, zone), Type.NAPTR)
    sendUpdate(update)
}

class BindException(
    message: String,
) : RuntimeException(message)

private suspend fun DnsClient.sendUpdate(update: Update) {
    logger.info("resolve dns server {}, port {}", dnsConfiguration.server, dnsConfiguration.port)

    val resolver = SimpleResolver(dnsConfiguration.server)
    resolver.port = dnsConfiguration.port
    resolver.tcp = false
    resolver.tsigKey = TSIG(TSIG.HMAC_SHA256, dnsConfiguration.dnsKeyName, dnsConfiguration.dnsKey)

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

private fun fqdn(
    name: String,
    zone: String,
): Name {
    val fq = if (name.endsWith(zone)) name else "$name.$zone"
    return Name.fromString(fq, Name.root)
}

private fun zoneName(zone: String): Name = Name.fromString(zone, Name.root)
