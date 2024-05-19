package codewithcal.au.calendarappexample

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.math.floor

class DataBaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    companion object{
        private const val DATABASE_NAME = "Medicamentos.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLE_QUERY = "CREATE TABLE meds (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, frecuencia TEXT, dosis FLOAT, duracion TEXT, notas TEXT)"
        db?.execSQL(CREATE_TABLE_QUERY)
        val CREATE_TABLE_QUERY2 = "CREATE TABLE events (med_name TEXT, date TEXT, time INTEGER, done BOOLEAN)"
        db?.execSQL(CREATE_TABLE_QUERY2)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS meds")
        db?.execSQL("DROP TABLE IF EXISTS events")
        onCreate(db)
    }

    //add

    fun addMedyEvent(name: String, frecuencia: String, dosis: Float, duracion: String, notas: String, inicio: String){

        //if(!existe()){
        val content = ContentValues()
        content.put("name", name)
        content.put("frecuencia", frecuencia)
        content.put("dosis", dosis)
        content.put("duracion", duracion)
        content.put("notas", notas)
        addMed(content)
        //}

        /*
        * *Estructura de las variables de tiempo* *
        * frecuencia: x-A
        *   0 x: numero
        *   1 A: [d,s,m,a]
        *       d: al dia
        *       s: a la semana
        *       m: al mes
        *       a: al año
        * duracion: x-A
        * inicio: z-k-y-t
        *   0 z: num dia
        *   1 k: num mes
        *   2 y: num año
        *   3 t: hora; 1600 = 16:00 = 4pm
        * */

        //Spliteamos las variables
        val frecSplt = frecuencia.split("-")
        val durSplt = duracion.split("-")
        val iniSplt = inicio.split("-")

        //Iniciamos los contadores para los ciclos
        var diasTotales: Int = 0
        var frecuenciaTotal: Int = 0

        //Calculamos los contadores
        when(frecSplt[1]){
            'd'.toString() -> frecuenciaTotal=1 //x veces al dia; resulta en decimal
            's'.toString() -> frecuenciaTotal=7
            'm'.toString() -> frecuenciaTotal=30
            's'.toString() -> frecuenciaTotal=365
        }
        when(durSplt[1]){
            'd'.toString() -> diasTotales=1
            's'.toString() -> diasTotales=7
            'm'.toString() -> diasTotales=30
            's'.toString() -> diasTotales=365
        }
        diasTotales *= durSplt[0].toInt()

        //otras variables para los ciclos
        var meses = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (bisiesto(iniSplt[2].toInt())) {
            meses[1]++
        }

        //Durante la duracion; por cada frecuencia. Medido en dias
        for (i in 0..<diasTotales step frecuenciaTotal){
            //verdadera prograsion vi
            val vi: Float = i.toFloat()/frecSplt[0].toFloat()

            //el dia de inicio + la iteracion actual es el dia
            var dia = iniSplt[0].toInt()+ floor(vi) //NO ES EL DIA REAL AUN

            var mes = iniSplt[1].toInt() //mes en el que se esta agregando
            var anio = iniSplt[2].toInt() //año en el que se esta agregando

            //Encontramos el dia, mes y año apropiado del evento
            while (dia > meses[mes]){
                dia -= meses[mes]
                if (mes < 11){
                    mes++
                }else {
                    mes = 0
                    anio++
                }
            }
            val difTiempo = floor(((vi/floor(vi)-1)*2400)) //Diferencia de tiempo relativo a la hora inicial
            var time = iniSplt[3].toInt() + difTiempo //No es el tiempo real aun
            if (time>= 2400){
                time -= 2400
            }
            val date = dia.toString()+"-"+mes.toString()+"-"+anio.toString()
            var content2 = ContentValues()
            content2.put("med_name", name)
            content2.put("date", date)
            content2.put("time", time)
            content2.put("done", false)
            addEvent(content2)

        }
    }

    fun addMed(info: ContentValues){
        var db = this.writableDatabase
        db.insert("meds",null,info)
        db.close()
    }

    fun addEvent(info: ContentValues){
        var db = this.writableDatabase
        db.insert("events",null,info)
        db.close()
    }

    //get

    fun getEventsInDate(date: String): Cursor {
        val QUERY = "GET * FROM events WHERE date = $date"
        val db = writableDatabase
        return db.rawQuery(QUERY, null)
    }

    fun getFrecuencia(name: String): Cursor {
        val QUERY = "GET frecuencia FROM meds WHERE name = $name"
        val db = writableDatabase
        return db.rawQuery(QUERY, null)
    }

    fun getDuracion(name: String): Cursor {
        val QUERY = "GET duracion FROM meds WHERE name = $name"
        val db = writableDatabase
        return db.rawQuery(QUERY, null)
    }

    fun getDosis(name: String): Cursor {
        val QUERY = "GET dosis FROM meds WHERE name = $name"
        val db = writableDatabase
        return db.rawQuery(QUERY, null)
    }

    fun getNotas(name: String): Cursor {
        val QUERY = "GET notas FROM meds WHERE name = $name"
        val db = writableDatabase
        return db.rawQuery(QUERY, null)
    }

    //delete



    //Funciones para calculos fuera de la bd

    fun bisiesto(anio: Int):Boolean{
        return if(anio%4 == 0){
            if(anio%100 == 0){
                anio%400 == 0
            } else {
                true
            }
        }else{
            false
        }
    }
}
