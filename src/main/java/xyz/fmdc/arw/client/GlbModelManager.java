package xyz.fmdc.arw.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.AntiRaidWeapons;
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

    // ID (ResourceLocation) -> 読み込まれた GLB モデルデータ
    private final Map<ResourceLocation, GlbLoader.GlbModelData> models = new HashMap<>();

    // --- 事前に登録しておくモデル識別子 (ResourceLocation) ---
    public static final ResourceLocation OTO127MM_ID =
            ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/mk45mod4nla.glb");

    // 実際のファイル名に合わせて指定（ops-39-2mat.glb の場合はそちらに変更）
    public static final ResourceLocation OPS39_ID =
            ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/ops-39-2mat.glb");

    public static final ResourceLocation SPQ9B_ID =
            ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/spq9b.glb");

    public static final ResourceLocation EMMI_ID =
            ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "models/block/emmision-test-old.glb");

    private GlbModelManager() {}

    /**
     * リソースパック読み込み/リロード時に呼び出され、GLBファイルをパースしてキャッシュします。
     */
    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        models.clear();
        AntiRaidWeapons.LOGGER.info("[ARW-DEBUG] GLBモデルの一括ロードを開始します...");

        ResourceLocation[] targets = new ResourceLocation[] {
                OTO127MM_ID,
                OPS39_ID,
                SPQ9B_ID,
                EMMI_ID
        };

        for (ResourceLocation location : targets) {
            AntiRaidWeapons.LOGGER.info("[ARW-DEBUG] ロード試行中: {}", location);
            try {
                var resourceOpt = resourceManager.getResource(location);
                if (resourceOpt.isPresent()) {
                    AntiRaidWeapons.LOGGER.info("[ARW-DEBUG] ファイルを発見しました: {}", location);
                    try (InputStream is = resourceOpt.get().open()) {
                        GlbLoader.GlbModelData data = GlbLoader.loadGlb(is);
                        if (data.rootNode != null) {
                            models.put(location, data);
                            AntiRaidWeapons.LOGGER.info("[ARW-DEBUG] GLBロード成功: {}", location);
                        } else {
                            AntiRaidWeapons.LOGGER.error("[ARW-DEBUG] GLBパース失敗 (dataまたはrootNodeがnull): {}", location);
                        }
                    }
                } else {
                    AntiRaidWeapons.LOGGER.error("[ARW-DEBUG] GLBファイルが見つかりません: {}", location);
                }
            } catch (Throwable t) { // Exception だけでなく Error（ClassNotFound や OutOfMemory 等）も捕捉
                AntiRaidWeapons.LOGGER.error("[ARW-DEBUG] {} のロード中に深刻なエラーが発生しました", location, t);
            }
        }
        AntiRaidWeapons.LOGGER.info("[ARW-DEBUG] GLBモデルの一括ロード処理が完了しました。総ロード数: {}", models.size());
    }

    /**
     * IDを指定して読み込み済みのモデルデータを取得します。
     */
    public GlbLoader.GlbModelData getModel(ResourceLocation location) {
        GlbLoader.GlbModelData data = models.get(location);
        if (data == null) {
            AntiRaidWeapons.LOGGER.error("[ARW-DEBUG] getModel 失敗: 要求キー '{}' | キャッシュ済キー一覧: {}",
                    location, models.keySet());
        }
        return models.get(location);
    }
}