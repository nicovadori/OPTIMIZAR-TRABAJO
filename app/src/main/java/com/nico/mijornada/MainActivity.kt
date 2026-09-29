package com.nico.mijornada

import android.app.*
import android.os.*
import android.content.*
import android.graphics.Color
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class Task(var name:String,var priority:String,var estimated:Int,var worked:Int=0,var done:Boolean=false,var date:String="")

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var timer: TextView
    private lateinit var status: TextView
    private lateinit var prefs: android.content.SharedPreferences
    private val tasks=mutableListOf<Task>()
    private val history=mutableListOf<Task>()
    private var current:Task?=null
    private var seconds=1500
    private var running=false
    private val handler=Handler(Looper.getMainLooper())
    private val tick=object:Runnable{
        override fun run(){
            if(running){
                seconds=max(0,seconds-1); updateTimer()
                if(seconds==0){ running=false; current?.let{it.worked=it.estimated}; status.text="Bloque terminado. Es momento de descansar."; save(); render() }
                else handler.postDelayed(this,1000)
            }
        }
    }

    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        prefs=getSharedPreferences("jornada",MODE_PRIVATE)
        load()
        showHome()
    }

    private fun showHome(){
        val root=LinearLayout(this); root.orientation=LinearLayout.VERTICAL; root.setPadding(24,30,24,20)
        val title=TextView(this); title.text="Mi Jornada"; title.textSize=30f; title.setTextColor(Color.rgb(30,30,35))
        root.addView(title,LinearLayout.LayoutParams(-1,70))
        val tabs=LinearLayout(this); tabs.orientation=LinearLayout.HORIZONTAL
        list=LinearLayout(this); list.orientation=LinearLayout.VERTICAL
        val scroll=ScrollView(this); scroll.addView(list)
        val add=Button(this); add.text="+ Nueva tarea"; add.setOnClickListener{addTaskDialog()}
        val hist=Button(this); hist.text="Historial"; hist.setOnClickListener{showHistory()}
        tabs.addView(add,LinearLayout.LayoutParams(0,60,1f)); tabs.addView(hist,LinearLayout.LayoutParams(0,60,1f))
        root.addView(tabs); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root); render()
    }

    private fun render(){
        list.removeAllViews()
        val pending=tasks.filter{!it.done}
        val completed=tasks.filter{it.done}
        val info=TextView(this); info.text="Hoy · ${completed.size}/${tasks.size} completadas"; info.textSize=18f; info.setPadding(0,18,0,18); list.addView(info)
        pending.forEach{t-> taskView(t) }
        if(completed.isNotEmpty()){
            val clear=Button(this); clear.text="Limpiar completadas"; clear.setOnClickListener{
                completed.forEach{t-> history.add(t.copy(date=t.date.ifBlank{today()}))}
                tasks.removeAll{it.done}; save(); render()
            }; list.addView(clear)
        }
        if(pending.isEmpty() && completed.isEmpty()){
            val empty=TextView(this); empty.text="No hay tareas. Agregá la primera."; empty.textSize=17f; empty.setPadding(0,50,0,0); list.addView(empty)
        }
    }

    private fun taskView(t:Task){
        val box=LinearLayout(this); box.orientation=LinearLayout.VERTICAL; box.setPadding(0,12,0,12)
        val name=TextView(this); name.text="${t.name}\n${t.priority} · ${t.estimated} min"; name.textSize=18f
        val work=Button(this); work.text="Trabajar"; work.setOnClickListener{startTask(t)}
        val done=Button(this); done.text="✓ Completar"; done.setOnClickListener{t.done=true;t.worked=max(t.worked,0);save();render()}
        box.addView(name); box.addView(work); box.addView(done); list.addView(box)
    }

    private fun startTask(t:Task){
        current=t; seconds=t.estimated*60; running=false
        val root=LinearLayout(this); root.orientation=LinearLayout.VERTICAL; root.setPadding(24,35,24,20)
        val back=Button(this); back.text="← Volver"; back.setOnClickListener{showHome()}
        root.addView(back)
        val name=TextView(this); name.text=t.name; name.textSize=24f; root.addView(name)
        timer=TextView(this); timer.textSize=64f; timer.gravity=Gravity.CENTER; root.addView(timer,LinearLayout.LayoutParams(-1,180))
        status=TextView(this); status.text="Listo para empezar"; status.textSize=17f; status.gravity=Gravity.CENTER; root.addView(status)
        val start=Button(this); start.text="▶ Comenzar"; start.setOnClickListener{if(!running){running=true;status.text="Trabajando…";handler.post(tick)}}
        val pause=Button(this); pause.text="Ⅱ Pausar"; pause.setOnClickListener{running=false;status.text="Pausado"}
        val finish=Button(this); finish.text="✓ Terminar tarea"; finish.setOnClickListener{
            running=false; t.worked=max(1,((t.estimated*60-seconds)/60));t.done=true;save();showHome()
        }
        root.addView(start);root.addView(pause);root.addView(finish)
        setContentView(root);updateTimer()
    }

    private fun updateTimer(){ if(::timer.isInitialized) timer.text=String.format(Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60) }

    private fun addTaskDialog(){
        val lay=LinearLayout(this);lay.orientation=LinearLayout.VERTICAL;lay.setPadding(30,10,30,0)
        val name=EditText(this);name.hint="Tarea";lay.addView(name)
        val mins=EditText(this);mins.hint="Minutos estimados";mins.inputType=2;mins.setText("25");lay.addView(mins)
        val p=Spinner(this);p.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Alta","Media","Baja"));lay.addView(p)
        AlertDialog.Builder(this).setTitle("Nueva tarea").setView(lay).setPositiveButton("Agregar"){_,_-> 
            val n=name.text.toString().trim(); if(n.isNotEmpty()){tasks.add(Task(n,p.selectedItem.toString(),max(1,mins.text.toString().toIntOrNull()?:25),date=today()));save();render()}
        }.setNegativeButton("Cancelar",null).show()
    }

    private fun showHistory(){
        val root=LinearLayout(this);root.orientation=LinearLayout.VERTICAL;root.setPadding(24,30,24,20)
        val title=TextView(this);title.text="Historial";title.textSize=28f;root.addView(title)
        val scroll=ScrollView(this);val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL
        if(history.isEmpty()){val e=TextView(this);e.text="Todavía no hay tareas en el historial.";e.textSize=17f;box.addView(e)}
        history.asReversed().forEach{t->val v=TextView(this);v.text="✓ ${t.name}\n${t.date} · estimado ${t.estimated} min · trabajado ${t.worked} min";v.textSize=17f;v.setPadding(0,20,0,20);box.addView(v)}
        scroll.addView(box);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val back=Button(this);back.text="← Volver a Hoy";back.setOnClickListener{showHome()};root.addView(back)
        setContentView(root)
    }

    private fun today()=SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(Date())

    private fun save(){
        val s=tasks.joinToString("|"){listOf(it.name,it.priority,it.estimated,it.worked,it.done,it.date).joinToString("~")}
        val h=history.joinToString("|"){listOf(it.name,it.priority,it.estimated,it.worked,it.done,it.date).joinToString("~")}
        prefs.edit().putString("tasks",s).putString("history",h).apply()
    }
    private fun load(){
        fun parse(key:String,target:MutableList<Task>){prefs.getString(key,"")?.takeIf{it.isNotEmpty()}?.split("|")?.forEach{r->val a=r.split("~");if(a.size>=6)target.add(Task(a[0],a[1],a[2].toIntOrNull()?:25,a[3].toIntOrNull()?:0,a[4]=="true",a[5]))}}
        parse("tasks",tasks);parse("history",history)
    }
}