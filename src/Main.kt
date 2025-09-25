import kotlin.concurrent.thread
import kotlin.random.Random


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