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
            if (declaration !is KSClassDeclaration &&
                declaration !is KSPropertyDeclaration &&
                declaration !is KSFunctionDeclaration) return@forEach
            val classFqName = declaration.simpleName.asString()
            declaration.docString?.let { kDocMap[classFqName] = it }
            if(declaration is KSClassDeclaration) {
                declaration.getDeclaredProperties().forEach { prop ->
                    if(prop.annotations.any { it.shortName.asString() == "Ignore" }) return@forEach
                    prop.docString?.let { kdoc ->
                        kDocMap["$classFqName.${prop.simpleName.asString()}"] = kdoc
                    }
                }
                declaration.getDeclaredFunctions().forEach { function ->
                    if(function.annotations.any { it.shortName.asString() == "Ignore" }) return@forEach
                    function.docString?.let { kdoc ->
                        kDocMap["$classFqName.${function.simpleName.asString()}"] = kdoc
                    }
                }
            }
        }

        val jsNameSymbols = resolver.getSymbolsWithAnnotation("kotlin.js.JsName")
        jsNameSymbols.forEach { symbol ->
            val jsNameAnnotation = symbol.annotations.firstOrNull { annotation ->
                annotation.annotationType.resolve().declaration.qualifiedName?.asString() == "kotlin.js.JsName"
            } ?: return@forEach

            val nameArgument = jsNameAnnotation.arguments.firstOrNull { argument ->
                argument.name?.asString() == "name"
            }
            val jsName = nameArgument?.value as? String ?: return@forEach
            if (symbol !is KSDeclaration) return@forEach
            typeMapping[symbol.simpleName.asString()] = jsName
        }
        return emptyList()
    }

    override fun finish() {
        if (kDocMap.isEmpty()) return
        kDocMap.forEach { (type, kDocString) ->
            val tsDoc = processKDoc(kDocString)
            if(typeMapping.contains(type)) {
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

    private fun processKDoc(kDocString: String) : String {
        var tsDoc = kDocString
        typeMapping.forEach { (kotlin, typeScript) ->
            tsDoc = tsDoc.replace("[$kotlin]", "[$typeScript]")
        }
        tsDoc = tsDoc.lines().joinToString { it.trim()+"\n * " }
        tsDoc = "/**\n$tsDoc/"
        return tsDoc
    }

    private fun String.escapeJson() = this.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}