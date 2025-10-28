package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tv: TextView

    private var n1 = ""
    private var n2 = ""
    private var op = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv = findViewById(R.id.tvResult)


        val btn0 = findViewById<Button>(R.id.btn0)
        val btn1 = findViewById<Button>(R.id.btn1)
        val btn2 = findViewById<Button>(R.id.btn2)
        val btn3 = findViewById<Button>(R.id.btn3)
        val btn4 = findViewById<Button>(R.id.btn4)
        val btn5 = findViewById<Button>(R.id.btn5)
        val btn6 = findViewById<Button>(R.id.btn6)
        val btn7 = findViewById<Button>(R.id.btn7)
        val btn8 = findViewById<Button>(R.id.btn8)
        val btn9 = findViewById<Button>(R.id.btn9)


        btn0.setOnClickListener { addNum("0") }
        btn1.setOnClickListener { addNum("1") }
        btn2.setOnClickListener { addNum("2") }
        btn3.setOnClickListener { addNum("3") }
        btn4.setOnClickListener { addNum("4") }
        btn5.setOnClickListener { addNum("5") }
        btn6.setOnClickListener { addNum("6") }
        btn7.setOnClickListener { addNum("7") }
        btn8.setOnClickListener { addNum("8") }
        btn9.setOnClickListener { addNum("9") }

        val add = findViewById<Button>(R.id.btnAdd)
        val sub = findViewById<Button>(R.id.btnSub)
        val mul = findViewById<Button>(R.id.btnMul)
        val div = findViewById<Button>(R.id.btnDiv)
        val eq = findViewById<Button>(R.id.btnEq)
        val clr = findViewById<Button>(R.id.btnC)

        add.setOnClickListener { setOp("+") }
        sub.setOnClickListener { setOp("-") }
        mul.setOnClickListener { setOp("*") }
        div.setOnClickListener { setOp("/") }

        eq.setOnClickListener { calc() }
        clr.setOnClickListener { clear() }
    }

    private fun addNum(n: String) {
        if (op.isEmpty()) {
            n1 += n
            tv.text = n1
        } else {
            n2 += n
            tv.text = "$n1$op$n2"
        }
    }

    private fun setOp(o: String) {
        if (n1.isNotEmpty()) {
            op = o
            tv.text = "$n1$op"
        }
    }

    private fun calc() {
        if (n1.isNotEmpty() && op.isNotEmpty() && n2.isNotEmpty()) {
            val a = n1.toDouble()
            val b = n2.toDouble()

            if (op == "/" && b == 0.0) {
                tv.text = "ай ай ай"
                n1 = ""
                n2 = ""
                op = ""
                return
            }

            val res = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> a / b
                else -> 0.0
            }

            val text = if (res % 1 == 0.0) res.toInt().toString() else res.toString()
            tv.text = text

            n1 = text
            n2 = ""
            op = ""
        }
    }

    private fun clear() {
        n1 = ""
        n2 = ""
        op = ""
        tv.text = "0"
    }
}
