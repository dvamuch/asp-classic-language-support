package dvamuch.aspclassiclanguagesupport2.mcp

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlComment
import dvamuch.aspclassiclanguagesupport2.lang.AspFileType
import dvamuch.aspclassiclanguagesupport2.lang.AspFileViewProvider
import dvamuch.aspclassiclanguagesupport2.lang.AspLanguage
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeDirectiveParser
import dvamuch.aspclassiclanguagesupport2.lang.include.AspIncludeReference
import dvamuch.aspclassiclanguagesupport2.lang.include.AspMissingIncludeInspection
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbIncludeUsageSearcher
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptFileType
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbUnresolvedIdentifierInspection
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbClassStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbConstDecl
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclaration
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbDeclarationUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFileSymbolTable
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbFunctionStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbId
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbIncludeSymbolResolver
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbNamedElement
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbParam
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbPropertyStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbResolveUtil
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbSubStmt
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbVarDecl
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbVisibilityStmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
data class AspFormatResult(val changed: Boolean, val content: String?)

@Serializable
data class AspInspectionProblem(
    val severity: String,
    val message: String,
    val line: Int,
    val column: Int,
    val endLine: Int,
    val endColumn: Int,
    val inspectionId: String
)

@Serializable
data class AspSymbolInfo(
    val name: String,
    val kind: String,
    val path: String,
    val line: Int,
    val column: Int,
    val visibility: String?,
    val fromInclude: Boolean,
    val implicit: Boolean
)

@Serializable
data class AspUsage(
    val path: String,
    val line: Int,
    val column: Int,
    val endLine: Int,
    val endColumn: Int,
    val snippet: String?,
    val declaration: Boolean
)

@Serializable
data class AspIncludeInfo(
    val sourcePath: String,
    val declaredPath: String,
    val resolvedPath: String?,
    val kind: String,
    val depth: Int,
    val resolved: Boolean,
    val cyclic: Boolean
)

@Serializable
data class AspSymbolAt(
    val name: String,
    val kind: String,
    val declarationPath: String?,
    val declarationLine: Int?,
    val declarationColumn: Int?,
    val type: String? = null
)

@Serializable
data class AspInspectionResult(val problems: List<AspInspectionProblem>)

@Serializable
data class AspSymbolsResult(val symbols: List<AspSymbolInfo>)

@Serializable
data class AspUsagesResult(val usages: List<AspUsage>)

@Serializable
data class AspIncludesResult(val includes: List<AspIncludeInfo>)

@Serializable
data class AspSymbolAtResult(val symbol: AspSymbolAt?)

/** IDE-backed operations shared by the MCP adapter and integration tests. */
internal object AspClassicMcpOperations {
    suspend fun formatFile(project: Project, path: String, returnContent: Boolean): AspFormatResult {
        val psiFile = plainRead { supportedPsiFile(project, path) }
        val document = plainRead {
            PsiDocumentManager.getInstance(project).getDocument(psiFile)
                ?: error("No editable document is available for $path")
        }
        val before = plainRead { document.text }
        val format = {
            WriteCommandAction.runWriteCommandAction(project) {
                CodeStyleManager.getInstance(project).reformat(psiFile)
                PsiDocumentManager.getInstance(project).commitAllDocuments()
            }
            FileDocumentManager.getInstance().saveDocument(document)
        }
        if (ApplicationManager.getApplication().isDispatchThread) {
            format()
        } else {
            withContext(Dispatchers.EDT) { format() }
        }

        val after = plainRead { document.text }
        return AspFormatResult(before != after, after.takeIf { returnContent })
    }

    suspend fun inspectFile(project: Project, path: String): List<AspInspectionProblem> = smartRead(project) {
        val file = supportedPsiFile(project, path)
        val problems = mutableListOf<AspInspectionProblem>()

        analysisFile(file)?.let { analysis ->
            problems += parserProblems(project, analysis)
            problems += inspectionProblems(
                project,
                analysis,
                VbUnresolvedIdentifierInspection(),
                "VbUnresolvedIdentifier",
                "warning"
            )
        }

        if (file.viewProvider is AspFileViewProvider) {
            file.viewProvider.getPsi(HTMLLanguage.INSTANCE)?.let { html ->
                problems += inspectionProblems(
                    project,
                    html,
                    AspMissingIncludeInspection(),
                    "AspMissingInclude",
                    "warning"
                )
            }
        }

        problems.sortedWith(compareBy(AspInspectionProblem::line, AspInspectionProblem::column, AspInspectionProblem::inspectionId))
    }

