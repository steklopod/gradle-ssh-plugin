package online.colaba

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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

    /**
     * The checkout folder is named otherwise than the project (a worktree, a CI workspace): the frontend
     * archive must still be found, or the deploy ships the whole `frontend/` folder instead of one file.
     */
    @Test
    fun `a path is found in a checkout folder named otherwise than the project`(@TempDir tmp: File) {
        val checkout = File(tmp, "smz-worktree").apply { mkdirs() }
        File(checkout, "frontend/.output.tar.xz").apply { parentFile.mkdirs(); writeText("archive") }
        val root = ProjectBuilder.builder().withProjectDir(checkout).withName("smz").build()
        val frontend = ProjectBuilder.builder().withParent(root).withProjectDir(File(checkout, "frontend")).withName("frontend").build()

        assertTrue(root.localExists("frontend/.output.tar.xz"), "the archive is seen from the root project")
        assertTrue(frontend.localExists(".output.tar.xz"), "and from its own subproject")
        assertFalse(root.localExists("frontend/node_modules"), "a missing path stays missing")
    }
}
