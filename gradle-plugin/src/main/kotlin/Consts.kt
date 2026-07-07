import org.gradle.api.attributes.Attribute

object Consts {
    const val PLUGIN_ID = "dev.justincodinguk.safeexports.kdoc-export-ts"
    val kdocElementAttribute = Attribute.of("dev.justincodinguk.safeexports.kdoc-export-elements", String::class.java)
    const val KDOC_JSON_ELEMENT = "extracted-kdoc-json"
}