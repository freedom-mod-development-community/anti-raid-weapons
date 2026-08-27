package xyz.fmdc.arw.modelloder

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.BufferedReader
import java.io.InputStreamReader

class WavefrontObject(val resourceLocation: ResourceLocation) {

    private val vertices = ArrayList<Vertex>()
    private val textureCoordinates = ArrayList<TextureCoordinate>()
    private val normals = ArrayList<Normal>()
    private val groups = HashMap<String, GroupObject>()
    private var currentGroup: GroupObject? = null

    init {
        loadObjModel()
    }

    private fun loadObjModel() {
        val resource = Minecraft.getInstance().resourceManager.getResource(resourceLocation)
            .orElseThrow { IllegalArgumentException("Could not find OBJ model: $resourceLocation") }

        BufferedReader(InputStreamReader(resource.open())).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                val tokens = trimmed.split("\\s+".toRegex())
                when (tokens[0]) {
                    "v" -> {
                        vertices.add(
                            Vertex(
                                tokens[1].toFloat(),
                                tokens[2].toFloat(),
                                tokens[3].toFloat()
                            )
                        )
                    }
                    "vt" -> {
                        // OBJのV座標は上下反転しているため 1.0f - v で補正
                        val u = tokens[1].toFloat()
                        val v = if (tokens.size > 2) 1.0f - tokens[2].toFloat() else 0.0f
                        textureCoordinates.add(TextureCoordinate(u, v))
                    }
                    "vn" -> {
                        normals.add(
                            Normal(
                                tokens[1].toFloat(),
                                tokens[2].toFloat(),
                                tokens[3].toFloat()
                            )
                        )
                    }
                    "g", "o" -> {
                        val groupName = if (tokens.size > 1) tokens[1] else "default"
                        currentGroup = groups.computeIfAbsent(groupName) { GroupObject() }
                    }
                    "f" -> {
                        if (currentGroup == null) {
                            currentGroup = groups.computeIfAbsent("default") { GroupObject() }
                        }
                        val face = Face()
                        for (i in 1 until tokens.size) {
                            val faceTokens = tokens[i].split("/")
                            val vIdx = faceTokens[0].toInt() - 1
                            val vtIdx = if (faceTokens.size > 1 && faceTokens[1].isNotEmpty()) faceTokens[1].toInt() - 1 else -1
                            val vnIdx = if (faceTokens.size > 2 && faceTokens[2].isNotEmpty()) faceTokens[2].toInt() - 1 else -1
                            face.addVertex(vIdx, vtIdx, vnIdx)
                        }
                        currentGroup!!.faces.add(face)
                    }
                }
            }
        }
    }

    /**
     * 全パーツを一括描画
     */
    fun renderAll(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val last = poseStack.last()
        val poseMatrix = last.pose()
        val normalMatrix = last.normal()

        for (group in groups.values) {
            renderGroup(group, poseMatrix, normalMatrix, consumer, packedLight, packedOverlay)
        }
    }

    /**
     * 指定した名前の特定パーツのみを描画
     */
    fun renderPart(
        partName: String,
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val group = groups[partName] ?: return
        val last = poseStack.last()
        renderGroup(group, last.pose(), last.normal(), consumer, packedLight, packedOverlay)
    }

    private fun renderGroup(
        group: GroupObject,
        poseMatrix: Matrix4f,
        normalMatrix: Matrix3f,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val posVec = Vector4f()
        val normVec = Vector3f()

        for (face in group.faces) {
            // 三角形分割（3頂点はそのまま1面、4頂点は2面に分割）
            val vertexCount = face.vertexIndices.size
            if (vertexCount < 3) continue

            for (i in 1 until vertexCount - 1) {
                renderVertex(face, 0, poseMatrix, normalMatrix, consumer, packedLight, packedOverlay, posVec, normVec)
                renderVertex(face, i, poseMatrix, normalMatrix, consumer, packedLight, packedOverlay, posVec, normVec)
                renderVertex(face, i + 1, poseMatrix, normalMatrix, consumer, packedLight, packedOverlay, posVec, normVec)
            }
        }
    }

    private fun renderVertex(
        face: Face,
        index: Int,
        poseMatrix: Matrix4f,
        normalMatrix: Matrix3f,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int,
        posVec: Vector4f,
        normVec: Vector3f
    ) {
        val vIdx = face.vertexIndices[index]
        val vtIdx = face.textureIndices[index]
        val vnIdx = face.normalIndices[index]

        val vert = vertices[vIdx]
        posVec.set(vert.x, vert.y, vert.z, 1.0f)
        posVec.mul(poseMatrix)

        val u = if (vtIdx >= 0 && vtIdx < textureCoordinates.size) textureCoordinates[vtIdx].u else 0.0f
        val v = if (vtIdx >= 0 && vtIdx < textureCoordinates.size) textureCoordinates[vtIdx].v else 0.0f

        if (vnIdx >= 0 && vnIdx < normals.size) {
            val norm = normals[vnIdx]
            normVec.set(norm.x, norm.y, norm.z)
            normVec.mul(normalMatrix)
            normVec.normalize()
        } else {
            normVec.set(0.0f, 1.0f, 0.0f)
            normVec.mul(normalMatrix)
        }

        consumer.vertex(posVec.x().toDouble(), posVec.y().toDouble(), posVec.z().toDouble())
            .color(255, 255, 255, 255)
            .uv(u, v)
            .overlayCoords(packedOverlay)
            .uv2(packedLight)
            .normal(normVec.x(), normVec.y(), normVec.z())
            .endVertex()
    }

    // =========================================================================
    // 内部データ保持クラス群
    // =========================================================================

    private class Vertex(val x: Float, val y: Float, val z: Float)
    private class TextureCoordinate(val u: Float, val v: Float)
    private class Normal(val x: Float, val y: Float, val z: Float)

    private class GroupObject {
        val faces = ArrayList<Face>()
    }

    private class Face {
        val vertexIndices = ArrayList<Int>()
        val textureIndices = ArrayList<Int>()
        val normalIndices = ArrayList<Int>()

        fun addVertex(v: Int, vt: Int, vn: Int) {
            vertexIndices.add(v)
            textureIndices.add(vt)
            normalIndices.add(vn)
        }
    }
}