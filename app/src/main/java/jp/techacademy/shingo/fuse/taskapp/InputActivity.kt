package jp.techacademy.shingo.fuse.taskapp

import android.R
import jp.techacademy.shingo.fuse.taskapp.databinding.ActivityInputBinding
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*



    class InputActivity : AppCompatActivity() {
        private lateinit var binding: ActivityInputBinding
        private lateinit var realm1: Realm
        private lateinit var realm2: Realm
        private lateinit var task: Task
        private lateinit var category: Category
        private var calendar = Calendar.getInstance()
        private var spinnerItems: List<Category>? = null


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityInputBinding.inflate(layoutInflater)
            setContentView(binding.root)


            // アクションバーの設定
            setSupportActionBar(binding.toolbar)
            if (supportActionBar != null) {
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            }

            // ボタンのイベントリスナーの設定
            binding.content.dateButton.setOnClickListener(dateClickListener)
            binding.content.timeButton.setOnClickListener(timeClickListener)
            binding.content.doneButton.setOnClickListener(doneClickListener)


            // Realmデータベースとの接続を開く
            val config1 = RealmConfiguration.create(schema = setOf(Task::class))
            realm1 = Realm.open(config1)

            // spinerのRealmデータベースとの接続を開く
            val config2 = RealmConfiguration.create(schema = setOf(Category::class))
            realm2 = Realm.open(config2)

            // EXTRA_TASKからTaskのidを取得
            val intent = intent
            val taskId = intent.getIntExtra(EXTRA_TASK, -1)


            //追加設定をする
            binding.content.additionButton.setOnClickListener {
                val intent1 = Intent(this, CategoryActivity::class.java)
                startActivity(intent1)

            }

            //カテゴリの登録
            spinnerItems = realm2.query<Category>().sort("id", Sort.DESCENDING).find()
            // ArrayAdapter
            val adapter = ArrayAdapter(
                applicationContext,
                R.layout.simple_spinner_item, spinnerItems as RealmResults<Category>
            )

            adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

            binding.content.spinner.adapter = adapter
            binding.content.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                //　アイテムが選択された時

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?, position: Int, id: Long
                ) {

                    category = binding.content.spinner.selectedItem as Category

                }

                //　アイテムが選択されなかった
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    //
                }
            }

            // タスクを取得または初期化
            initTask(taskId)
            category  = Category()
        }


        override fun onDestroy() {
            super.onDestroy()
            // Realmデータベースとの接続を閉じる
            realm1.close()
            realm2.close()
        }


        /**
         * 日付選択ボタン
         */
        private val dateClickListener = View.OnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    setDateTimeButtonText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        /**
         * 時刻選択ボタン
         */
        private val timeClickListener = View.OnClickListener {
            val timePickerDialog = TimePickerDialog(
                this,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    setDateTimeButtonText()
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
            )
            timePickerDialog.show()
        }

        /**
         * 決定ボタン
         */
        private val doneClickListener = View.OnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                addTask()
                finish()
            }
        }


        private fun initTask(taskId: Int) {
            // 引数のtaskIdに合致するタスクを検索
            val findTask = realm1.query<Task>("id==$taskId").first().find()


            if (findTask == null) {
                // 新規作成の場合
                task = Task()
                task.id = -1


                // 日付の初期値を1日後に設定
                calendar.add(Calendar.DAY_OF_MONTH, 1)

            } else {
                // 更新の場合
                task = findTask

                // taskの日時をcalendarに反映
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)
                calendar.time = simpleDateFormat.parse(task.date) as Date

                // taskの値を画面項目に反映
                binding.content.titleEditText.setText(task.title)
                binding.content.contentEditText.setText(task.contents)

                val categoryPosition = spinnerItems?.indexOfFirst { it.id == task.categoryId } ?: -1
                binding.content.spinner.setSelection(categoryPosition)


                }


                // 日付と時刻のボタンの表示を設定
                setDateTimeButtonText()
            }



        /**
         * タスクの登録または更新を行う
         */
        private suspend fun addTask() {
            // 日付型オブジェクトを文字列に変換用
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)

            // 登録（更新）する値を取得
            val title = binding.content.titleEditText.text.toString()
            val content = binding.content.contentEditText.text.toString()
            val date = simpleDateFormat.format(calendar.time)

            if (task.id == -1) {
                // 登録

                // 最大のid+1をセット
                task.id = (realm1.query<Task>().max("id", Int::class).find() ?: -1) + 1
                // 画面項目の値で更新
                task.title = title
                task.contents = content
                task.date = date
                task.categoryId = category.id // カテゴリのIDを設定
                // 登録処理
                realm1.writeBlocking {
                    copyToRealm(task)
                }
            } else {
                // 更新
                realm1.write {
                    findLatest(task)?.apply {
                        // 画面項目の値で更新
                        this.title = title
                        this.contents = content
                        this.date = date
                        this.categoryId = category.id
                    }
                }
            }
        }


        /**
         * 日付と時刻のボタンの表示を設定する
         */
        private fun setDateTimeButtonText() {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPANESE)
            binding.content.dateButton.text = dateFormat.format(calendar.time)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPANESE)
            binding.content.timeButton.text = timeFormat.format(calendar.time)

        }

}












