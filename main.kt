import java.util.Random
class Human{

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
        speed=_sp}

//    fun move(){
//        print("human is moved")
//
//    }

    fun moveTo(_toX: Int, _toY: Int){
        x+=_toX*speed
        y+=_toY*speed

        println("$name $surname move to: $x, $y with speed:$speed")
    }

}

fun main(){
    val random = Random()
    val humans = arrayOf(
        Human("Алексей", "Иванов", "Сергеевич", 1),
        Human("Дмитрий", "Петров", "Александрович", 1),
        Human("Михаил", "Федоров", "Дмитриевич", 2),
        Human("Иван", "Смирнов", "Игоревич", 3),
        Human("Артем", "Кузнецов", "Олегович", 1),
        Human("Сергей", "Попов", "Викторович", 5),
        Human("Андрей", "Васильев", "Николаевич", 6),
        Human("Анна", "Орлова", "Владимировна", 2),
        Human("Екатерина", "Виноградова", "Сергеевна", 3),
        Human("Ольга", "Соколова", "Андреевна", 4),
        Human("Наталья", "Лебедева", "Ивановна", 1),
        Human("Виктор", "Новиков", "Петрович", 5),
        Human("Юрий", "Морозов", "Анатольевич", 6),
        Human("Елена", "Зайцева", "Дмитриевна", 2),
        Human("Ирина", "Павлова", "Николаевна", 3)
    )
    print("Сколько тиков будут ходить люди? (цел число): ")
    val seconds = readln().toInt()
    println()

    for (i in 1..seconds) {
        for (person in humans) {
            val rand_x = listOf(-1, 1).random()
            val rand_y = listOf(-1, 1).random()
            person.moveTo(rand_x, rand_y)
            Thread.sleep(500)
        }
        println()
    }
//    val randomIntInRange = random.nextInt(10, 100)
////    val result = if (Random.nextBoolean()) 1 else -1
//
//    val petya: Human = Human("Petya", "Ivanov", "Petrovich", 443)
////    petya.move()
//    petya.moveTo(10,100)
//    println("${petya.x}")



}
