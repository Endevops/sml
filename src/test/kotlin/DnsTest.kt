package be.endevops

import kotlin.test.Test
import kotlin.test.assertEquals

class DnsTest {
    @Test
    fun base32Test() {
        assertEquals("gfwifwefpmd4mlcqrkqhlvovcx4p7vldz7pir4zumq5vavpiwtua", identifierEncode("9925:BE0847183845"))
        assertEquals("gfwifwefpmd4mlcqrkqhlvovcx4p7vldz7pir4zumq5vavpiwtua", identifierEncode("9925:be0847183845"))
        assertEquals("eh5boavaktmbgzyh2a63dz4qov33fvp5nsdvqklucfraayoodw6a", identifierEncode("9915:test"))
        assertEquals("85008b8279e07ab0392da75fa55856a2", oldIdentifierEncode("9915:test"))
        assertEquals("0470b6ff4bf67e36f4696494fdaa028f", oldIdentifierEncode("9925:be0847183845"))
        assertEquals("b79311d749c2d8cc01ac0e9c68f4f484", oldIdentifierEncode("9925:BE0794750791"))
    }
}
