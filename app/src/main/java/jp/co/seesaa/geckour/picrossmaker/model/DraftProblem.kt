package jp.co.seesaa.geckour.picrossmaker.model

import android.graphics.Bitmap
import android.support.annotation.Nullable
import com.github.gfx.android.orma.annotation.Column
import com.github.gfx.android.orma.annotation.PrimaryKey
import com.github.gfx.android.orma.annotation.Setter
import com.github.gfx.android.orma.annotation.Table
import java.sql.Timestamp
import java.util.*

@Table
data class DraftProblem(
        @Setter("id") @PrimaryKey(autoincrement = true) val id: Long = -1L,
        @Setter("title") @Column var title: String = "no title",
        @Setter("keys_horizontal") @Column val keysHorizontal: Problem.KeysCluster = Problem.KeysCluster(),
        @Setter("keys_vertical") @Column val keysVertical: Problem.KeysCluster = Problem.KeysCluster(),
        @Setter("thumb") @Column @Nullable var thumb: Bitmap? = null,
        @Setter("created_at") @Column val createdAt: Timestamp = Timestamp(System.currentTimeMillis()),
        @Setter("edited_at") @Column var editedAt: Timestamp = Timestamp(System.currentTimeMillis()),
        @Setter("catalog") @Column var catalog: Cell.Catalog = Cell.Catalog(ArrayList())
)