package jp.co.seesaa.geckour.picrossmaker.model.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.github.gfx.android.orma.annotation.StaticTypeAdapter;

import java.io.ByteArrayOutputStream;

@StaticTypeAdapter(targetType = Bitmap.class, serializedType = String.class)
public class BitmapAdapter {
    public static String serialize(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
        }
    }

    public static Bitmap deserialize(String base64String) {
        if (base64String == null) {
            return null;
        } else {
            byte[] byteArray = Base64.decode(base64String, 0);
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        }
    }
}
