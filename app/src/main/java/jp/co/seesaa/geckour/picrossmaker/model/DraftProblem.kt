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
data class DraftProblem(@Setter("id") @PrimaryKey(autoincrement = true) val id: Long,
                        @Setter("title") @Column var title: String,
                        @Setter("keys_horizontal") @Column val keysHorizontal: Problem.Companion.KeysCluster,
                        @Setter("keys_vertical") @Column val keysVertical: Problem.Companion.KeysCluster,
                        @Setter("thumb") @Column @Nullable var thumb: Bitmap?,
                        @Setter("created_at") @Column val createdAt: Timestamp,
                        @Setter("edited_at") @Column var editedAt: Timestamp,
                        @Setter("catalog") @Column var catalog: Cell.Companion.Catalog) {

    constructor(id: Long = -1L,
                title: String = "no title",
                keysHorizontal: Problem.Companion.KeysCluster = Problem.Companion.KeysCluster(),
                keysVertical: Problem.Companion.KeysCluster = Problem.Companion.KeysCluster(),
                thumb: Bitmap? = null,
                catalog: Cell.Companion.Catalog = Cell.Companion.Catalog(ArrayList<Cell>())):
            this(id, title, keysHorizontal, keysVertical, thumb, Timestamp(System.currentTimeMillis()), Timestamp(System.currentTimeMillis()), catalog)
}