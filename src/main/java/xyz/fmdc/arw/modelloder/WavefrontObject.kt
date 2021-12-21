/**
 * This file is originally a part of MinecraftForge.
 * This file is available under and governed by the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
 * However, the original source was available under Forge Public License (https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/MinecraftForge-License.txt)
 */

package xyz.fmdc.arw.modelloder

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


// Currently this is set of empty methods
// TODO: migration
/**
 * Wavefront Object importer
 * Based heavily off of the specifications found at http://en.wikipedia.org/wiki/Wavefront_.obj_file
 */
//TODO: Split parser and parsed object for small memory
class WavefrontObject /*: IModelCustom*/ {
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
/*
    var vertices = ArrayList<Vertex>()
    var vertexNormals = ArrayList<Vertex>()
    var textureCoordinates = ArrayList<TextureCoordinate>()
    var groupObjects = ArrayList<GroupObject>()
    private var currentGroupObject: GroupObject? = null
    private var fileName: String
 */

    constructor(resource: ResourceLocation) {
        /*
        fileName = resource.toString()
        try {
            val res = Minecraft.getMinecraft().resourceManager.getResource(resource)
            loadObjModel(res.inputStream)
        } catch (e: IOException) {
            throw ModelFormatException("IO Exception reading model format", e)
        }
         */
    }

    constructor(filename: String, inputStream: InputStream) {
        /*
        fileName = filename
        loadObjModel(inputStream)
        */
    }

    /*@Throws(ModelFormatException::class)*/
    private fun loadObjModel(inputStream: InputStream) {/*
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
        }*/
    }

    @SideOnly(Side.CLIENT)
    /*override*/ fun renderAll() {/*
        val tessellator = Tessellator.instance
        if (currentGroupObject != null) {
            tessellator.startDrawing(currentGroupObject!!.glDrawingMode)
        } else {
            tessellator.startDrawing(GL11.GL_TRIANGLES)
        }
        tessellateAll(tessellator)
        tessellator.draw()*/
    }

    @SideOnly(Side.CLIENT)
    fun tessellateAll(tessellator: Tessellator?) {/*
        for (groupObject in groupObjects) {
            groupObject.render(tessellator)
        }*/
    }

    @SideOnly(Side.CLIENT)
    /*override*/ fun renderOnly(vararg groupNames: String) {/*
        for (groupObject in groupObjects) {
            for (groupName in groupNames) {
                if (groupName.equals(groupObject.name, ignoreCase = true)) {
                    groupObject.render()
                }
            }
        }*/
    }

    @SideOnly(Side.CLIENT)
    fun tessellateOnly(tessellator: Tessellator?, vararg groupNames: String) {
        /*for (groupObject in groupObjects) {
            for (groupName in groupNames) {
                if (groupName.equals(groupObject.name, ignoreCase = true)) {
                    groupObject.render(tessellator)
                }
            }
        }*/
    }

    @SideOnly(Side.CLIENT)
    /*override*/ fun renderPart(partName: String) {
        /*for (groupObject in groupObjects) {
            if (partName.equals(groupObject.name, ignoreCase = true)) {
                groupObject.render()
            }
        }*/
    }

    @SideOnly(Side.CLIENT)
    fun renderPart(partName: String, light: Int) {
        /*for (groupObject in groupObjects) {
            if (partName.equals(groupObject.name, ignoreCase = true)) {
                groupObject.render(light)
            }
        }*/
    }

    @SideOnly(Side.CLIENT)
    fun tessellatePart(tessellator: Tessellator?, partName: String) {
        /*for (groupObject in groupObjects) {
            if (partName.equals(groupObject.name, ignoreCase = true)) {
                groupObject.render(tessellator)
            }
        }*/
    }

    @SideOnly(Side.CLIENT)
    /*override*/ fun renderAllExcept(vararg excludedGroupNames: String) {
        /*for (groupObject in groupObjects) {
            var skipPart = false
            for (excludedGroupName in excludedGroupNames) {
                if (excludedGroupName.equals(groupObject.name, ignoreCase = true)) {
                    skipPart = true
                }
            }
            if (!skipPart) {
                groupObject.render()
            }
        }*/
    }

    @SideOnly(Side.CLIENT)
    fun tessellateAllExcept(tessellator: Tessellator?, vararg excludedGroupNames: String) {
        /*var exclude: Boolean
        for (groupObject in groupObjects) {
            exclude = false
            for (excludedGroupName in excludedGroupNames) {
                if (excludedGroupName.equals(groupObject.name, ignoreCase = true)) {
                    exclude = true
                }
            }
            if (!exclude) {
                groupObject.render(tessellator)
            }
        }*/
    }
/*
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
            var subTokens: Array<String>? = null
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

            // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
            if (isValidFace_V_VT_VN_Line(line)) {
                face.vertices = arrayOfNulls(tokens.size)
                face.textureCoordinates = arrayOfNulls(tokens.size)
                face.vertexNormals = arrayOfNulls(tokens.size)
                for (i in tokens.indices) {
                    subTokens = tokens[i].split("/").toTypedArray()
                    face.vertices[i] = vertices[subTokens[0].toInt() - 1]
                    face.textureCoordinates[i] = textureCoordinates[subTokens[1].toInt() - 1]
                    face.vertexNormals[i] = vertexNormals[subTokens[2].toInt() - 1]
                }
                face.faceNormal = face.calculateFaceNormal()
            } else if (isValidFace_V_VT_Line(line)) {
                face.vertices = arrayOfNulls(tokens.size)
                face.textureCoordinates = arrayOfNulls(tokens.size)
                for (i in tokens.indices) {
                    subTokens = tokens[i].split("/").toTypedArray()
                    face.vertices[i] = vertices[subTokens[0].toInt() - 1]
                    face.textureCoordinates[i] = textureCoordinates[subTokens[1].toInt() - 1]
                }
                face.faceNormal = face.calculateFaceNormal()
            } else if (isValidFace_V_VN_Line(line)) {
                face.vertices = arrayOfNulls(tokens.size)
                face.vertexNormals = arrayOfNulls(tokens.size)
                for (i in tokens.indices) {
                    subTokens = tokens[i].split("//").toTypedArray()
                    face.vertices[i] = vertices[subTokens[0].toInt() - 1]
                    face.vertexNormals[i] = vertexNormals[subTokens[1].toInt() - 1]
                }
                face.faceNormal = face.calculateFaceNormal()
            } else if (isValidFace_V_Line(line)) {
                face.vertices = arrayOfNulls(tokens.size)
                for (i in tokens.indices) {
                    face.vertices[i] = vertices[tokens[i].toInt() - 1]
                }
                face.faceNormal = face.calculateFaceNormal()
            } else {
                throw ModelFormatException("Error parsing entry ('$line', line $lineCount) in file '$fileName' - Incorrect format")
            }
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

    override fun getType(): String {
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
*/
}
/*

@SideOnly(Side.CLIENT)
fun GroupObject.render(light: Int) {
    if (faces.size > 0) {
        val tessellator = Tessellator.instance
        tessellator.startDrawing(glDrawingMode)
        tessellator.setBrightness(light)
        render(tessellator)
        tessellator.draw()
    }
}
*/
