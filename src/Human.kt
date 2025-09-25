import Movable

open class Human : Movable{
    var name: String=""
    var surname: String=""
    var second_name: String=""
    override var speed: Int=0

    //var group_number: Int=-1
    override var x: Int=0
    override var y: Int=0
    constructor(_name: String, _surname: String, _second: String, _sp: Int){
        name=_name
        surname=_surname
        second_name=_second
        speed=_sp
    }

    override fun moveTo(_toX: Int, _toY: Int){
        x+=_toX*speed
        y+=_toY*speed
        println("$name $surname move to: $x, $y with speed:$speed")
    }

}