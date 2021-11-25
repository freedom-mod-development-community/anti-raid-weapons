package xyz.fmdc.arw.gen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.io.Serializable

val vec3D = ClassName("xyz.fmdc.arw.vector", "Vec3D")
val vec3I = ClassName("xyz.fmdc.arw.vector", "Vec3I")

val vec2D = ClassName("xyz.fmdc.arw.vector", "Vec2D")

val serializable = Serializable::class.asClassName()

val double = Double::class.asClassName()
val any = Any::class.asClassName()
val boolean = Boolean::class.asTypeName()
val int = Int::class.asTypeName()
val long = Long::class.asTypeName()
val float = Float::class.asTypeName()
val string = String::class.asTypeName()

val delegateTransientAnnotationSpec = AnnotationSpec.builder(Transient::class).useSiteTarget(AnnotationSpec.UseSiteTarget.DELEGATE).build()
