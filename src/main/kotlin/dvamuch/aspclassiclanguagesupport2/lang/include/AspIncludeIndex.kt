package dvamuch.aspclassiclanguagesupport2.lang.include

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.util.Locale

class AspIncludeIndex : ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void> = NAME

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { input ->
        buildMap {
            for (directive in AspIncludeDirectiveParser.findAll(input.contentAsText)) {
                put(targetName(directive.path), null)
            }
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
        file.extension?.lowercase(Locale.ROOT) in SUPPORTED_EXTENSIONS
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 1

    companion object {
        private val NAME = ID.create<String, Void>("asp.classic.include.targets")
        private val SUPPORTED_EXTENSIONS = setOf("asp", "inc")

        fun candidateConsumers(
            includedFile: VirtualFile,
            searchScope: GlobalSearchScope
        ): Collection<VirtualFile> {
            return FileBasedIndex.getInstance().getContainingFiles(
                NAME,
                targetName(includedFile.name),
                searchScope
            )
        }

        private fun targetName(path: String): String {
            return path.replace('\\', '/')
                .substringAfterLast('/')
                .lowercase(Locale.ROOT)
        }
    }
}
