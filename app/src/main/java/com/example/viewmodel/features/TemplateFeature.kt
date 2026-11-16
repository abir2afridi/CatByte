package com.example.viewmodel.features

import com.example.data.TemplateEngine
import com.example.model.ViralTemplate
import com.example.viewmodel.EditorViewModel

/**
 * Template library actions.
 *
 * A template is a ready-made edit (timeline slots + canvas ratio). Applying
 * one creates a NEW draft from the template and opens it, so the project the
 * user was working on is never destroyed.
 */
internal object TemplateFeature {

    /** Creates a draft from [template] and opens it in the editor. */
    fun applyTemplate(vm: EditorViewModel, template: ViralTemplate) {
        val project = TemplateEngine.instantiateTemplate(template, emptyList())
        vm.repository.saveProject(project)
        ProjectLifecycleFeature.openProject(vm, project)
    }
}
