package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.lang.Language
import com.intellij.psi.templateLanguages.TemplateLanguage
import dvamuch.aspclassiclanguagesupport2.lang.vbscript.VbScriptLanguage

object AspLanguage : Language(VbScriptLanguage, "ASP"), TemplateLanguage
