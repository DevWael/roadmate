import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Extension to access the version catalog from convention plugins.
 * Shared across all convention plugins that need catalog lookups.
 */
internal val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
