import kotlin.concurrent.thread
import kotlin.random.Random

open class Human{
    var name: String=""
    var surname: String=""
    var second_name: String=""
    var speed: Int=0

    //var group_number: Int=-1
    var x: Int=0
    var y: Int=0
    constructor(_name: String, _surname: String, _second: String, _sp: Int){
        name=_name
        surname=_surname
        second_name=_second
        speed=_sp
    }

    open fun moveTo(_toX: Int, _toY: Int){
        x+=_toX*speed
        y+=_toY*speed
        println("$name $surname move to: $x, $y with speed:$speed")
    }

}

class driver(_name: String, _surname: String, _second: String, _sp: Int) : Human(_name, _surname, _second, _sp){
    override fun moveTo(_toX: Int, _toY: Int){
        if (Random.nextBoolean()){
            x += -_toX*speed
        } else {
            y += _toY*speed
        }
        println("$name $surname move to: $x, $y with speed:$speed")
    }
}



fun main(){

    val humans = arrayOf(
        Human("Алексей", "Иванов", "Сергеевич", 1),
        Human("Дмитрий", "Петров", "Александрович", 1),
        Human("Михаил", "Федоров", "Дмитриевич", 2)
    )

    val Driv = driver("Иван", "Смирнов", "Игоревич", 25)
    val allof = humans+Driv

    print("Сколько тиков будут ходить люди? (цел число): ")
    val seconds = readln().toInt()
    println()


    for (i in 1..seconds) {
        val threads = mutableListOf<Thread>()
        for (person in allof) {
            val th = thread {
                val rand_x = listOf(-1, 1).random()
                val rand_y = listOf(-1, 1).random()
                person.moveTo(rand_x, rand_y)}
            threads.add(th)
        }
        threads.forEach { it.join() }
        println()
        Thread.sleep(500)
    }

}