    suspend fun getSymbols(project: Project, path: String): List<AspSymbolInfo> = smartRead(project) {
        val physicalFile = supportedPsiFile(project, path)
        val primary = analysisFile(physicalFile) ?: return@smartRead emptyList()
        val files = buildList {
            add(primary to false)
            if (physicalFile.viewProvider is AspFileViewProvider) {
                VbIncludeSymbolResolver.includedVbScriptFiles(primary).forEach { add(it to true) }
            }
        }

        files.flatMap { (file, fromInclude) ->
            VbFileSymbolTable.get(file).declarations()
                .mapNotNull { declaration -> symbolInfo(project, declaration, fromInclude) }
        }.distinctBy { symbol ->
            listOf(symbol.path, symbol.line.toString(), symbol.column.toString(), symbol.name.lowercase(), symbol.kind)
        }.sortedWith(compareBy(AspSymbolInfo::path, AspSymbolInfo::line, AspSymbolInfo::column))
    }

    suspend fun findUsages(project: Project, path: String, line: Int, column: Int): List<AspUsage> = smartRead(project) {
        val file = supportedPsiFile(project, path)
        val id = idAt(file, line, column) ?: return@smartRead emptyList()
        val declaration = declarationFor(id) ?: return@smartRead emptyList()
        val usages = linkedMapOf<String, AspUsage>()

        fun addUsage(element: PsiElement, range: TextRange, isDeclaration: Boolean) {
            usage(project, element.containingFile, range, isDeclaration)?.let { usage ->
                val key = "${usage.path}:${usage.line}:${usage.column}:${usage.endLine}:${usage.endColumn}"
                val previous = usages[key]
                if (previous == null || isDeclaration) usages[key] = usage
            }
        }

        addUsage(declaration, declaration.textRange, true)
        val scope = GlobalSearchScope.projectScope(project)
        ReferencesSearch.search(declaration, scope, false).forEach { reference ->
            ProgressManager.checkCanceled()
            val element = reference.element
            addUsage(element, reference.rangeInElement.shiftRight(element.textRange.startOffset), false)
        }

        VbIncludeUsageSearcher.find(declaration, scope).forEach { usageInfo ->
            ProgressManager.checkCanceled()
            val element = usageInfo.element ?: return@forEach
            val range = usageInfo.rangeInElement?.shiftRight(element.textRange.startOffset) ?: element.textRange
            addUsage(element, range, false)
        }

        usages.values.sortedWith(compareBy(AspUsage::path, AspUsage::line, AspUsage::column))
    }

    suspend fun resolveIncludes(project: Project, path: String): List<AspIncludeInfo> = smartRead(project) {
        val root = supportedPsiFile(project, path).viewProvider.getPsi(AspLanguage)
            ?: return@smartRead emptyList()
        val result = mutableListOf<AspIncludeInfo>()
        val visited = mutableSetOf(root.virtualFile.url)
        val active = linkedSetOf(root.virtualFile.url)

        fun visit(file: PsiFile, depth: Int) {
            ProgressManager.checkCanceled()
            val html = file.viewProvider.getPsi(HTMLLanguage.INSTANCE) ?: return
            val comments = PsiTreeUtil.collectElementsOfType(html, XmlComment::class.java)
                .sortedBy(PsiElement::getTextOffset)
            for (comment in comments) {
                ProgressManager.checkCanceled()
                val directive = AspIncludeDirectiveParser.parse(comment.text) ?: continue
                val resolvedPsi = AspIncludeReference(comment, directive).resolve()?.containingFile
                val targetFile = resolvedPsi?.originalFile?.virtualFile
                val targetAsp = resolvedPsi?.viewProvider?.getPsi(AspLanguage)
                val cyclic = targetAsp?.virtualFile?.url in active
                result += AspIncludeInfo(
                    sourcePath = projectPath(project, file.virtualFile),
                    declaredPath = directive.path,
                    resolvedPath = targetFile?.let { projectPath(project, it) },
                    kind = directive.kind.name.lowercase(),
                    depth = depth,
                    resolved = targetFile != null,
                    cyclic = cyclic
                )
                if (targetAsp != null && !cyclic && visited.add(targetAsp.virtualFile.url)) {
                    active += targetAsp.virtualFile.url
                    visit(targetAsp, depth + 1)
                    active -= targetAsp.virtualFile.url
                }
            }
        }

        visit(root, 1)
        result
    }

    suspend fun getSymbolAt(project: Project, path: String, line: Int, column: Int): AspSymbolAt? = smartRead(project) {
        val file = supportedPsiFile(project, path)
        val id = idAt(file, line, column) ?: return@smartRead null
        val name = (id as? VbNamedElement)?.name?.takeIf(String::isNotBlank) ?: return@smartRead null
        val declaration = declarationFor(id)
        if (declaration == null) {
            return@smartRead AspSymbolAt(name, "reference", null, null, null)
        }

        val location = sourceLocation(project, declaration.containingFile, declaration.textOffset)
        AspSymbolAt(
            name = name,
            kind = symbolKind(declaration),
            declarationPath = location.path,
            declarationLine = location.line,
            declarationColumn = location.column
        )
    }

