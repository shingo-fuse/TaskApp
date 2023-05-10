package jp.techacademy.shingo.fuse.taskapp

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.io.Serializable

class Category:  RealmObject, Serializable {
    @PrimaryKey
    var id = -1

    var category = ""//カテゴリ
    override fun toString(): String {
        return category
    }
}