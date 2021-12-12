package online.colaba

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths


internal class CommonTest {
    private val resources = "src/test/resources/"

    @Test
    fun localExistsTest() {
        val visible = Paths.get(resources, "files/visible").toAbsolutePath().toFile()
        println(visible)
        assertTrue(visible.exists())
    }

    @Test
    fun localNotExistsTest() {
        val visible = Paths.get(resources, "files/.hidden").toAbsolutePath().toFile()
        println(visible)
        assertTrue(visible.exists())
    }
}
