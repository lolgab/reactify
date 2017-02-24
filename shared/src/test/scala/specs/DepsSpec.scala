package specs

import reactify._
import org.scalatest.{Matchers, WordSpec}

class DepsSpec extends WordSpec with Matchers {
  "Deps" should {
    val width: Var[Double] = Var(0.0)

    val left: Var[Double] = Var(0.0)
    val center: Dep[Double, Double] = Dep(left, width / 2.0)
    val right: Dep[Double, Double] = Dep(left, width, submissive = true)

    "have the basic values" in {
      left() should be(0.0)
      width() should be(0.0)
      center() should be(0.0)
      right() should be(0.0)
    }
    "set left and reflect properly in right" in {
      left := 50.0
      left() should be(50.0)
      width() should be(0.0)
      center() should be(50.0)
      right() should be(50.0)
    }
    "set width and reflect properly in right" in {
      width := 25.0
      left() should be(50.0)
      width() should be(25.0)
      center() should be(62.5)
      right() should be(75.0)
    }
    "set right and reflect properly in left" in {
      right := 100.0
      left() should be(75.0)
      width() should be(25.0)
      center() should be(87.5)
      right() should be(100.0)
    }
    "set width again and not change left" in {
      width := 50.0
      left() should be(75.0)
      width() should be(50.0)
      center() should be(100.0)
      right() should be(125.0)
    }
    "set center and reflect properly in left and right" in {
      center := 200.0
      left() should be(175.0)
      width() should be(50.0)
      center() should be(200.0)
      right() should be(225.0)
    }
    "set width another time and retain center" in {
      width := 100.0
      left() should be(150.0)
      width() should be(100.0)
      center() should be(200.0)
      right() should be(250.0)
    }
    "set left and verify center and right adjust" in {
      left := 100.0
      left() should be(100.0)
      width() should be(100.0)
      center() should be(150.0)
      right() should be(200.0)
    }
    "set width and retain left's position" in {
      width := 500.0
      left() should be(100.0)
      width() should be(500.0)
      center() should be(350.0)
      right() should be(600.0)
    }
  }
  "Deps Specific Use-Cases" when {
    "combining Ints" should {
      "do simple addition" in {
        val a = Var(5)
        val b = Var(10)
        val combined = Dep[Int, Int](a, b)
        combined() should be(15)

        a := 10
        combined() should be(20)
      }
      "support customized connector" in {
        val a = Var(5)
        val b = Var(10)
        val distance = Dep[Int, Int](a, b)(new DepConnector[Int, Int] {
          override def combine(variable: => Int, adjustment: => Int): Int = math.abs(variable - adjustment)

          override def extract(value: => Int, adjustment: => Int): Int = adjustment - value
        })

        distance() should be(5)

        a := 20
        distance() should be(10)

        distance := 8
        distance() should be(8)
        a() should be(2)
        b() should be(10)
      }
    }
    "validating derived observables" should {
      val left = Var(0.0)
      val width = Var(0.0)
      val center = Dep[Double, Double](left, width / 2.0)
      val right = Dep[Double, Double](left, width)

      var leftValue = left()
      left.attach(d => leftValue = d)
      left.observing should be(Set.empty)

      "update center" in {
        center := 500.0
        left() should be(500.0)
        right() should be(500.0)
        left.observing should be(List(width))
        leftValue should be(500.0)
      }
      "update width" in {
        width := 100.0
        center() should be(500.0)
        left() should be(450.0)
        right() should be(550.0)
        leftValue should be(450.0)
      }
    }
  }
}