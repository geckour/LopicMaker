package jp.co.seesaa.geckour.picrossmaker.model.adapter;

import com.github.gfx.android.orma.annotation.StaticTypeAdapter;
import com.google.gson.Gson;

import jp.co.seesaa.geckour.picrossmaker.model.Cell.Catalog;

@StaticTypeAdapter(targetType = Catalog.class, serializedType = String.class)
public class CellCatalogAdapter {
    public static String serialize(Catalog catalog) {
        Gson gson = new Gson();
        return gson.toJson(catalog);
    }

    public static Catalog deserialize(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Catalog.class);
    }
}
