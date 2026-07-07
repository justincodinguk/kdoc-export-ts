import org.gradle.api.provider.Property
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class AggregateExtension @Inject constructor(objects: ObjectFactory) {
    val embedKdoc: Property<Boolean> = objects.property(Boolean::class.java).apply {
        convention(false)
    }
}