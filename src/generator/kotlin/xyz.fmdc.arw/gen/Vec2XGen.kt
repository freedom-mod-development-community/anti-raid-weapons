package xyz.fmdc.arw.gen

import com.squareup.kotlinpoet.*
import java.io.File

object Vec2XGen {
    fun generates(base: File) {
        generateVec2X(double, vec2D).writeTo(base)
    }

    private fun generateVec2X(elemType: TypeName, type: ClassName): FileSpec {
        return FileSpec.builder(type.packageName, type.simpleName + ".g")
            //@file:Suppress("RemoveRedundantCallsOfConversionMethods", "RemoveRedundantQualifierName", "unused")
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "RemoveRedundantCallsOfConversionMethods")
                    .addMember("%S", "RemoveRedundantQualifierName")
                    .addMember("%S", "unused")
                    .build()
            )
            .addImport("kotlin.math", "sqrt")
            .addType(TypeSpec.classBuilder(type)
                .addModifiers(KModifier.DATA)
                .addSuperinterface(serializable)
                .addKdoc("This is non-mutable 2 dimensional [%T].\n", elemType)
                .addKdoc("This class have only basic operations. please use extension function.\n")
                .addKdoc("@author anatawa12")
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("x", elemType)
                        .addParameter("y", elemType)
                        .build()
                )
                .addProperty(field("x", elemType))
                .addProperty(field("y", elemType))
                .apply {
                    if (elemType == double) {
                        val types = listOf(double, int, float)
                        var passedDD = false
                        for (type1 in types) {
                            for (type2 in types) {
                                if (!passedDD) {
                                    passedDD = true
                                    continue
                                }
                                addFunction(constructor(type1, type2))
                            }
                        }
                    }
                }
                .addFunction(
                    FunSpec.builder("equals")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("other", any.copy(nullable = true))
                        .returns(boolean)
                        .addStatement("if (this === other) return true")
                        .addStatement("if (javaClass != other?.javaClass) return false")
                        .addStatement("")
                        .addStatement("other as %T", type)
                        .addStatement("")
                        .addStatement("if (x != other.x) return false")
                        .addStatement("if (y != other.y) return false")
                        .addStatement("")
                        .addStatement("return true")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("hashCode")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(int)
                        .addStatement("var result = x.hashCode()")
                        .addStatement("result = 31 * result + y.hashCode()")
                        .addStatement("return result")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(string)
                        .addStatement("return %P", "[\$x,\$y]")
                        .build()
                )
                .addFunction(dot(type, elemType))
                .addProperty(
                    PropertySpec.builder("normPow2", elemType)
                        .addAnnotation(delegateTransientAnnotationSpec)
                        .delegate("kotlin.lazy { (this dot this) }")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("norm", double)
                        .addAnnotation(delegateTransientAnnotationSpec)
                        .delegate("kotlin.lazy { sqrt(normPow2.toDouble()) }")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("normalized")
                        .returns(vec2D)
                        .addStatement("return this / norm")
                        .build()
                )
                .addType(
                    TypeSpec.companionObjectBuilder()
                        .addProperty(
                            PropertySpec.builder("serialVersionUID", long)
                                .addModifiers(KModifier.CONST)
                                .addModifiers(KModifier.PRIVATE)
                                .initializer("1")
                                .build()
                        )
                        .addProperty(
                            PropertySpec.builder("ORIGIN", type)
                                .addAnnotation(JvmStatic::class)
                                .initializer("%T(0, 0)", type)
                                .build()
                        )
                        .build()
                )
                .addFunction(additiveOp("plus", "+", vec2D, vec2D))
                .addFunction(additiveOp("minus", "-", vec2D, vec2D))
                //.addFunction(additiveOp("plus", "+", vec3I, type))
                //.addFunction(additiveOp("minus", "-", vec3I, type))
                .addFunction(multiplicativeOp("times", "*", double, vec2D))
                .addFunction(multiplicativeOp("div", "/", double, vec2D))
                .addFunction(multiplicativeOp("times", "*", int, type))
                .addFunction(multiplicativeOp("div", "/", int, type))
                .build())
            .indent("    ")
            .build()
    }

    fun field(name: String, type: TypeName) = PropertySpec.builder(name, type)
        .addAnnotation(JvmField::class)
        .initializer(name)
        .build()

    fun dot(forType: TypeName, result: TypeName) = FunSpec.builder("dot")
        .addModifiers(KModifier.INFIX)
        .addParameter("other", forType)
        .returns(result)
        .addStatement("return x * other.x + y * other.y")
        .build()

    fun additiveOp(name: String, operator: String, otherType: TypeName, returnName: TypeName) = FunSpec.builder(name)
        .addModifiers(KModifier.OPERATOR)
        .addParameter("other", otherType)
        .returns(returnName)
        .addStatement("return %L(x $operator other.x, y $operator other.y)", returnName)
        .build()

    fun multiplicativeOp(name: String, operator: String, otherType: TypeName, returnName: TypeName) =
        FunSpec.builder(name)
            .addModifiers(KModifier.OPERATOR)
            .returns(returnName)
            .addParameter("other", otherType)
            .addStatement("return %L(x $operator other, y $operator other)", returnName)
            .build()

    fun constructor(x: TypeName, y: TypeName) = FunSpec.constructorBuilder()
        .addParameter("x", x)
        .addParameter("y", y)
        .callThisConstructor(if (x == double) "x" else "x.toDouble()", if (y == double) "y" else "y.toDouble()")
        .build()

}
