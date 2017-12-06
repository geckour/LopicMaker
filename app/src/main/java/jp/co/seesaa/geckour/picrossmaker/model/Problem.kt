package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Bitmap
import android.support.annotation.Nullable
import com.github.gfx.android.orma.annotation.*
import java.sql.Timestamp
import java.util.*

@Table
data class Problem(@Setter("id") @PrimaryKey(autoincrement = true) val id: Long = -1L,
                   @Setter("title") @Column(indexed = true) var title: String = "no title",
                   @Setter("genres") @Column val genres: List<String> = listOf(),
                   @Setter("keys_horizontal") @Column val keysHorizontal: KeysCluster = KeysCluster(),
                   @Setter("keys_vertical") @Column val keysVertical: KeysCluster = KeysCluster(),
                   @Setter("thumb") @Column @Nullable var thumb: Bitmap?,
                   @Setter("created_at") @Column val createdAt: Timestamp = Timestamp(System.currentTimeMillis()),
                   @Setter("edited_at") @Column var editedAt: Timestamp = Timestamp(System.currentTimeMillis()),
                   @Setter("catalog") @Column var catalog: Cell.Catalog = Cell.Catalog(ArrayList())) {

    class KeysCluster(vararg val keys: List<Int>)
}