    private fun inspectionProblems(
        project: Project,
        file: PsiFile,
        inspection: LocalInspectionTool,
        inspectionId: String,
        severity: String
    ): List<AspInspectionProblem> {
        val holder = ProblemsHolder(com.intellij.codeInspection.InspectionManager.getInstance(project), file, true)
        val visitor = inspection.buildVisitor(holder, true)
        PsiTreeUtil.processElements(file) { element ->
            visitor.visitElement(element)
            true
        }
        return holder.results.mapNotNull { descriptor ->
            problem(project, descriptor, inspectionId, severity)
        }
    }

    private fun parserProblems(project: Project, file: PsiFile): List<AspInspectionProblem> =
        PsiTreeUtil.collectElementsOfType(file, PsiErrorElement::class.java).map { error ->
            val range = error.textRange
            val start = sourceLocation(project, file, range.startOffset)
            val end = sourceLocation(project, file, range.endOffset)
            AspInspectionProblem(
                severity = "error",
                message = error.errorDescription,
                line = start.line,
                column = start.column,
                endLine = end.line,
                endColumn = end.column,
                inspectionId = "AspParser"
            )
        }

    private fun problem(
        project: Project,
        descriptor: ProblemDescriptor,
        inspectionId: String,
        severity: String
    ): AspInspectionProblem? {
        val element = descriptor.psiElement ?: return null
        val relative = descriptor.textRangeInElement ?: TextRange(0, element.textLength)
        val startOffset = (element.textOffset + relative.startOffset).coerceAtMost(element.containingFile.textLength)
        val endOffset = (element.textOffset + relative.endOffset).coerceAtMost(element.containingFile.textLength)
        val start = sourceLocation(project, element.containingFile, startOffset)
        val end = sourceLocation(project, element.containingFile, endOffset)
        return AspInspectionProblem(
            severity = severity,
            message = descriptor.descriptionTemplate.replace("#ref", element.text),
            line = start.line,
            column = start.column,
            endLine = end.line,
            endColumn = end.column,
            inspectionId = inspectionId
        )
    }

    private fun symbolInfo(project: Project, declaration: VbDeclaration, fromInclude: Boolean): AspSymbolInfo? {
        val id = declaration.id
        val name = (id as? VbNamedElement)?.name?.takeIf(String::isNotBlank) ?: return null
        val location = sourceLocation(project, id.containingFile, id.textOffset)
        return AspSymbolInfo(
            name = name,
            kind = symbolKind(id),
            path = location.path,
            line = location.line,
            column = location.column,
            visibility = symbolVisibility(declaration),
            fromInclude = fromInclude,
            implicit = declaration.implicit
        )
    }

    private fun symbolVisibility(declaration: VbDeclaration): String? {
        val id = declaration.id
        val visibility = PsiTreeUtil.getParentOfType(id, VbVisibilityStmt::class.java, false)
        if (visibility != null) {
            return if (visibility.text.trimStart().startsWith("Private", ignoreCase = true)) "private" else "public"
        }
        if (declaration.scope is VbClassStmt && id.parent is VbVarDecl) return "private"
        return null
    }

    private fun symbolKind(element: PsiElement): String = when ((element as? VbId)?.parent) {
        is VbClassStmt -> "class"
        is VbSubStmt -> "sub"
        is VbFunctionStmt -> "function"
        is VbPropertyStmt -> "property"
        is VbConstDecl -> "const"
        is VbParam -> "parameter"
        else -> "variable"
    }

    private fun declarationFor(id: VbId): VbId? = when {
        VbDeclarationUtil.declaration(id) != null -> id
        else -> VbResolveUtil.resolve(id) as? VbId
    }

    private fun idAt(file: PsiFile, line: Int, column: Int): VbId? {
        val analysis = analysisFile(file) ?: return null
        val document = PsiDocumentManager.getInstance(file.project).getDocument(analysis) ?: return null
        val offset = offsetAt(document, line, column)
        val leaf = analysis.findElementAt(offset.coerceAtMost((analysis.textLength - 1).coerceAtLeast(0)))
            ?: return null
        return PsiTreeUtil.getParentOfType(leaf, VbId::class.java, false)
            ?: if (offset > 0) {
                analysis.findElementAt(offset - 1)?.let { PsiTreeUtil.getParentOfType(it, VbId::class.java, false) }
            } else null
    }

