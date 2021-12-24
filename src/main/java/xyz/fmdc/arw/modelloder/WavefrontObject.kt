/**
 * This file is originally a part of MinecraftForge.
 * This file is available under and governed by the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
 * However, the original source was available under Forge Public License (https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/MinecraftForge-License.txt)
 */

package xyz.fmdc.arw.modelloder

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11
import xyz.fmdc.arw.minus
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Wavefront Object importer
 * Based heavily off of the specifications found at http://en.wikipedia.org/wiki/Wavefront_.obj_file
 */
//TODO: Split parser and parsed object for small memory
class WavefrontObject {
    companion object {
        private val vertexPattern =
            Pattern.compile("(v( (\\-){0,1}\\d+(\\.\\d+)?){3,4} *\\n)|(v( (\\-){0,1}\\d+(\\.\\d+)?){3,4} *$)")
        private val vertexNormalPattern =
            Pattern.compile("(vn( (\\-){0,1}\\d+(\\.\\d+)?){3,4} *\\n)|(vn( (\\-){0,1}\\d+(\\.\\d+)?){3,4} *$)")
        private val textureCoordinatePattern =
            Pattern.compile("(vt( (\\-){0,1}\\d+\\.\\d+){2,3} *\\n)|(vt( (\\-){0,1}\\d+(\\.\\d+)?){2,3} *$)")
        private val face_V_VT_VN_Pattern =
            Pattern.compile("(f( \\d+/\\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+/\\d+){3,4} *$)")
        private val face_V_VT_Pattern = Pattern.compile("(f( \\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+){3,4} *$)")
        private val face_V_VN_Pattern = Pattern.compile("(f( \\d+//\\d+){3,4} *\\n)|(f( \\d+//\\d+){3,4} *$)")
        private val face_V_Pattern = Pattern.compile("(f( \\d+){3,4} *\\n)|(f( \\d+){3,4} *$)")
        private val groupObjectPattern = Pattern.compile("([go]( [\\w\\d\\.]+) *\\n)|([go]( [\\w\\d\\.]+) *$)")
    }

    private var vertexMatcher: Matcher? = null
    private var vertexNormalMatcher: Matcher? = null
    private var textureCoordinateMatcher: Matcher? = null
    private var face_V_VT_VN_Matcher: Matcher? = null
    private var face_V_VT_Matcher: Matcher? = null
    private var face_V_VN_Matcher: Matcher? = null
    private var face_V_Matcher: Matcher? = null
    private var groupObjectMatcher: Matcher? = null
    var vertices = ArrayList<Vertex>()
    var vertexNormals = ArrayList<Vertex>()
    var textureCoordinates = ArrayList<TextureCoordinate>()
    var groupObjects = ArrayList<GroupObject>()
    private var currentGroupObject: GroupObject? = null
    private var fileName: String

    constructor(resource: ResourceLocation) {
        fileName = resource.toString()
        try {
            val res = Minecraft.getMinecraft().resourceManager.getResource(resource)
            loadObjModel(res.inputStream)
        } catch (e: IOException) {
            throw ModelFormatException("IO Exception reading model format", e)
        }
    }

    constructor(filename: String, inputStream: InputStream) {
        fileName = filename
        loadObjModel(inputStream)
    }

