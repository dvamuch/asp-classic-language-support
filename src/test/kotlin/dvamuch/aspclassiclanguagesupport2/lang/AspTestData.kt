package dvamuch.aspclassiclanguagesupport2.lang

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object AspTestData {
    private val root: Path = Path.of(
        System.getProperty("user.dir"),
        "src",
        "test",
        "testData",
        "asp"
    )

    fun read(relativePath: String): String {
        val file = root.resolve(relativePath).normalize()
        require(file.startsWith(root)) { "Unexpected test data path: $relativePath" }
        return Files.readString(file, StandardCharsets.UTF_8)
    }

    fun realWorldDir(): Path = root.resolve("real-world")
}
