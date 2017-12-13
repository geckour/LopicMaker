package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Bitmap
import android.support.annotation.Nullable
import com.github.gfx.android.orma.annotation.*
import java.sql.Timestamp
import java.util.*

@Table
data class Problem(
        @Setter("id") @PrimaryKey(autoincrement = true) val id: Long = -1L,
        @Setter("title") @Column(indexed = true) var title: String = "no title",
        @Setter("draft") @Column(indexed = true) var draft: Boolean = true,
        @Setter("tags") @Column val tags: List<String> = listOf(),
        @Setter("keys_horizontal") @Column val keysHorizontal: KeysCluster = KeysCluster(),
        @Setter("keys_vertical") @Column val keysVertical: KeysCluster = KeysCluster(),
        @Setter("thumb") @Column @Nullable var thumb: Bitmap?,
        @Setter("created_at") @Column val createdAt: Timestamp = Timestamp(System.currentTimeMillis()),
        @Setter("edited_at") @Column var editedAt: Timestamp = Timestamp(System.currentTimeMillis()),
        @Setter("source") @Column var source: Source = Source.OWN,
        @Setter("catalog") @Column var catalog: Cell.Catalog = Cell.Catalog(ArrayList())
) {
    enum class Source {
        OWN,
        SERVER_ORIGIN,
        SERVER_OTHER,
        OTHER
    }

    class KeysCluster(vararg val keys: List<Int>)

    @StaticTypeAdapter(targetType = Source::class, serializedType = String::class)
    class SourceSerializer {
        companion object {
            @JvmStatic
            fun serialize(source: Source): String = source.name

            @JvmStatic
            fun deserialize(name: String): Source = Source.valueOf(name)
        }
    }
}