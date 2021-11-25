package xyz.fmdc.arw.gen

import com.squareup.kotlinpoet.*
import java.io.File

object Vec3XGen {
    fun generates(base: File) {
        generateVec3X(double, vec3D, vec3I).writeTo(base)
        generateVec3X(int, vec3I, vec3D).writeTo(base)
    }

    val vec3 = ClassName("net.minecraft.util", "Vec3")
    private fun generateVec3X(elemType: TypeName, type: ClassName, otherType: TypeName): FileSpec {
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
            .addType(
                TypeSpec.classBuilder(type)
                    .addModifiers(KModifier.DATA)
                    .addSuperinterface(serializable)
                    .addKdoc("This is non-mutable 3 dimensional [%T].\n", elemType)
                    .addKdoc("This class have only basic operations. please use extension function.\n")
                    .addKdoc("@author anatawa12")
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("x", elemType)
                            .addParameter("y", elemType)
                            .addParameter("z", elemType)
                            .build()
                    )
                    .addProperty(field("x", elemType))
                    .addProperty(field("y", elemType))
                    .addProperty(field("z", elemType))
                    .apply {
                        if (elemType == double) {
                            val types = listOf(double, int, float)
                            var passedDDD = false
                            for (type1 in types) {
                                for (type2 in types) {
                                    for (type3 in types) {
                                        if (!passedDDD) {
                                            passedDDD = true
                                            continue
                                        }

                                        addFunction(constructor(type1, type2, type3))
                                    }
                                }
                            }
                            passedDDD = false
                            for (type1 in types) {
                                for (type2 in types) {
                                    for (type3 in types) {
                                        if (!passedDDD) {
                                            passedDDD = true
                                            continue
                                        }
                                        if (double !in setOf(type1, type2, type3)) continue

                                        addFunction(copy(type, type1, type2, type3))
                                    }
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
                            .addStatement("if (z != other.z) return false")
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
                            .addStatement("result = 31 * result + z.hashCode()")
                            .addStatement("return result")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("toString")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(string)
                            .addStatement("return %P", "[\$x,\$y,\$z]")
                            .build()
                    )
                    .addFunction(cross(type, type))
                    .addFunction(dot(type, elemType))
                    .addFunction(cross(otherType, vec3D))
                    .addFunction(dot(otherType, double))
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
                    .addProperty(
                        PropertySpec.builder("normOnXZ", double)
                            .addAnnotation(delegateTransientAnnotationSpec)
                            .delegate("kotlin.lazy { sqrt(x.toDouble() * x + z * z) }")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("toVec3")
                            .addStatement(
                                "return %T.createVectorHelper(x.toDouble(), y.toDouble(), z.toDouble())!!",
                                vec3
                            )
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("normalized")
                            .returns(vec3D)
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
                            .build()
                    )
                    .addFunction(additiveOp("plus", "+", vec3D, vec3D))
                    .addFunction(additiveOp("minus", "-", vec3D, vec3D))
                    .addFunction(additiveOp("plus", "+", vec3I, type))
                    .addFunction(additiveOp("minus", "-", vec3I, type))
                    .addFunction(multiplicativeOp("times", "*", double, vec3D))
                    .addFunction(multiplicativeOp("div", "/", double, vec3D))
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

    fun cross(forType: TypeName, result: TypeName) = FunSpec.builder("cross")
        .addModifiers(KModifier.INFIX)
        .addParameter("other", forType)
        .returns(result)
        .addStatement(
            "return %T(y * other.z - z * other.y, \n" +
                    "        z * other.x - x * other.z, \n" +
                    "        x * other.y - y * other.x)", result
        )
        .build()

    fun dot(forType: TypeName, result: TypeName) = FunSpec.builder("dot")
        .addModifiers(KModifier.INFIX)
        .addParameter("other", forType)
        .returns(result)
        .addStatement("return x * other.x + y * other.y + z * other.z")
        .build()

    fun additiveOp(name: String, operator: String, otherType: TypeName, returnName: TypeName) = FunSpec.builder(name)
        .addModifiers(KModifier.OPERATOR)
        .addParameter("other", otherType)
        .returns(returnName)
        .addStatement("return %L(x $operator other.x, y $operator other.y, z $operator other.z)", returnName)
        .build()

    fun multiplicativeOp(name: String, operator: String, otherType: TypeName, returnName: TypeName) =
        FunSpec.builder(name)
            .addModifiers(KModifier.OPERATOR)
            .returns(returnName)
            .addParameter("other", otherType)
            .addStatement("return %L(x $operator other, y $operator other, z $operator other)", returnName)
            .build()

    fun constructor(x: TypeName, y: TypeName, z: TypeName) = FunSpec.constructorBuilder()
        .addParameter("x", x)
        .addParameter("y", y)
        .addParameter("z", z)
        .callThisConstructor(
            if (x == double) "x" else "x.toDouble()",
            if (y == double) "y" else "y.toDouble()",
            if (z == double) "z" else "z.toDouble()"
        )
        .build()

    fun copy(thisType: ClassName, x: TypeName, y: TypeName, z: TypeName) = FunSpec.builder("copy")
        .addParameter(copyParam("x", x))
        .addParameter(copyParam("y", y))
        .addParameter(copyParam("z", z))
        .addStatement("return %T(x, y, z)", thisType)
        .build()

    fun copyParam(name: String, type: TypeName) = ParameterSpec.builder(name, type)
        .apply {
            if (type == double)
                defaultValue("this.$name")
        }
        .build()

}
