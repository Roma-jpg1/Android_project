import kotlin.random.Random

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