    private fun usage(project: Project, file: PsiFile, range: TextRange, declaration: Boolean): AspUsage? {
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return null
        val startOffset = range.startOffset.coerceIn(0, document.textLength)
        val endOffset = range.endOffset.coerceIn(startOffset, document.textLength)
        val start = sourceLocation(project, file, startOffset)
        val end = sourceLocation(project, file, endOffset)
        val lineNumber = document.getLineNumber(startOffset.coerceAtMost((document.textLength - 1).coerceAtLeast(0)))
        val lineText = document.charsSequence.subSequence(
            document.getLineStartOffset(lineNumber),
            document.getLineEndOffset(lineNumber)
        ).toString().trim().take(240)
        return AspUsage(
            path = start.path,
            line = start.line,
            column = start.column,
            endLine = end.line,
            endColumn = end.column,
            snippet = lineText.takeIf(String::isNotEmpty),
            declaration = declaration
        )
    }

    private data class SourceLocation(val path: String, val line: Int, val column: Int)

    private fun sourceLocation(project: Project, file: PsiFile, offset: Int): SourceLocation {
        val document = PsiDocumentManager.getInstance(project).getDocument(file)
            ?: error("No document is available for ${file.name}")
        val safeOffset = offset.coerceIn(0, document.textLength)
        val line = if (document.textLength == 0) 0 else document.getLineNumber(
            safeOffset.coerceAtMost(document.textLength - 1)
        )
        return SourceLocation(
            path = projectPath(project, file.originalFile.virtualFile),
            line = line + 1,
            column = safeOffset - document.getLineStartOffset(line) + 1
        )
    }

    private fun offsetAt(document: Document, line: Int, column: Int): Int {
        require(line >= 1) { "line must be 1 or greater" }
        require(column >= 1) { "column must be 1 or greater" }
        require(line <= document.lineCount) { "line $line is outside the file" }
        val lineIndex = line - 1
        val start = document.getLineStartOffset(lineIndex)
        val end = document.getLineEndOffset(lineIndex)
        val offset = start + column - 1
        require(offset <= end) { "column $column is outside line $line" }
        return offset
    }

    private fun analysisFile(file: PsiFile): PsiFile? = when {
        file.fileType == VbScriptFileType -> file
        file.viewProvider is AspFileViewProvider -> file.viewProvider.getPsi(AspLanguage)
        else -> null
    }

    private fun supportedPsiFile(project: Project, path: String): PsiFile {
        val requested = Path(path)
        val roots = projectRoots(project)
        val virtualFile = if (requested.isAbsolute) {
            val resolved = requested.normalize()
            val file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(resolved)
                ?: error("File not found: $path")
            require(roots.any { root -> VfsUtilCore.isAncestor(root, file, false) }) {
                "Path must stay inside the current project: $path"
            }
            file
        } else {
            val normalized = requested.normalize().toPortablePath()
            require(normalized != ".." && !normalized.startsWith("../")) {
                "Path must stay inside the current project: $path"
            }
            roots.firstNotNullOfOrNull { root -> root.findFileByRelativePath(normalized) }
                ?: error("File not found: $path")
        }
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            ?: error("PSI is not available for $path")
        require(psiFile.fileType == AspFileType || psiFile.fileType == VbScriptFileType) {
            "Only ASP, INC, and VBS files are supported: $path"
        }
        return psiFile
    }

    private fun projectPath(project: Project, file: VirtualFile): String {
        val root = projectRoots(project).firstOrNull { candidate ->
            VfsUtilCore.isAncestor(candidate, file, false)
        }
        return root?.let { VfsUtilCore.getRelativePath(file, it, '/') }
            ?: Path(file.path).normalize().toPortablePath()
    }

    private fun projectRoots(project: Project): List<VirtualFile> = buildList {
        addAll(ProjectRootManager.getInstance(project).contentRoots)
        project.basePath?.let { basePath ->
            LocalFileSystem.getInstance().findFileByPath(basePath)?.let(::add)
        }
    }.distinctBy(VirtualFile::getUrl)

    private fun Path.toPortablePath(): String = toString().replace('\\', '/')

    private suspend fun <T> smartRead(project: Project, action: () -> T): T {
        val application = ApplicationManager.getApplication()
        return if (application.isReadAccessAllowed || application.isDispatchThread) {
            action()
        } else {
            smartReadAction(project, action)
        }
    }

    private suspend fun <T> plainRead(action: () -> T): T {
        val application = ApplicationManager.getApplication()
        return if (application.isReadAccessAllowed || application.isDispatchThread) {
            action()
        } else {
            readAction(action)
        }
    }
}
