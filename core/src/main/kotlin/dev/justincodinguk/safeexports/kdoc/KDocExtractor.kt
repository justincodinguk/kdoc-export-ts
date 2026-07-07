package dev.justincodinguk.safeexports.kdoc

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.io.OutputStreamWriter

class KDocExtractor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val kDocMap = mutableMapOf<String, String>()
    private val typeMapping = mutableMapOf(
        "String" to "string",
        "Boolean" to "boolean",
        "Int" to "number",
        "Array" to "array",
        "List" to "KtList",
        "Map" to "KtMap",
        "Set" to "KtSet",
        "Double" to "number",
        "Float" to "number",
        "Short" to "number",
        "Long" to "bigint",
        "Byte" to "number",
        "Char" to "number",
        "Unit" to "void",
        "Any" to "any"
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("kotlin.js.JsExport")
        symbols.forEach { declaration ->
            when (declaration) {
                is KSClassDeclaration -> processClassDeclarationTransitively(declaration)
                is KSPropertyDeclaration -> processPropertyDeclaration(declaration, null)
                is KSFunctionDeclaration -> processFunctionDeclaration(declaration, null)
            }
        }
        return emptyList()
    }

    private fun processClassDeclarationTransitively(decl: KSClassDeclaration) {
        val className = decl.simpleName.asString()
        if (kDocMap.contains(className)) return

        val modifiedClassName = getModifiedName(decl, className)
        decl.docString?.let {
            kDocMap[modifiedClassName] = it
        }
        decl.getDeclaredProperties().forEach { prop ->
            processPropertyDeclaration(prop, modifiedClassName)
        }
        decl.getDeclaredFunctions().forEach { function ->
            processFunctionDeclaration(function, modifiedClassName)
        }
    }

    private fun processFunctionDeclaration(function: KSFunctionDeclaration, className: String?) {
        if (function.annotations.any { it.shortName.asString() == "Ignore" }) return
        val modifiedFunctionName = getModifiedName(function, function.simpleName.asString())
        val key = "${className?.plus(".") ?: ""}$modifiedFunctionName"
        if (kDocMap.contains(key)) return
        function.docString?.let { kdoc ->
            kDocMap[key] = kdoc
        }
    }

    private fun processPropertyDeclaration(prop: KSPropertyDeclaration, className: String?) {
        if (prop.annotations.any { it.shortName.asString() == "Ignore" }) return
        val modifiedPropName = getModifiedName(prop, prop.simpleName.asString())
        val key = "${className?.plus(".") ?: ""}$modifiedPropName"
        if (kDocMap.contains(key)) return
        prop.docString?.let { kdoc ->
            kDocMap["${className?.plus(".") ?: ""}$modifiedPropName"] = kdoc
        }
    }

    private fun getModifiedName(decl: KSDeclaration, originalName: String): String {
        val jsNameAnnotation = decl.annotations.firstOrNull { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString() == "kotlin.js.JsName"
        } ?: return originalName

        val nameArgument = jsNameAnnotation.arguments.firstOrNull { argument ->
            argument.name?.asString() == "name"
        }
        val jsName = nameArgument?.value as? String ?: return originalName
        return jsName
    }

    override fun finish() {
        kDocMap.forEach { (type, kDocString) ->
            val tsDoc = processKDoc(kDocString)
            if (typeMapping.contains(type)) {
                kDocMap[typeMapping[type]!!] = tsDoc
                kDocMap.remove(type)
            } else kDocMap[type] = tsDoc
        }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = "",
            fileName = "extracted-kdocs",
            extensionName = "json"
        )

        OutputStreamWriter(file).use { writer ->
            writer.write("{\n")
            val entries = kDocMap.entries.joinToString(",\n") { (key, value) ->
                "  \"$key\": \"${value.escapeJson()}\""
            }
            writer.write(entries)
            writer.write("\n}")
        }
    }

    private fun processKDoc(kDocString: String): String {
        var tsDoc = kDocString
        typeMapping.forEach { (kotlin, typeScript) ->
            tsDoc = tsDoc.replace("[$kotlin]", "[$typeScript]")
        }
        tsDoc = tsDoc.lines().joinToString("\n * ")
        tsDoc = "/**\n ${tsDoc.trim()}/\n"
        return tsDoc
    }

    private fun String.escapeJson() = this.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}