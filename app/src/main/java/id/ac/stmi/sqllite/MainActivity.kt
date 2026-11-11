package id.ac.stmi.sqllite

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

data class Service(val id: Long, val vehicle: String, val plate: String, val work: String, val date: String, val cost: Long)

class ServiceDb(context: MainActivity) : SQLiteOpenHelper(context, "garasi.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) = db.execSQL("CREATE TABLE service (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle TEXT NOT NULL, plate TEXT NOT NULL, work TEXT NOT NULL, date TEXT NOT NULL, cost INTEGER NOT NULL)")
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun all(): List<Service> = readableDatabase.query("service", null, null, null, null, null, "id DESC").use { c ->
        val rows = mutableListOf<Service>()
        while (c.moveToNext()) rows.add(Service(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getLong(5)))
        rows
    }
    fun save(s: Service) {
        val v = ContentValues().apply { put("vehicle", s.vehicle); put("plate", s.plate); put("work", s.work); put("date", s.date); put("cost", s.cost) }
        if (s.id == 0L) writableDatabase.insertOrThrow("service", null, v)
        else writableDatabase.update("service", v, "id=?", arrayOf(s.id.toString()))
    }
    fun delete(id: Long) { writableDatabase.delete("service", "id=?", arrayOf(id.toString())) }
}

class MainActivity : AppCompatActivity() {
    private lateinit var db: ServiceDb
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private lateinit var count: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = ServiceDb(this)
        list = findViewById(R.id.recordList)
        empty = findViewById(R.id.emptyState)
        count = findViewById(R.id.recordCount)
        findViewById<Button>(R.id.addButton).setOnClickListener { form(null) }
        refresh()
    }
    override fun onDestroy() { db.close(); super.onDestroy() }

    private fun refresh() {
        val rows = db.all()
        list.removeAllViews()
        count.text = getString(R.string.record_count, rows.size)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        rows.forEach { s ->
            val item = layoutInflater.inflate(R.layout.item_service, list, false)
            item.findViewById<TextView>(R.id.itemVehicle).text = s.vehicle
            item.findViewById<TextView>(R.id.itemDetail).text = "${s.plate} • ${s.date}\n${s.work} • Rp ${"%,d".format(s.cost)}"
            item.findViewById<Button>(R.id.editButton).setOnClickListener { form(s) }
            item.findViewById<Button>(R.id.deleteButton).setOnClickListener {
                AlertDialog.Builder(this).setTitle(R.string.delete_title).setMessage(getString(R.string.delete_message, s.vehicle))
                    .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete) { _, _ -> db.delete(s.id); refresh() }.show()
            }
            list.addView(item)
        }
    }

    private fun form(existing: Service?) {
        val view = layoutInflater.inflate(R.layout.dialog_service, null)
        val vehicle = view.findViewById<EditText>(R.id.inputVehicle)
        val plate = view.findViewById<EditText>(R.id.inputPlate)
        val work = view.findViewById<EditText>(R.id.inputWork)
        val date = view.findViewById<EditText>(R.id.inputDate)
        val cost = view.findViewById<EditText>(R.id.inputCost)
        existing?.let { vehicle.setText(it.vehicle); plate.setText(it.plate); work.setText(it.work); date.setText(it.date); cost.setText(it.cost.toString()) }
        val dialog = AlertDialog.Builder(this).setTitle(if (existing == null) R.string.add_record else R.string.edit_record)
            .setView(view).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val missing = listOf(vehicle, plate, work, date, cost).firstOrNull { it.text.toString().trim().isEmpty() }
                if (missing != null) { missing.error = getString(R.string.required); missing.requestFocus(); return@setOnClickListener }
                val amount = cost.text.toString().toLongOrNull()
                if (amount == null || amount < 0) { cost.error = getString(R.string.invalid_cost); return@setOnClickListener }
                db.save(Service(existing?.id ?: 0, vehicle.text.toString().trim(), plate.text.toString().trim().uppercase(), work.text.toString().trim(), date.text.toString().trim(), amount))
                dialog.dismiss(); refresh(); Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }
}
