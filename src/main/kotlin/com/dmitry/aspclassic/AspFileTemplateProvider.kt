package com.dmitry.aspclassic

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

/**
 * File template provider for ASP Classic
 */
class AspFileTemplateProvider : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("ASP Classic", AspIcons.FILE)
        group.addTemplate(FileTemplateDescriptor("ASP File.asp", AspIcons.FILE))
        group.addTemplate(FileTemplateDescriptor("ASP Include.inc", AspIcons.FILE))
        return group
    }
}

