package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Bitmap
import android.support.annotation.Nullable
import com.github.gfx.android.orma.annotation.*
import java.sql.Timestamp

@Table
data class Problem(@Setter("id") @PrimaryKey(autoincrement = true) val id: Long,
                   @Setter("title") @Column var title: String,
                   @Setter("keys_horizontal") @Column val keysHorizontal: KeysCluster,
                   @Setter("keys_vertical") @Column val keysVertical: KeysCluster,
                   @Setter("thumb") @Column @Nullable var thumb: Bitmap?,
                   @Setter("created_at") @Column val createdAt: Timestamp,
                   @Setter("edited_at") @Column var editedAt: Timestamp) {

    constructor(id: Long, title: String, keysHorizontal: KeysCluster, keysVertical: KeysCluster, thumb: Bitmap?):
            this(id, title, keysHorizontal, keysVertical, thumb, Timestamp(System.currentTimeMillis()), Timestamp(System.currentTimeMillis()))

    constructor(id: Long = -1L, title: String = "no title", keysHorizontal: KeysCluster = KeysCluster(), keysVertical: KeysCluster = KeysCluster()):
            this(id, title, keysHorizontal, keysVertical, null)

    companion object {
        class KeysCluster(vararg val keys: List<Int>)
    }
}