package jp.co.seesaa.geckour.picrossmaker.model.adapter;

import android.support.annotation.NonNull;

import com.github.gfx.android.orma.annotation.StaticTypeAdapter;
import com.google.gson.Gson;

import jp.co.seesaa.geckour.picrossmaker.model.Problem.Companion.KeysCluster;

@StaticTypeAdapter(targetType = KeysCluster.class, serializedType = String.class)
public class KeysClusterAdapter {
    @NonNull
    public static String serialize(@NonNull KeysCluster cluster) {
        Gson gson = new Gson();
        return gson.toJson(cluster);
    }

    @NonNull
    public static KeysCluster deserialize(@NonNull String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, KeysCluster.class);
    }
}