    @Throws(ModelFormatException::class)
    private fun loadObjModel(inputStream: InputStream) {
        var reader: BufferedReader? = null
        var lineCount = 0
        try {
            reader = BufferedReader(InputStreamReader(inputStream))
            for (currentLine in reader.lineSequence()) {
                lineCount++
                if (currentLine.startsWith("#") || currentLine.isEmpty()) {
                    continue
                } else if (currentLine.startsWith("v ")) {
                    val vertex = parseVertex(currentLine, lineCount)
                    if (vertex != null) {
                        vertices.add(vertex)
                    }
                } else if (currentLine.startsWith("vn ")) {
                    val vertex = parseVertexNormal(currentLine, lineCount)
                    if (vertex != null) {
                        vertexNormals.add(vertex)
                    }
                } else if (currentLine.startsWith("vt ")) {
                    val textureCoordinate = parseTextureCoordinate(currentLine, lineCount)
                    if (textureCoordinate != null) {
                        textureCoordinates.add(textureCoordinate)
                    }
                } else if (currentLine.startsWith("f ")) {
                    if (currentGroupObject == null) {
                        currentGroupObject = GroupObject("Default")
                    }
                    val face = parseFace(currentLine, lineCount)
                    currentGroupObject!!.faces.add(face)
                } else if (currentLine.startsWith("g ") or currentLine.startsWith("o ")) {
                    val group = parseGroupObject(currentLine, lineCount)
                    if (group != null) {
                        if (currentGroupObject != null) {
                            groupObjects.add(currentGroupObject!!)
                        }
                    }
                    currentGroupObject = group
                }
            }
            groupObjects.add(currentGroupObject!!)
        } catch (e: IOException) {
            throw ModelFormatException("IO Exception reading model format", e)
        } finally {
            try {
                reader!!.close()
            } catch (e: IOException) {
                // hush
            }
            try {
                inputStream.close()
            } catch (e: IOException) {
                // hush
            }
        }
    }

    @SideOnly(Side.CLIENT)
    fun renderAll() {
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        if (currentGroupObject != null) {
            buffer.begin(currentGroupObject!!.glDrawingMode, Face.VERTEX_FORMAT)
        } else {
            buffer.begin(GL11.GL_TRIANGLES, Face.VERTEX_FORMAT)
        }
        tessellateAll(buffer)
        tessellator.draw()
    }

    @SideOnly(Side.CLIENT)
    fun tessellateAll(buffer: BufferBuilder) {
        for (groupObject in groupObjects) {
            groupObject.render(buffer)
        }
    }

