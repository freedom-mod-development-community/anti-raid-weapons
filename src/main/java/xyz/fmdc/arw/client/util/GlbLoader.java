package xyz.fmdc.arw.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.fmdc.arw.AntiRaidWeapons;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlbLoader {

    // マテリアル表現用Enum
    public enum AlphaMode {
        OPAQUE,      // 不透明
        MASK,        // 打ち抜き透明（Cutout: 葉っぱやフェンス）
        BLEND        // 透過（Translucent: ガラスや半透明）
    }

    // パースしたマテリアル情報
    public static class MaterialInfo {
        public ResourceLocation textureLocation;
        public AlphaMode alphaMode = AlphaMode.OPAQUE;
        public boolean isEmissive = false; // 発光（自体発光）フラグ
    }

    public static class AnimationChannel {
        public String targetNodeName;
        public String path; // "translation", "rotation", "scale"
        public float[] keyframeTimes;
        public float[] keyframeValues;
    }

    public static class GlbAnimation {
        public String name;
        public float maxTime = 0.0f;
        public List<AnimationChannel> channels = new ArrayList<>();
    }

    public static class GlbNode {
        public String name;
        public Vector3f translation = new Vector3f();
        public Quaternionf rotation = new Quaternionf();
        public Vector3f scale = new Vector3f(1, 1, 1);
        public List<GlbNode> children = new ArrayList<>();
        public List<MeshPart> meshParts = new ArrayList<>();
    }

    public static class MeshPart {
        public float[] positions;
        public float[] normals;
        public float[] uvs;
        public int[] indices;
        public MaterialInfo material;
        public float[] baseColorFactor = new float[]{1.0f, 1.0f, 1.0f, 1.0f}; // RGB+Alpha マテリアル色
    }

    public static class GlbModelData {
        public GlbNode rootNode;
        public Map<String, GlbAnimation> animations = new HashMap<>();
        public List<MaterialInfo> materials = new ArrayList<>();
    }

    public static GlbModelData loadGlb(InputStream stream) throws Exception {
        GlbModelData modelData = new GlbModelData();

        // 1. ヘッダー解析
        byte[] header = readBytes(stream, 12);
        ByteBuffer headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        if (headerBuffer.getInt() != 0x46546C67) throw new IllegalArgumentException("Invalid GLB magic!");
        headerBuffer.getInt(); // version

        // Chunk 0: JSON
        byte[] chunk0Header = readBytes(stream, 8);
        ByteBuffer c0Buffer = ByteBuffer.wrap(chunk0Header).order(ByteOrder.LITTLE_ENDIAN);
        int chunk0Length = c0Buffer.getInt();
        byte[] jsonBytes = readBytes(stream, chunk0Length);
        JsonObject json = JsonParser.parseString(new String(jsonBytes, StandardCharsets.UTF_8)).getAsJsonObject();

        // Chunk 1: BIN (バイナリデータバッファ)
        byte[] chunk1Header = readBytes(stream, 8);
        ByteBuffer c1Buffer = ByteBuffer.wrap(chunk1Header).order(ByteOrder.LITTLE_ENDIAN);
        int chunk1Length = c1Buffer.getInt();
        byte[] binBytes = readBytes(stream, chunk1Length);
        ByteBuffer binBuffer = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN);

        // 2. テクスチャ画像のロードと DynamicTexture 登録 (images チャンクの解析)
        List<ResourceLocation> imagesList = parseAndRegisterImages(json, binBuffer);

        // 3. マテリアル構造の解析
        if (json.has("materials")) {
            JsonArray jsonMaterials = json.getAsJsonArray("materials");
            for (JsonElement elem : jsonMaterials) {
                JsonObject matObj = elem.getAsJsonObject();
                MaterialInfo mat = new MaterialInfo();

                // アルファモードの取得
                if (matObj.has("alphaMode")) {
                    String mode = matObj.get("alphaMode").getAsString();
                    if ("BLEND".equalsIgnoreCase(mode)) mat.alphaMode = AlphaMode.BLEND;
                    else if ("MASK".equalsIgnoreCase(mode)) mat.alphaMode = AlphaMode.MASK;
                }

                // 発光（Emissive）判定
                if (matObj.has("emissiveTexture") ||
                        (matObj.has("emissiveFactor") && !matObj.getAsJsonArray("emissiveFactor").isEmpty())) {
                    mat.isEmissive = true;
                }

                // ベースカラーテクスチャの ResourceLocation 解決
                mat.textureLocation = resolveTextureLocation(json, matObj, imagesList);

                modelData.materials.add(mat);
            }
        }

        // 4. メッシュ・ノード構築 (マテリアル情報を伝達できるように修正)
        modelData.rootNode = parseNodeHierarchy(json, binBuffer, modelData.materials);

        // 5. アニメーション構造の解析
        if (json.has("animations")) {
            JsonArray animsArray = json.getAsJsonArray("animations");
            for (int i = 0; i < animsArray.size(); i++) {
                JsonObject animJson = animsArray.get(i).getAsJsonObject();
                GlbAnimation anim = new GlbAnimation();
                anim.name = animJson.has("name") ? animJson.get("name").getAsString() : "anim_" + i;

                JsonArray samplers = animJson.getAsJsonArray("samplers");
                JsonArray channels = animJson.getAsJsonArray("channels");

                for (JsonElement chElement : channels) {
                    JsonObject chObj = chElement.getAsJsonObject();
                    int samplerIdx = chObj.get("sampler").getAsInt();
                    JsonObject target = chObj.getAsJsonObject("target");
                    int nodeIdx = target.get("node").getAsInt();
                    String path = target.get("path").getAsString();

                    JsonObject samplerObj = samplers.get(samplerIdx).getAsJsonObject();
                    int inputAccessorIdx = samplerObj.get("input").getAsInt();
                    int outputAccessorIdx = samplerObj.get("output").getAsInt();

                    AnimationChannel channel = new AnimationChannel();
                    channel.targetNodeName = getNodeNameById(json, nodeIdx);
                    channel.path = path;
                    channel.keyframeTimes = readAccessorFloats(json, binBuffer, inputAccessorIdx);
                    channel.keyframeValues = readAccessorFloats(json, binBuffer, outputAccessorIdx);

                    if (channel.keyframeTimes.length > 0) {
                        float max = channel.keyframeTimes[channel.keyframeTimes.length - 1];
                        if (max > anim.maxTime) anim.maxTime = max;
                    }
                    anim.channels.add(channel);
                }
                modelData.animations.put(anim.name, anim);
            }
        }

        return modelData;
    }

    /**
     * GLB内の images 配列を解析し、バイナリから NativeImage を作成して DynamicTexture として登録・リスト化する
     */
    private static List<ResourceLocation> parseAndRegisterImages(JsonObject json, ByteBuffer binBuffer) {
        List<ResourceLocation> imagesList = new ArrayList<>();
        if (!json.has("images")) return imagesList;

        JsonArray jsonImages = json.getAsJsonArray("images");
        JsonArray bufferViews = json.has("bufferViews") ? json.getAsJsonArray("bufferViews") : null;

        for (int i = 0; i < jsonImages.size(); i++) {
            JsonObject imgObj = jsonImages.get(i).getAsJsonObject();
            ResourceLocation registeredLoc = null;

            // GLB 埋め込みバイナリ (bufferView) の場合
            if (imgObj.has("bufferView") && bufferViews != null) {
                int bufferViewIdx = imgObj.get("bufferView").getAsInt();
                JsonObject bView = bufferViews.get(bufferViewIdx).getAsJsonObject();

                int byteOffset = bView.has("byteOffset") ? bView.get("byteOffset").getAsInt() : 0;
                int byteLength = bView.get("byteLength").getAsInt();

                byte[] imageBytes = new byte[byteLength];
                binBuffer.position(byteOffset);
                binBuffer.get(imageBytes);

                try (InputStream in = new ByteArrayInputStream(imageBytes)) {
                    NativeImage nativeImage = NativeImage.read(in);
                    DynamicTexture dynTexture = new DynamicTexture(nativeImage);

                    // DynamicTexture として Minecraft の TextureManager に登録
                    registeredLoc = ResourceLocation.fromNamespaceAndPath(
                            AntiRaidWeapons.MOD_ID, "dynamic_glb_tex_" + System.nanoTime() + "_" + i
                    );
                    Minecraft.getInstance().getTextureManager().register(registeredLoc, dynTexture);
                } catch (Exception e) {
                    AntiRaidWeapons.LOGGER.error("Failed to register dynamic texture from GLB", e);
                }

                // 外部参照 (uri) の場合
            } else if (imgObj.has("uri")) {
                String uri = imgObj.get("uri").getAsString();
                registeredLoc = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, uri);
            }

            // 画像ロードに失敗した場合でもインデックス整合性のために null をいれて保持
            imagesList.add(registeredLoc);
        }

        return imagesList;
    }

    /**
     * material オブジェクトおよび glTF 仕様の textures 配列を経由して ResourceLocation を解決する
     */
    private static ResourceLocation resolveTextureLocation(JsonObject rootJson, JsonObject matObj, List<ResourceLocation> imagesList) {
        try {
            if (!matObj.has("pbrMetallicRoughness")) return null;
            JsonObject pbr = matObj.getAsJsonObject("pbrMetallicRoughness");

            if (!pbr.has("baseColorTexture")) return null;
            int textureIndex = pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt();

            // glTF 仕様: textures[textureIndex].source が images 配列のインデックスを指す
            int imageIndex = textureIndex; // デフォルト (1:1 対応フォールバック)
            if (rootJson.has("textures")) {
                JsonArray textures = rootJson.getAsJsonArray("textures");
                if (textureIndex < textures.size()) {
                    JsonObject texObj = textures.get(textureIndex).getAsJsonObject();
                    if (texObj.has("source")) {
                        imageIndex = texObj.get("source").getAsInt();
                    }
                }
            }

            if (imageIndex >= 0 && imageIndex < imagesList.size()) {
                return imagesList.get(imageIndex);
            }
        } catch (Exception e) {
            AntiRaidWeapons.LOGGER.warn("Failed to resolve texture location in GLB", e);
        }
        return null;
    }

    private static GlbNode parseNodeHierarchy(JsonObject json, ByteBuffer bin, List<MaterialInfo> materials) {
        if (!json.has("nodes")) return new GlbNode();

        JsonArray nodesArray = json.getAsJsonArray("nodes");
        int nodeCount = nodesArray.size();

        // 1. 全ノードをインスタンス化して配列に保持
        GlbNode[] allNodes = new GlbNode[nodeCount];
        boolean[] isChild = new boolean[nodeCount]; // 親が存在するかの判定用

        for (int i = 0; i < nodeCount; i++) {
            JsonObject nodeObj = nodesArray.get(i).getAsJsonObject();
            GlbNode node = new GlbNode();
            node.name = nodeObj.has("name") ? nodeObj.get("name").getAsString() : "node_" + i;

            // トランスフォームの読み込み
            if (nodeObj.has("translation")) {
                JsonArray t = nodeObj.getAsJsonArray("translation");
                node.translation.set(t.get(0).getAsFloat(), t.get(1).getAsFloat(), t.get(2).getAsFloat());
            }
            if (nodeObj.has("rotation")) {
                JsonArray r = nodeObj.getAsJsonArray("rotation");
                node.rotation.set(r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat(), r.get(3).getAsFloat());
            }
            if (nodeObj.has("scale")) {
                JsonArray s = nodeObj.getAsJsonArray("scale");
                node.scale.set(s.get(0).getAsFloat(), s.get(1).getAsFloat(), s.get(2).getAsFloat());
            }

            // メッシュ読み込み
            if (nodeObj.has("mesh")) {
                int meshIdx = nodeObj.get("mesh").getAsInt();
                parseMesh(json, bin, meshIdx, node, materials);
            }

            allNodes[i] = node;
        }

        // 2. glTF の children フィールドを走査し、正しい親子関係（ツリー構造）を構築
        for (int i = 0; i < nodeCount; i++) {
            JsonObject nodeObj = nodesArray.get(i).getAsJsonObject();
            if (nodeObj.has("children")) {
                JsonArray childrenArray = nodeObj.getAsJsonArray("children");
                for (JsonElement childElem : childrenArray) {
                    int childIdx = childElem.getAsInt();
                    if (childIdx >= 0 && childIdx < nodeCount) {
                        allNodes[i].children.add(allNodes[childIdx]);
                        isChild[childIdx] = true; // 他のノードの子であることが確定
                    }
                }
            }
        }

        // 3. ルートノードの決定（どのノードの children にもなっていない最上位ノードを集める）
        GlbNode root = new GlbNode();
        root.name = "root";

        // glTF 内で scenes が定義されている場合はシーンの nodes を優先参照、なければ親のないノードを格納
        if (json.has("scenes") && !json.getAsJsonArray("scenes").isEmpty()) {
            JsonObject scene = json.getAsJsonArray("scenes").get(0).getAsJsonObject();
            if (scene.has("nodes")) {
                for (JsonElement nElem : scene.getAsJsonArray("nodes")) {
                    int rootNodeIdx = nElem.getAsInt();
                    root.children.add(allNodes[rootNodeIdx]);
                }
                return root;
            }
        }

        // フォールバック: 親が存在しないトップレベルノードを root の子にする
        for (int i = 0; i < nodeCount; i++) {
            if (!isChild[i]) {
                root.children.add(allNodes[i]);
            }
        }

        return root;
    }

    private static void parseMesh(JsonObject json, ByteBuffer bin, int meshIdx, GlbNode node, List<MaterialInfo> materials) {
        JsonObject meshObj = json.getAsJsonArray("meshes").get(meshIdx).getAsJsonObject();
        JsonArray primitives = meshObj.getAsJsonArray("primitives");

        for (JsonElement primElem : primitives) {
            JsonObject prim = primElem.getAsJsonObject();
            JsonObject attributes = prim.getAsJsonObject("attributes");

            MeshPart part = new MeshPart();

            // POSITION
            if (attributes.has("POSITION")) {
                part.positions = readAccessorFloats(json, bin, attributes.get("POSITION").getAsInt());
            }
            // NORMAL
            if (attributes.has("NORMAL")) {
                part.normals = readAccessorFloats(json, bin, attributes.get("NORMAL").getAsInt());
            }
            // TEXCOORD_0
            if (attributes.has("TEXCOORD_0")) {
                part.uvs = readAccessorFloats(json, bin, attributes.get("TEXCOORD_0").getAsInt());
            }
            // INDICES
            if (prim.has("indices")) {
                part.indices = readAccessorInts(json, bin, prim.get("indices").getAsInt());
            }

            // MATERIAL (BaseColor & MaterialInfo 参照紐付け)
            if (prim.has("material")) {
                int matIdx = prim.get("material").getAsInt();

                // 解析済みの MaterialInfo を MeshPart に割り当て
                if (materials != null && matIdx < materials.size()) {
                    part.material = materials.get(matIdx);
                }

                JsonObject matObj = json.getAsJsonArray("materials").get(matIdx).getAsJsonObject();
                if (matObj.has("pbrMetallicRoughness")) {
                    JsonObject pbr = matObj.getAsJsonObject("pbrMetallicRoughness");
                    if (pbr.has("baseColorFactor")) {
                        JsonArray col = pbr.getAsJsonArray("baseColorFactor");
                        part.baseColorFactor = new float[]{
                                col.get(0).getAsFloat(),
                                col.get(1).getAsFloat(),
                                col.get(2).getAsFloat(),
                                col.get(3).getAsFloat()
                        };
                    }
                }
            }

            node.meshParts.add(part);
        }
    }

    private static float[] readAccessorFloats(JsonObject json, ByteBuffer bin, int accessorIdx) {
        JsonObject accessor = json.getAsJsonArray("accessors").get(accessorIdx).getAsJsonObject();
        int bufferViewIdx = accessor.get("bufferView").getAsInt();
        int count = accessor.get("count").getAsInt();
        String type = accessor.get("type").getAsString();

        JsonObject bufferView = json.getAsJsonArray("bufferViews").get(bufferViewIdx).getAsJsonObject();
        int byteOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        if (accessor.has("byteOffset")) byteOffset += accessor.get("byteOffset").getAsInt();

        int numComponents = switch (type) {
            case "SCALAR"       -> 1;
            case "VEC2"         -> 2;
            case "VEC3"         -> 3;
            case "VEC4", "MAT2" -> 4;
            case "MAT3"         -> 9;
            case "MAT4"         -> 16;
            default -> throw new IllegalArgumentException("Unsupported accessor type: " + type);
        };

        float[] result = new float[count * numComponents];
        bin.position(byteOffset);
        for (int i = 0; i < result.length; i++) {
            result[i] = bin.getFloat();
        }
        return result;
    }

    private static int[] readAccessorInts(JsonObject json, ByteBuffer bin, int accessorIdx) {
        JsonObject accessor = json.getAsJsonArray("accessors").get(accessorIdx).getAsJsonObject();
        int bufferViewIdx = accessor.get("bufferView").getAsInt();
        int count = accessor.get("count").getAsInt();
        int componentType = accessor.get("componentType").getAsInt(); // 5123 = UNSIGNED_SHORT, 5125 = UNSIGNED_INT

        JsonObject bufferView = json.getAsJsonArray("bufferViews").get(bufferViewIdx).getAsJsonObject();
        int byteOffset = bufferView.has("byteOffset") ? bufferView.get("byteOffset").getAsInt() : 0;
        if (accessor.has("byteOffset")) byteOffset += accessor.get("byteOffset").getAsInt();

        int[] result = new int[count];
        bin.position(byteOffset);

        for (int i = 0; i < count; i++) {
            if (componentType == 5123) {
                result[i] = bin.getShort() & 0xFFFF;
            } else if (componentType == 5125) {
                result[i] = bin.getInt();
            } else {
                result[i] = bin.get() & 0xFF;
            }
        }
        return result;
    }

    private static byte[] readBytes(InputStream stream, int length) throws IOException {
        byte[] data = stream.readNBytes(length);
        if (data.length < length) {
            throw new IllegalArgumentException("Unexpected EOF: expected " + length + " bytes");
        }
        return data;
    }

    private static String getNodeNameById(JsonObject json, int nodeIdx) {
        JsonObject nodeObj = json.getAsJsonArray("nodes").get(nodeIdx).getAsJsonObject();
        return nodeObj.has("name") ? nodeObj.get("name").getAsString() : "node_" + nodeIdx;
    }
}