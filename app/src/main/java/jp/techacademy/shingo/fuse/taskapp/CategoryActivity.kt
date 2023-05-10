package jp.techacademy.shingo.fuse.taskapp


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import jp.techacademy.shingo.fuse.taskapp.databinding.ActivityCategoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryBinding
    private lateinit var realm: Realm
    private lateinit var category: Category


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TaskRealmデータベースとの接続を開く
        val config = RealmConfiguration.create(schema = setOf(Category::class))
        realm = Realm.open(config)

        //カテゴリのデフォルト値変更
        category = Category()


        binding.saveButton.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                addCategory()
            }
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }
    }

    //カテゴリの登録または更新
    private suspend fun addCategory() {

        if (category.id == -1) {
            // 登録(更新)する値の取得
            val categoryName = binding.categoryEdit.text.toString()
            // 最大のid+1をセット
            category.id = (realm.query<Category>().max("id", Int::class).find() ?: -1) + 1
            // 画面項目の値で更新
            category.category = categoryName


            // 登録処理
            realm.writeBlocking {
                copyToRealm(category)
            }
        } else {
            // 更新
            realm.write {
                findLatest(category)?.apply {
                    // 画面項目の値で更新
                    this.category = category
                }
            }
        }
    }
}