    @SideOnly(Side.CLIENT)
    fun renderOnly(vararg groupNames: String) {
        for (groupObject in groupObjects) {
            for (groupName in groupNames) {
                if (groupName.equals(groupObject.name, ignoreCase = true)) {
                    groupObject.render()
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    fun tessellateOnly(buffer: BufferBuilder, vararg groupNames: String) {
        for (groupObject in groupObjects) {
            for (groupName in groupNames) {
                if (groupName.equals(groupObject.name, ignoreCase = true)) {
                    groupObject.render(buffer)
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    fun renderPart(partName: String) {
        for (groupObject in groupObjects) {
            if (partName.equals(groupObject.name, ignoreCase = true)) {
                groupObject.render()
            }
        }
    }

    @SideOnly(Side.CLIENT)
    fun renderPart(partName: String, light: Int) {
        for (groupObject in groupObjects) {
            if (partName.equals(groupObject.name, ignoreCase = true)) {
                groupObject.render(light)
            }
        }
    }

    @SideOnly(Side.CLIENT)
    fun tessellatePart(buffer: BufferBuilder, partName: String) {
        for (groupObject in groupObjects) {
            if (partName.equals(groupObject.name, ignoreCase = true)) {
                groupObject.render(buffer)
            }
        }
    }

    @SideOnly(Side.CLIENT)
    fun renderAllExcept(vararg excludedGroupNames: String) {
        for (groupObject in groupObjects) {
            var skipPart = false
            for (excludedGroupName in excludedGroupNames) {
                if (excludedGroupName.equals(groupObject.name, ignoreCase = true)) {
                    skipPart = true
                }
            }
            if (!skipPart) {
                groupObject.render()
            }
        }
    }

    @SideOnly(Side.CLIENT)
    fun tessellateAllExcept(buffer: BufferBuilder, vararg excludedGroupNames: String) {
        var exclude: Boolean
        for (groupObject in groupObjects) {
            exclude = false
            for (excludedGroupName in excludedGroupNames) {
                if (excludedGroupName.equals(groupObject.name, ignoreCase = true)) {
                    exclude = true
                }
            }
            if (!exclude) {
                groupObject.render(buffer)
            }
        }
    }

    @Throws(ModelFormatException::class)
    private fun parseVertex(line: String, lineCount: Int): Vertex? {
        var line = line
        val vertex: Vertex? = null
        if (isValidVertexLine(line)) {
            line = line.substring(line.indexOf(" ") + 1)
            val tokens = line.split(" ").toTypedArray()
            try {
                if (tokens.size == 2) {
                    return Vertex(tokens[0].toFloat(), tokens[1].toFloat())
                } else if (tokens.size == 3) {
                    return Vertex(tokens[0].toFloat(), tokens[1].toFloat(), tokens[2].toFloat())
                }
            } catch (e: NumberFormatException) {
                throw ModelFormatException(String.format("Number formatting error at line %d", lineCount), e)
            }
        } else {
            throw ModelFormatException("Error parsing entry ('$line', line $lineCount) in file '$fileName' - Incorrect format")
        }
        return vertex
    }

    @Throws(ModelFormatException::class)
    private fun parseVertexNormal(line: String, lineCount: Int): Vertex? {
        var line = line
        val vertexNormal: Vertex? = null
        if (isValidVertexNormalLine(line)) {
            line = line.substring(line.indexOf(" ") + 1)
            val tokens = line.split(" ").toTypedArray()
            try {
                if (tokens.size == 3) return Vertex(tokens[0].toFloat(), tokens[1].toFloat(), tokens[2].toFloat())
            } catch (e: NumberFormatException) {
                throw ModelFormatException(String.format("Number formatting error at line %d", lineCount), e)
            }
        } else {
            throw ModelFormatException("Error parsing entry ('$line', line $lineCount) in file '$fileName' - Incorrect format")
        }
        return vertexNormal
    }

    @Throws(ModelFormatException::class)
    private fun parseTextureCoordinate(line: String, lineCount: Int): TextureCoordinate? {
        var line = line
        val textureCoordinate: TextureCoordinate? = null
        if (isValidTextureCoordinateLine(line)) {
            line = line.substring(line.indexOf(" ") + 1)
            val tokens = line.split(" ").toTypedArray()
            try {
                if (tokens.size == 2) return TextureCoordinate(tokens[0].toFloat(),
                    1 - tokens[1].toFloat()) else if (tokens.size == 3) return TextureCoordinate(
                    tokens[0].toFloat(), 1 - tokens[1].toFloat(), tokens[2].toFloat())
            } catch (e: NumberFormatException) {
                throw ModelFormatException(String.format("Number formatting error at line %d", lineCount), e)
            }
        } else {
            throw ModelFormatException("Error parsing entry ('$line', line $lineCount) in file '$fileName' - Incorrect format")
        }
        return textureCoordinate
    }

    @Throws(ModelFormatException::class)
    private fun parseFace(line: String, lineCount: Int): Face {
        var face: Face? = null
        if (isValidFaceLine(line)) {
            face = Face()
            val trimmedLine = line.substring(line.indexOf(" ") + 1)
            val tokens = trimmedLine.split(" ").toTypedArray()
            if (tokens.size == 3) {
                if (currentGroupObject!!.glDrawingMode == -1) {
                    currentGroupObject!!.glDrawingMode = GL11.GL_TRIANGLES
                } else if (currentGroupObject!!.glDrawingMode != GL11.GL_TRIANGLES) {
                    throw ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Invalid number of points for face (expected 4, found " + tokens.size + ")")
                }
            } else if (tokens.size == 4) {
                if (currentGroupObject!!.glDrawingMode == -1) {
                    currentGroupObject!!.glDrawingMode = GL11.GL_QUADS
                } else if (currentGroupObject!!.glDrawingMode != GL11.GL_QUADS) {
                    throw ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Invalid number of points for face (expected 3, found " + tokens.size + ")")
                }
            }

            val subTokens = tokens.map { it.split("/") }
            // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
            if (isValidFace_V_VT_VN_Line(line)) {
                face.vertices = Array(tokens.size) { vertices[subTokens[it][0].toInt() - 1] }
                face.textureCoordinates = Array(tokens.size) { textureCoordinates[subTokens[it][1].toInt() - 1] }
                face.vertexNormals = Array(tokens.size) { vertexNormals[subTokens[it][2].toInt() - 1] }
            } else if (isValidFace_V_VT_Line(line)) {
                face.vertices = Array(tokens.size) { vertices[subTokens[it][0].toInt() - 1] }
                face.textureCoordinates = Array(tokens.size) { textureCoordinates[subTokens[it][1].toInt() - 1] }
            } else if (isValidFace_V_VN_Line(line)) {
                face.vertices = Array(tokens.size) { vertices[subTokens[it][0].toInt() - 1] }
                face.vertexNormals = Array(tokens.size) { vertexNormals[subTokens[it][1].toInt() - 1] }
            } else if (isValidFace_V_Line(line)) {
                face.vertices = Array(tokens.size) { vertices[tokens[it].toInt() - 1] }
            } else {
                throw ModelFormatException("Error parsing entry ('$line', line $lineCount) in file '$fileName' - Incorrect format")
            }
            // force calc normal
            face.faceNormal
        } else {
            throw ModelFormatException("Error parsing entry ('$line', line $lineCount) in file '$fileName' - Incorrect format")
        }
        return face
    }

    @Throws(ModelFormatException::class)
    private fun parseGroupObject(line: String, lineCount: Int): GroupObject? {
        var group: GroupObject? = null
        if (isValidGroupObjectLine(line)) {
            val trimmedLine = line.substring(line.indexOf(" ") + 1)
            if (trimmedLine.isNotEmpty()) {
                group = GroupObject(trimmedLine)
            }
        } else {
            throw ModelFormatException("Error parsing entry ('$line', line $lineCount) in file '$fileName' - Incorrect format")
        }
        return group
    }

    fun getType(): String {
        return "obj"
    }


    /***
     * Verifies that the given line from the model file is a valid vertex
     * @param line the line being validated
     * @return true if the line is a valid vertex, false otherwise
     */
    private fun isValidVertexLine(line: String): Boolean {
        if (vertexMatcher != null) {
            vertexMatcher!!.reset()
        }
        vertexMatcher = vertexPattern.matcher(line)
        return vertexMatcher!!.matches()
    }

    /***
     * Verifies that the given line from the model file is a valid vertex normal
     * @param line the line being validated
     * @return true if the line is a valid vertex normal, false otherwise
     */
    private fun isValidVertexNormalLine(line: String): Boolean {
        if (vertexNormalMatcher != null) {
            vertexNormalMatcher!!.reset()
        }
        vertexNormalMatcher = vertexNormalPattern.matcher(line)
        return vertexNormalMatcher!!.matches()
    }

    /***
     * Verifies that the given line from the model file is a valid texture coordinate
     * @param line the line being validated
     * @return true if the line is a valid texture coordinate, false otherwise
     */
    private fun isValidTextureCoordinateLine(line: String): Boolean {
        if (textureCoordinateMatcher != null) {
            textureCoordinateMatcher!!.reset()
        }
        textureCoordinateMatcher = textureCoordinatePattern.matcher(line)
        return textureCoordinateMatcher!!.matches()
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by vertices, texture coordinates, and vertex normals
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1/vt1/vn1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private fun isValidFace_V_VT_VN_Line(line: String): Boolean {
        if (face_V_VT_VN_Matcher != null) {
            face_V_VT_VN_Matcher!!.reset()
        }
        face_V_VT_VN_Matcher = face_V_VT_VN_Pattern.matcher(line)
        return face_V_VT_VN_Matcher!!.matches()
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by vertices and texture coordinates
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1/vt1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private fun isValidFace_V_VT_Line(line: String): Boolean {
        if (face_V_VT_Matcher != null) {
            face_V_VT_Matcher!!.reset()
        }
        face_V_VT_Matcher = face_V_VT_Pattern.matcher(line)
        return face_V_VT_Matcher!!.matches()
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by vertices and vertex normals
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1//vn1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private fun isValidFace_V_VN_Line(line: String): Boolean {
        if (face_V_VN_Matcher != null) {
            face_V_VN_Matcher!!.reset()
        }
        face_V_VN_Matcher = face_V_VN_Pattern.matcher(line)
        return face_V_VN_Matcher!!.matches()
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by only vertices
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private fun isValidFace_V_Line(line: String): Boolean {
        if (face_V_Matcher != null) {
            face_V_Matcher!!.reset()
        }
        face_V_Matcher = face_V_Pattern.matcher(line)
        return face_V_Matcher!!.matches()
    }

    /***
     * Verifies that the given line from the model file is a valid face of any of the possible face formats
     * @param line the line being validated
     * @return true if the line is a valid face that matches any of the valid face formats, false otherwise
     */
    private fun isValidFaceLine(line: String): Boolean {
        return isValidFace_V_VT_VN_Line(line) || isValidFace_V_VT_Line(line) || isValidFace_V_VN_Line(line) || isValidFace_V_Line(
            line)
    }

    /***
     * Verifies that the given line from the model file is a valid group (or object)
     * @param line the line being validated
     * @return true if the line is a valid group (or object), false otherwise
     */
    private fun isValidGroupObjectLine(line: String): Boolean {
        if (groupObjectMatcher != null) {
            groupObjectMatcher!!.reset()
        }
        groupObjectMatcher = groupObjectPattern.matcher(line)
        return groupObjectMatcher!!.matches()
    }
}

@SideOnly(Side.CLIENT)
fun GroupObject.render(light: Int) {
    if (faces.size > 0) {
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        buffer.begin(glDrawingMode, Face.VERTEX_FORMAT)
        // TODO: brightness
        //tessellator.setBrightness(light)
        render(buffer)
        tessellator.draw()
    }
}

data class Vertex(val x: Float, val y: Float, val z: Float) {
    constructor(x: Float, y: Float) : this(x, y, 0f)
    fun toVec3d() = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())
}
data class TextureCoordinate(val u: Float, val v: Float) {
    constructor(u: Float, v: Float, w: Float) : this(u, v)
}

class GroupObject constructor(val name: String) {
    val faces = mutableListOf<Face>()
    var glDrawingMode = -1

    fun render() {
        if (faces.isNotEmpty()) {
            val tessellator: Tessellator = Tessellator.getInstance()
            val buffer = tessellator.buffer
            buffer.begin(glDrawingMode, Face.VERTEX_FORMAT)
            render(tessellator.buffer)
            tessellator.draw()
        }
    }

    fun render(buffer: BufferBuilder) {
        if (faces.isNotEmpty()) {
            for (face in faces) {
                face.addFaceForRender(buffer)
            }
        }
    }
}

class Face constructor() {
    lateinit var vertices: Array<Vertex>
    lateinit var textureCoordinates: Array<TextureCoordinate>
    val faceNormal: Vertex by lazy(LazyThreadSafetyMode.NONE) { calculateFaceNormal() }
    var vertexNormals: Array<Vertex>? = null

    fun calculateFaceNormal(): Vertex {
        val v1 = vertices[1].toVec3d() - vertices[0].toVec3d()
        val v2 = vertices[2].toVec3d() - vertices[0].toVec3d()
        val normalVector = v1.crossProduct(v2).normalize()

        return Vertex(normalVector.x.toFloat(), normalVector.y.toFloat(), normalVector.z.toFloat())
    }

    @SideOnly(Side.CLIENT)
    fun addFaceForRender(buffer: BufferBuilder) {
        addFaceForRender(buffer, 0.0005)
    }

    @SideOnly(Side.CLIENT)
    fun addFaceForRender(buffer: BufferBuilder, textureOffset: Double) {
        var averageU = 0f
        var averageV = 0f
        val textureCoordinates = textureCoordinates

        for (i in textureCoordinates.indices) {
            averageU += textureCoordinates[i].u
            averageV += textureCoordinates[i].v
        }
        averageU /= textureCoordinates.size
        averageV /= textureCoordinates.size

        for (i in vertices.indices) {
            buffer.pos(vertices[i].x.toDouble(), vertices[i].y.toDouble(), vertices[i].z.toDouble())
            var offsetU = textureOffset
            var offsetV = textureOffset
            if (textureCoordinates[i].u > averageU) {
                offsetU = -offsetU
            }
            if (textureCoordinates[i].v > averageV) {
                offsetV = -offsetV
            }
            buffer.tex(textureCoordinates[i].u + offsetU, textureCoordinates[i].v + offsetV)
            buffer.normal(faceNormal.x, faceNormal.y, faceNormal.z)
            buffer.endVertex()
        }
    }

    companion object {
        val VERTEX_FORMAT = DefaultVertexFormats.POSITION_TEX_NORMAL
    }
}

class ModelFormatException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
