package xyz.fmdc.arw.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.client.util.FastGlbModel;
import xyz.fmdc.arw.client.util.GlbLoader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * GLBモデルを一括読み込み・保持・提供する管理クラス。
 * リソースパックのリロード（F3+T）にも自動対応します。
 */
public class GlbModelManager implements ResourceManagerReloadListener {

    public static final GlbModelManager INSTANCE = new GlbModelManager();

    // ID -> VBO化されたモデルデータ
    private final Map<ResourceLocation, FastGlbModel> fastModels = new HashMap<>();

    public static final ResourceLocation OTO127MM_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/oto127mm_stealth.glb");
    public static final ResourceLocation MK45MOD4_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/mk45mod4nla.glb");
    public static final ResourceLocation PHALANX_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/phalanx.glb");
    public static final ResourceLocation OPS39_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/ops-39-2mat.glb");
    public static final ResourceLocation SPQ9B_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/spq9b.glb");
    public static final ResourceLocation EMMI_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/emmision-test-old.glb");
    public static final ResourceLocation ATAGO_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/atago.glb");
    public static final ResourceLocation MK13GMLS_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/mk13-gmls.glb");
    public static final ResourceLocation RIM66M2_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/entity/rim66m2.glb");
    public static final ResourceLocation MK13GMLS_ID = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/mk13-gmls.glb");

    private GlbModelManager() {}

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        // メインスレッド（RenderThread）で既存VBO破棄 & 新規VBO生成を実行
        RenderSystem.recordRenderCall(() -> {
            // 既存 VBO の破棄 (VRAM解放)
            fastModels.values().forEach(FastGlbModel::close);
            fastModels.clear();

            AntiRaidWeapons.LOGGER.info("[ARW-DEBUG] GLBモデル(VBO)の一括ロードを開始します...");

            ResourceLocation[] targets = new ResourceLocation[] {
                    OTO127MM_ID, MK45MOD4_ID, OPS39_ID, SPQ9B_ID, EMMI_ID, ATAGO_ID, PHALANX_ID, RIM66M2_ID
                    OTO127MM_ID, MK45MOD4_ID, OPS39_ID, SPQ9B_ID, EMMI_ID, ATAGO_ID, PHALANX_ID, MK13GMLS_ID
            };

            for (ResourceLocation location : targets) {
                try {
                    var resourceOpt = resourceManager.getResource(location);
                    if (resourceOpt.isPresent()) {
                        try (InputStream is = resourceOpt.get().open()) {
                            GlbLoader.GlbModelData rawData = GlbLoader.loadGlb(is);
                            if (rawData.rootNode != null) {
                                // テクスチャ未設定マテリアルに対して textures/... 以下の同名テクスチャを自動探索・割り当て
                                String path = location.getPath();
                                if (path.startsWith("models/") && path.endsWith(".glb")) {
                                    String texPath = "textures/" + path.substring("models/".length(), path.length() - 4) + ".png";
                                    ResourceLocation candidateTex = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), texPath);
                                    if (resourceManager.getResource(candidateTex).isPresent()) {
                                        for (GlbLoader.MaterialInfo mat : rawData.materials) {
                                            if (mat.textureLocation == null) {
                                                mat.textureLocation = candidateTex;
                                            }
                                        }
                                    }
                                }

                                // VBO化
                                FastGlbModel fastModel = new FastGlbModel(rawData);
                                fastModels.put(location, fastModel);
                                AntiRaidWeapons.LOGGER.info("[ARW-DEBUG] GLB-VBO構築成功: {}", location);
                            }
                        }
                    }
                } catch (Throwable t) {
                    AntiRaidWeapons.LOGGER.error("[ARW-DEBUG] {} のロード中にエラーが発生しました", location, t);
                }
            }
        });
    }

    public FastGlbModel getFastModel(ResourceLocation location) {
        return fastModels.get(location);
    }
}